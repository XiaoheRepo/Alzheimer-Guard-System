-- =====================================================================
-- Phase 2 RAG 基线迁移：启用 pgvector + 升级 vector_store 表
-- 基线依据：SRS FR-AI-004 / FR-PRO-002 / FR-PRO-009 ；
--           SADD §3.4 + ADR-003（pgvector + HNSW，同库事务一致性）；
--           DBD §2.5.3 vector_store（1024 维 vector / cosine / HNSW）。
-- 说明：
--   1) V1__init_schema.sql 已建 vector_store（embedding 列为 TEXT 占位）；
--      本脚本在原表上原地升级为 vector(1024)，并补齐 chunk_index / updated_at。
--   2) 由用户在数据库实例（PostgreSQL 14+）显式授权创建 vector 扩展。
--      容器镜像建议使用 pgvector/pgvector:pg16。
--   3) 与 Flyway 版本控制并存：本仓库 db/migration 下也存在 V7__pgvector_rag.sql
--      作为应用启动时的实际执行脚本；本文件为基线归档版本（按用户命名要求）。
-- =====================================================================

-- 0. 启用 pgvector 扩展（幂等）
CREATE EXTENSION IF NOT EXISTS vector;

-- 1. vector_store 升级（在 V1__init_schema.sql 已建表的基础上 ALTER）
ALTER TABLE vector_store
    ADD COLUMN IF NOT EXISTS chunk_index INT NOT NULL DEFAULT 0;

ALTER TABLE vector_store
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT now();

-- 旧 embedding 列为 TEXT 占位，本期切换为 vector(1024)。
-- 历史数据视为脏数据，统一置 NULL（首次部署时 V1 后通常为空表）。
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

-- 2. 索引：患者隔离 + HNSW 余弦距离 + 覆盖唯一键
CREATE INDEX IF NOT EXISTS idx_vector_store_patient_valid
    ON vector_store (patient_id, valid);

CREATE INDEX IF NOT EXISTS idx_vector_store_embedding_hnsw
    ON vector_store USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);

CREATE UNIQUE INDEX IF NOT EXISTS uk_vector_store_source
    ON vector_store (patient_id, source_type, source_id, chunk_index)
    WHERE valid = TRUE;

COMMENT ON COLUMN vector_store.chunk_index IS '长文本切片序号（0..N），LLD §7.1.3';
COMMENT ON COLUMN vector_store.updated_at  IS '最近一次维护时间（ALTER/UPSERT/失效）';
COMMENT ON COLUMN vector_store.embedding   IS '1024 维向量（DashScope text-embedding-v3，cosine）';
