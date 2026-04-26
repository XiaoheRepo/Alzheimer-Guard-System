package com.xiaohelab.guard.server.ai.service;

import com.xiaohelab.guard.server.ai.dto.RagHit;
import com.xiaohelab.guard.server.ai.repository.VectorStoreNativeDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * RAG 检索服务（FR-AI-004：基于患者档案的向量检索）。
 *
 * <p>降级策略：embedding 服务或 pgvector 不可用时返回空列表，AI 对话回退为无背景知识 prompt。</p>
 */
@Service
@EnableConfigurationProperties(RagProperties.class)
public class RagRetrievalService {

    private static final Logger log = LoggerFactory.getLogger(RagRetrievalService.class);

    private final EmbeddingClient embeddingClient;
    private final VectorStoreNativeDao nativeDao;
    private final RagProperties props;

    public RagRetrievalService(EmbeddingClient embeddingClient,
                               VectorStoreNativeDao nativeDao,
                               RagProperties props) {
        this.embeddingClient = embeddingClient;
        this.nativeDao = nativeDao;
        this.props = props;
    }

    /**
     * 患者隔离的 Top-K 检索。
     *
     * @param patientId 患者 ID（必传）
     * @param query     用户查询文本
     * @param topK      <=0 时使用配置默认值
     */
    public List<RagHit> retrieveContext(Long patientId, String query, int topK) {
        if (patientId == null || query == null || query.isBlank()) return Collections.emptyList();
        if (!embeddingClient.isEnabled()) return Collections.emptyList();
        Optional<float[]> qv = embeddingClient.embed(query);
        if (qv.isEmpty()) return Collections.emptyList();
        int k = topK > 0 ? topK : props.getTopK();
        try {
            List<RagHit> hits = nativeDao.searchTopK(patientId, qv.get(), k, props.getSimilarityThreshold());
            log.debug("[RAG] retrieve patientId={} k={} hits={}", patientId, k, hits.size());
            return hits;
        } catch (Exception e) {
            log.warn("[RAG] 检索失败降级 patientId={} err={}", patientId, e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<RagHit> retrieveContext(Long patientId, String query) {
        return retrieveContext(patientId, query, 0);
    }
}
