package com.xiaohelab.guard.server.ai.service;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesis;
import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesisParam;
import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesisResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 阿里云百炼 DashScope 客户端封装。
 * <ul>
 *   <li>{@link #chat(String, String)} — 文本对话（带主备模型 fallback）</li>
 *   <li>{@link #generateImage(String)} — WanX 文生图，返回首张图片 URL</li>
 * </ul>
 * 无 api-key 时 {@link #isEnabled()} == false，调用方应降级到桩。
 */
@Component
@EnableConfigurationProperties(DashScopeProperties.class)
public class DashScopeClient {

    private static final Logger log = LoggerFactory.getLogger(DashScopeClient.class);

    private final DashScopeProperties props;

    public DashScopeClient(DashScopeProperties props) {
        this.props = props;
        if (!props.isEnabled()) {
            log.warn("[DashScope] ai.dashscope.api-key 未配置，AI 功能将走 stub 降级模式");
        } else {
            log.info("[DashScope] 已就绪 chat-model={} image-model={}",
                    props.getChat().getModel(), props.getImage().getModel());
        }
    }

    public boolean isEnabled() { return props.isEnabled(); }

    public DashScopeProperties getProps() { return props; }

    /**
     * 单轮对话；先用主模型，超时/失败时回落到 fallback-model。
     * @param systemPrompt 可空
     * @param userPrompt   不能为空
     * @return 模型回复文本
     */
    public Optional<String> chat(String systemPrompt, String userPrompt) {
        if (!isEnabled()) return Optional.empty();
        try {
            return Optional.ofNullable(callChat(props.getChat().getModel(), systemPrompt, userPrompt));
        } catch (Exception e) {
            log.warn("[DashScope] 主模型 {} 调用失败，尝试回落到 {}：{}",
                    props.getChat().getModel(), props.getChat().getFallbackModel(), e.getMessage());
            try {
                return Optional.ofNullable(callChat(props.getChat().getFallbackModel(), systemPrompt, userPrompt));
            } catch (Exception ex) {
                log.error("[DashScope] 备用模型也失败：{}", ex.getMessage());
                return Optional.empty();
            }
        }
    }

    private String callChat(String model, String systemPrompt, String userPrompt) throws Exception {
        Generation gen = new Generation();
        List<Message> messages = (systemPrompt != null && !systemPrompt.isBlank())
                ? List.of(
                    Message.builder().role(Role.SYSTEM.getValue()).content(systemPrompt).build(),
                    Message.builder().role(Role.USER.getValue()).content(userPrompt).build())
                : Collections.singletonList(
                    Message.builder().role(Role.USER.getValue()).content(userPrompt).build());

        GenerationParam param = GenerationParam.builder()
                .apiKey(props.getApiKey())
                .model(model)
                .messages(messages)
                .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                .maxTokens(props.getChat().getMaxTokens())
                .build();
        GenerationResult result = gen.call(param);
        if (result == null || result.getOutput() == null
                || result.getOutput().getChoices() == null
                || result.getOutput().getChoices().isEmpty()) {
            return null;
        }
        return result.getOutput().getChoices().get(0).getMessage().getContent();
    }

    /**
     * 文生图（WanX）。
     * @return 首张图片 URL；失败返回 empty
     */
    public Optional<String> generateImage(String prompt) {
        if (!isEnabled()) return Optional.empty();
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
