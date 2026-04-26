package com.xiaohelab.guard.server.ai.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * RAG 配置（HC-05 占位）。
 *
 * <p>当前从 {@code application.yml} 的 {@code ai.rag.*} 读取；
 * 后续应迁移到 {@code sys_config} 动态配置（TODO Phase 4）。</p>
 */
@ConfigurationProperties(prefix = "ai.rag")
public class RagProperties {

    /** 检索 Top-K，默认 5 */
    private int topK = 5;

    /** 余弦相似度阈值（>= 才纳入），默认 0.7 */
    private double similarityThreshold = 0.7;

    /** 切片长度（字符），默认 512 */
    private int chunkSize = 512;

    /** 切片重叠（字符），默认 100 */
    private int chunkOverlap = 100;

    public int getTopK() { return topK; }
    public void setTopK(int topK) { this.topK = topK; }
    public double getSimilarityThreshold() { return similarityThreshold; }
    public void setSimilarityThreshold(double similarityThreshold) { this.similarityThreshold = similarityThreshold; }
    public int getChunkSize() { return chunkSize; }
    public void setChunkSize(int chunkSize) { this.chunkSize = chunkSize; }
    public int getChunkOverlap() { return chunkOverlap; }
    public void setChunkOverlap(int chunkOverlap) { this.chunkOverlap = chunkOverlap; }
}
