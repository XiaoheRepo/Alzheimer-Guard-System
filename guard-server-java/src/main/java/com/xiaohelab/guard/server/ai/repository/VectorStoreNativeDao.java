package com.xiaohelab.guard.server.ai.repository;

import com.xiaohelab.guard.server.ai.dto.RagHit;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * vector_store 原生 SQL DAO（pgvector 字段 JPA 不映射，全部走这里）。
 *
 * <p>基线：DBD §2.5.3、SADD §3.4 + ADR-003、LLD §7.1.3。</p>
 */
@Repository
public class VectorStoreNativeDao {

    private final JdbcTemplate jdbc;

    public VectorStoreNativeDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** 检测 pgvector 扩展是否启用（启动期健康检查使用）。 */
    public boolean isExtensionInstalled() {
        try {
            Integer cnt = jdbc.queryForObject(
                    "SELECT count(*) FROM pg_extension WHERE extname = 'vector'", Integer.class);
            return cnt != null && cnt > 0;
        } catch (Exception e) {
            return false;
        }
    }

    /** 将 float[] 序列化为 pgvector 字面量，例如 "[0.1,0.2,...]"。 */
    public static String toVectorLiteral(float[] vec) {
        StringBuilder sb = new StringBuilder(vec.length * 8 + 2);
        sb.append('[');
        for (int i = 0; i < vec.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(vec[i]);
        }
        sb.append(']');
        return sb.toString();
    }

    /**
     * 将已写入的行（JPA insert 时 embedding=null）补回 1024 维向量。
     * 单独 UPDATE 走 ::vector cast，避免 JPA 不识别 vector 类型。
     */
    public int upsertEmbedding(Long id, float[] embedding) {
        return jdbc.update(
                "UPDATE vector_store SET embedding = ?::vector, updated_at = now() WHERE id = ?",
                toVectorLiteral(embedding), id);
    }

    /**
     * 患者隔离的 Top-K 余弦相似度检索（FR-AI-004）。
     *
     * @param patientId       患者 ID（必传，硬隔离）
     * @param queryEmbedding  查询向量
     * @param topK            返回上限
     * @param threshold       相似度下限（cosine_similarity = 1 - cosine_distance）
     */
    public List<RagHit> searchTopK(Long patientId, float[] queryEmbedding, int topK, double threshold) {
        String literal = toVectorLiteral(queryEmbedding);
        String sql =
                "SELECT id, source_type, source_id, content, " +
                "       1 - (embedding <=> ?::vector) AS similarity " +
                "  FROM vector_store " +
                " WHERE patient_id = ? " +
                "   AND valid = TRUE " +
                "   AND embedding IS NOT NULL " +
                " ORDER BY embedding <=> ?::vector " +
                " LIMIT ?";
        return jdbc.query(sql,
                ps -> {
                    ps.setString(1, literal);
                    ps.setLong(2, patientId);
                    ps.setString(3, literal);
                    ps.setInt(4, Math.max(1, topK));
                },
                (rs, n) -> new RagHit(
                        rs.getLong("id"),
                        rs.getString("source_type"),
                        rs.getString("source_id"),
                        rs.getString("content"),
                        rs.getDouble("similarity")))
                .stream()
                .filter(h -> h.similarity() >= threshold)
                .toList();
    }

    /** FR-PRO-009：物理删除该患者全部向量。 */
    public int deleteByPatient(Long patientId) {
        return jdbc.update("DELETE FROM vector_store WHERE patient_id = ?", patientId);
    }

    /** 覆盖式重建：将旧版本同 source 记录 valid=FALSE。 */
    public int invalidateBySource(Long patientId, String sourceType, String sourceId) {
        return jdbc.update(
                "UPDATE vector_store SET valid = FALSE, updated_at = now() " +
                " WHERE patient_id = ? AND source_type = ? AND source_id = ? AND valid = TRUE",
                patientId, sourceType, sourceId);
    }
}
