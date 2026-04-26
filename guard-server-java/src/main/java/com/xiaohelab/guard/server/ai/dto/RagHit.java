package com.xiaohelab.guard.server.ai.dto;

/**
 * RAG 检索命中结果（FR-AI-004 / FR-AI-006 可解释性）。
 *
 * @param id          vector_store.id（用于日志/调试）
 * @param sourceType  PROFILE / MEMORY / RESCUE_CASE
 * @param sourceId    对应业务记录 ID（patientId / noteId / caseId）
 * @param content     原始文本切片
 * @param similarity  余弦相似度（范围 [-1, 1]，值越大越相关）
 */
public record RagHit(Long id, String sourceType, String sourceId, String content, double similarity) {
}
