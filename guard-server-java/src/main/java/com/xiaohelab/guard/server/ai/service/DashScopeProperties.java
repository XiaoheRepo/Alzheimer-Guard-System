package com.xiaohelab.guard.server.ai.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** 阿里云百炼 DashScope 配置（backend_handbook §19.4）。 */
@ConfigurationProperties(prefix = "ai.dashscope")
public class DashScopeProperties {

    private String apiKey;
    private Chat chat = new Chat();
    private Image image = new Image();
    private Embedding embedding = new Embedding();

    public boolean isEnabled() {
        return apiKey != null && !apiKey.isBlank();
    }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public Chat getChat() { return chat; }
    public void setChat(Chat chat) { this.chat = chat; }
    public Image getImage() { return image; }
    public void setImage(Image image) { this.image = image; }
    public Embedding getEmbedding() { return embedding; }
    public void setEmbedding(Embedding embedding) { this.embedding = embedding; }

    public static class Chat {
        private String model = "qwen-max";
        private String fallbackModel = "qwen-turbo";
        private int timeoutMs = 30000;
        private int maxTokens = 4096;

        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public String getFallbackModel() { return fallbackModel; }
        public void setFallbackModel(String fallbackModel) { this.fallbackModel = fallbackModel; }
        public int getTimeoutMs() { return timeoutMs; }
        public void setTimeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; }
        public int getMaxTokens() { return maxTokens; }
        public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }
    }

    public static class Image {
        private String model = "wanx-v1";
        private String size = "1024*1024";

        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public String getSize() { return size; }
        public void setSize(String size) { this.size = size; }
    }

    public static class Embedding {
        private String model = "text-embedding-v3";
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
    }
}
