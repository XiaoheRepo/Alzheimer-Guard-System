-- =====================================================================
-- V7: Phase 2 RAG —— 启用 pgvector + 升级 vector_store
-- 基线：SRS FR-AI-004 / FR-PRO-002 / FR-PRO-009、SADD §3.4 + ADR-003、
--       DBD §2.5.3、LLD §7.1.3、backend_handbook §3.4.2。
--
-- ⚠ 数据库实例必须预装 pgvector（pgvector/pgvector:pg16 镜像或源码安装）。
--   用户已显式授权本迁移启用扩展和重建 embedding 列。
-- =====================================================================

-- 0. pgvector 扩展（幂等）
CREATE EXTENSION IF NOT EXISTS vector;

-- 1. 列升级 ----------------------------------------------------------
ALTER TABLE vector_store
    ADD COLUMN IF NOT EXISTS chunk_index INT NOT NULL DEFAULT 0;

ALTER TABLE vector_store
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT now();

-- 将占位 TEXT embedding 列替换为 vector(1024)；历史值清空（占位期未使用）
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'vector_store' AND column_name = 'embedding'
          AND udt_name <> 'vector'
    ) THEN
        EXECUTE 'ALTER TABLE vector_store ALTER COLUMN embedding DROP DEFAULT';
        EXECUTE 'ALTER TABLE vector_store ALTER COLUMN embedding TYPE vector(1024) USING NULL';
    END IF;
END $$;

-- 2. 索引 ------------------------------------------------------------
-- V1 中的 idx_vector_patient(patient_id, valid, created_at DESC) 保留；这里再补 HNSW 与唯一键。
CREATE INDEX IF NOT EXISTS idx_vector_store_patient_valid
    ON vector_store (patient_id, valid);

CREATE INDEX IF NOT EXISTS idx_vector_store_embedding_hnsw
    ON vector_store USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);

CREATE UNIQUE INDEX IF NOT EXISTS uk_vector_store_source
    ON vector_store (patient_id, source_type, source_id, chunk_index)
    WHERE valid = TRUE;

COMMENT ON COLUMN vector_store.chunk_index IS '长文本切片序号（0..N），LLD §7.1.3';
COMMENT ON COLUMN vector_store.updated_at  IS '最近一次维护时间（UPSERT/失效）';
COMMENT ON COLUMN vector_store.embedding   IS '1024 维向量（DashScope text-embedding-v3，cosine）';
