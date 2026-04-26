package com.xiaohelab.guard.server.ai.service;

import com.xiaohelab.guard.server.ai.entity.VectorStoreEntity;
import com.xiaohelab.guard.server.ai.repository.VectorStoreNativeDao;
import com.xiaohelab.guard.server.ai.repository.VectorStoreRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 向量化写入服务（FR-PRO-002 / FR-PRO-009 / LLD §7.1.3）。
 *
 * <ul>
 *   <li>切片→Embedding→写入 vector_store（覆盖式重建：旧版置 valid=FALSE）；</li>
 *   <li>所有 public 入口 {@link Async} 异步执行，保证业务接口不被阻塞；</li>
 *   <li>Embedding 不可用时降级为 no-op + warn（启动/运行期都不抛异常）。</li>
 * </ul>
 */
@Service
@EnableConfigurationProperties(RagProperties.class)
public class VectorIndexService {

    public static final String SOURCE_PROFILE = "PROFILE";
    public static final String SOURCE_MEMORY  = "MEMORY";

    private static final Logger log = LoggerFactory.getLogger(VectorIndexService.class);

    private final VectorStoreRepository repo;
    private final VectorStoreNativeDao nativeDao;
    private final EmbeddingClient embeddingClient;
    private final TextChunker chunker;
    private final RagProperties props;

    public VectorIndexService(VectorStoreRepository repo,
                              VectorStoreNativeDao nativeDao,
                              EmbeddingClient embeddingClient,
                              TextChunker chunker,
                              RagProperties props) {
        this.repo = repo;
        this.nativeDao = nativeDao;
        this.embeddingClient = embeddingClient;
        this.chunker = chunker;
        this.props = props;
    }

    @PostConstruct
    void healthCheck() {
        boolean ext = nativeDao.isExtensionInstalled();
        if (!ext) {
            log.warn("[RAG] pgvector 扩展未启用：请在数据库执行 CREATE EXTENSION vector; 否则向量检索将不可用");
        } else {
            log.info("[RAG] pgvector 扩展就绪；topK={} threshold={} chunkSize={} overlap={}",
                    props.getTopK(), props.getSimilarityThreshold(),
                    props.getChunkSize(), props.getChunkOverlap());
        }
    }

    /** 索引患者长文本档案（FR-PRO-002）。 */
    @Async("guardAsyncExecutor")
    @Transactional(rollbackFor = Exception.class)
    public void indexProfile(Long patientId, String sourceId, String longText) {
        indexInternal(patientId, SOURCE_PROFILE, sourceId, longText);
    }

    /** 索引患者记忆笔记（FR-PRO-002 / Phase 3 入口）。 */
    @Async("guardAsyncExecutor")
    @Transactional(rollbackFor = Exception.class)
    public void indexMemory(Long patientId, String noteId, String kind, String content) {
        String prefix = (kind == null || kind.isBlank()) ? "" : "[" + kind + "] ";
        indexInternal(patientId, SOURCE_MEMORY, noteId, prefix + (content == null ? "" : content));
    }

    /** FR-PRO-009：患者注销时物理删除全部向量。 */
    @Async("guardAsyncExecutor")
    @Transactional(rollbackFor = Exception.class)
    public void deletePatient(Long patientId) {
        if (patientId == null) return;
        int n = nativeDao.deleteByPatient(patientId);
        log.info("[RAG] FR-PRO-009 删除患者向量 patientId={} affected={}", patientId, n);
    }

    /**
     * 实际写入逻辑：
     * 1. 旧版同 source 记录 valid=FALSE（覆盖式重建，避免唯一索引冲突）；
     * 2. 切片 → 逐片 embed → JPA insert 元数据 → JdbcTemplate 回填 vector；
     * 3. 任意环节失败回滚。
     */
    /** 内部实现：事务由调用方（@Transactional 公共入口）提供。 */
    private void indexInternal(Long patientId, String sourceType, String sourceId, String text) {
        if (patientId == null || sourceId == null || text == null || text.isBlank()) return;
        if (!embeddingClient.isEnabled()) {
            log.warn("[RAG] EmbeddingClient 不可用，跳过 indexInternal patientId={} sourceType={} sourceId={}",
                    patientId, sourceType, sourceId);
            return;
        }
        // 1. 失效旧版本
        nativeDao.invalidateBySource(patientId, sourceType, sourceId);

        // 2. 切片
        List<String> chunks = chunker.chunk(text, props.getChunkSize(), props.getChunkOverlap());
        if (chunks.isEmpty()) return;

        int written = 0;
        OffsetDateTime now = OffsetDateTime.now();
        for (int i = 0; i < chunks.size(); i++) {
            String slice = chunks.get(i);
            Optional<float[]> vec = embeddingClient.embed(slice);
            if (vec.isEmpty()) {
                log.warn("[RAG] embed 失败，跳过 chunk patientId={} sourceId={} idx={}", patientId, sourceId, i);
                continue;
            }
            // JPA 写元数据（embedding 列 @Transient）
            VectorStoreEntity e = new VectorStoreEntity();
            e.setPatientId(patientId);
            e.setSourceType(sourceType);
            e.setSourceId(sourceId);
            e.setSourceVersion(1L);
            e.setChunkIndex(i);
            e.setContent(slice);
            e.setValid(true);
            e.setUpdatedAt(now);
            VectorStoreEntity saved = repo.save(e);
            // JdbcTemplate 回填 vector(1024)
            nativeDao.upsertEmbedding(saved.getId(), vec.get());
            written++;
        }
        log.info("[RAG] indexed patientId={} sourceType={} sourceId={} chunks={}/{}",
                patientId, sourceType, sourceId, written, chunks.size());
    }
}
