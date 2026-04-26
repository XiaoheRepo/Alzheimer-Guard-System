package com.xiaohelab.guard.server.ai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 文本向量化客户端（Phase 2 RAG 铺路占位）。
 *
 * <p>基线依据：SADD §3.4（pgvector + HNSW 1024 维 cosine）、API V2.0 §1.10、backend_handbook §3.4.2。
 *
 * <p>设计要点：
 * <ul>
 *   <li>注入 {@link ObjectProvider} 而非直接注入 {@link EmbeddingModel}：<br>
 *       缺 {@code spring.ai.dashscope.api-key} 时容器中可能没有 EmbeddingModel Bean，
 *       此处需要降级而非启动失败（&red;line：环境降级）。</li>
 *   <li>Phase 1 仅装配 + 暴露 {@link #embed(String)} / {@link #isEnabled()} 方法，
 *       <b>不接入业务调用</b>（不写入 vector_store，不参与 prompt 拼装）。</li>
 *   <li>Phase 2 RAG 检索流水线启用后，{@code patient_memory_note} 与外部知识入库时
 *       通过本类完成 1024 维向量计算并写入 {@code vector_store}（DBD §2.5）。</li>
 * </ul>
 *
 * <p>模型名当前由 spring-ai-alibaba 自动装配从 {@code spring.ai.dashscope.embedding.options.model} 读取，
 * 默认 {@code text-embedding-v3}（与 {@link DashScopeProperties.Embedding} 保持一致）。
 */
@Component
public class EmbeddingClient {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingClient.class);

    private final ObjectProvider<EmbeddingModel> embeddingModelProvider;
    private final DashScopeProperties props;

    public EmbeddingClient(ObjectProvider<EmbeddingModel> embeddingModelProvider,
                           DashScopeProperties props) {
        this.embeddingModelProvider = embeddingModelProvider;
        this.props = props;
        if (!isEnabled()) {
            log.warn("[Embedding] EmbeddingModel Bean 不可用（api-key 缺失或自动装配未生效），向量化降级关闭");
        } else {
            log.info("[Embedding] 已就绪 model={}", props.getEmbedding().getModel());
        }
    }

    /** 嵌入服务是否可用：需要 api-key 且容器中存在 Spring AI EmbeddingModel Bean。 */
    public boolean isEnabled() {
        return props.isEnabled() && embeddingModelProvider.getIfAvailable() != null;
    }

    /**
     * 计算文本向量（1024 维，cosine 度量；SADD §3.4）。
     *
     * @param text 入参文本，空白则返回 empty
     * @return float[] 向量；未启用 / 调用失败时返回 {@link Optional#empty()}
     */
    public Optional<float[]> embed(String text) {
        if (text == null || text.isBlank()) return Optional.empty();
        if (!isEnabled()) return Optional.empty();
        EmbeddingModel model = embeddingModelProvider.getIfAvailable();
        if (model == null) return Optional.empty();
        try {
            float[] vec = model.embed(text);
            if (vec == null || vec.length == 0) return Optional.empty();
            return Optional.of(vec);
        } catch (Exception e) {
            log.warn("[Embedding] 向量化失败：{}", e.getMessage());
            return Optional.empty();
        }
    }
}
