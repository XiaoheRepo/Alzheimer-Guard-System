package com.xiaohelab.guard.server.ai.service;

import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesis;
import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesisParam;
import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesisResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/**
 * 阿里云百炼 DashScope 客户端封装。
 *
 * <p>Phase 1 迁移（API V2.0 §1.10 / backend_handbook §3.4.2）：
 * <ul>
 *   <li>{@link #chat(String, String)} —— 已切换到 Spring AI {@link ChatClient}（底层走 spring-ai-alibaba-starter-dashscope）。</li>
 *   <li>{@link #generateImage(String)} —— 暂保留 DashScope SDK 直接调用（Spring AI 1.0.0.x 图像 API 不稳定，过渡期方案）。</li>
 *   <li>主备 fallback 逻辑保留：主模型异常 → fallback 模型。</li>
 *   <li>无 api-key（{@link DashScopeProperties#isEnabled()} == false）或上下文中没有
 *       {@link ChatModel} Bean 时 {@link #isEnabled()} == false，调用方应降级到桩。</li>
 * </ul>
 *
 * <p>Phase 2 将引入 {@code @Tool} Function Calling + RAG，届时 chat() 会扩展 tool 注册路径。
 */
@Component
@EnableConfigurationProperties(DashScopeProperties.class)
public class DashScopeClient {

    private static final Logger log = LoggerFactory.getLogger(DashScopeClient.class);

    private final DashScopeProperties props;
    private final ObjectProvider<ChatModel> chatModelProvider;

    public DashScopeClient(DashScopeProperties props,
                           ObjectProvider<ChatModel> chatModelProvider) {
        this.props = props;
        this.chatModelProvider = chatModelProvider;
        if (!props.isEnabled()) {
            log.warn("[DashScope] ai.dashscope.api-key 未配置，AI 功能将走 stub 降级模式");
        } else if (chatModelProvider.getIfAvailable() == null) {
            log.warn("[DashScope] Spring AI ChatModel Bean 缺失（spring.ai.dashscope.api-key 未配置？），chat 将走 stub 降级模式");
        } else {
            log.info("[DashScope] Spring AI ChatClient 已就绪 chat-model={} image-model={}",
                    props.getChat().getModel(), props.getImage().getModel());
        }
    }

    /** chat 是否可用：需要 api-key 且容器中存在 Spring AI ChatModel Bean。 */
    public boolean isEnabled() {
        return props.isEnabled() && chatModelProvider.getIfAvailable() != null;
    }

    public DashScopeProperties getProps() { return props; }

    /**
     * 单轮对话；先用主模型，失败时回落到 fallback 模型。
     * 新实现走 Spring AI {@link ChatClient}，模型名通过 {@link ChatOptions#builder()} 动态指定，
     * 对外签名（参数 / 返回类型）保持不变，{@code AiSessionService} 调用方零修改。
     *
     * @param systemPrompt 可空
     * @param userPrompt   不能为空
     * @return 模型回复文本
     */
    public Optional<String> chat(String systemPrompt, String userPrompt) {
        if (!isEnabled()) return Optional.empty();
        ChatModel chatModel = chatModelProvider.getIfAvailable();
        if (chatModel == null) return Optional.empty();
        try {
            return Optional.ofNullable(callChat(chatModel, props.getChat().getModel(), systemPrompt, userPrompt));
        } catch (Exception e) {
            log.warn("[DashScope] 主模型 {} 调用失败，尝试回落到 {}：{}",
                    props.getChat().getModel(), props.getChat().getFallbackModel(), e.getMessage());
            try {
                return Optional.ofNullable(callChat(chatModel, props.getChat().getFallbackModel(), systemPrompt, userPrompt));
            } catch (Exception ex) {
                log.error("[DashScope] 备用模型也失败：{}", ex.getMessage());
                return Optional.empty();
            }
        }
    }

    private String callChat(ChatModel chatModel, String model, String systemPrompt, String userPrompt) {
        ChatOptions options = ChatOptions.builder()
                .model(model)
                .maxTokens(props.getChat().getMaxTokens())
                .build();

        ChatClient client = ChatClient.builder(chatModel).build();
        ChatClient.ChatClientRequestSpec spec = client.prompt().options(options);
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            spec = spec.system(systemPrompt);
        }
        String content = spec.user(userPrompt).call().content();
        return (content == null || content.isBlank()) ? null : content;
    }

    /**
     * 文生图（WanX）。
     *
     * <p>Phase 1 仍走 DashScope SDK 原始 API（Spring AI 1.0.0.x 的 ImageModel 抽象在阿里云端
     * 还不稳定）。Phase 2/3 计划切到 {@code spring-ai} 统一抽象。
     *
     * @return 首张图片 URL；失败返回 empty
     */
    public Optional<String> generateImage(String prompt) {
        if (!props.isEnabled()) return Optional.empty();
        try {
            ImageSynthesis is = new ImageSynthesis();
            ImageSynthesisParam param = ImageSynthesisParam.builder()
                    .apiKey(props.getApiKey())
                    .model(props.getImage().getModel())
                    .prompt(prompt)
                    .n(1)
                    .size(props.getImage().getSize())
                    .build();
            ImageSynthesisResult result = is.call(param);
            if (result == null || result.getOutput() == null
                    || result.getOutput().getResults() == null
                    || result.getOutput().getResults().isEmpty()) {
                return Optional.empty();
            }
            Map<String, String> first = result.getOutput().getResults().get(0);
            String url = first.get("url");
            return (url == null || url.isBlank()) ? Optional.empty() : Optional.of(url);
        } catch (Exception e) {
            log.error("[DashScope] 文生图调用失败：{}", e.getMessage());
            return Optional.empty();
        }
    }
}
