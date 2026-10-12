package com.example.recruitment.ai.llm;

import com.example.recruitment.ai.config.AiProperties;
import com.example.recruitment.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * 大模型客户端：封装 Spring AI ChatClient。
 * 未配置真实模型时调用会抛出可读的业务异常（上层应保证 Mock 模式下不会调用）。
 */
@Slf4j
@Component
public class LlmClient {

    private final AiProperties aiProperties;
    private final ObjectProvider<ChatClient.Builder> chatClientBuilderProvider;

    public LlmClient(AiProperties aiProperties,
                     ObjectProvider<ChatClient.Builder> chatClientBuilderProvider) {
        this.aiProperties = aiProperties;
        this.chatClientBuilderProvider = chatClientBuilderProvider;
    }

    /**
     * 单轮对话，返回文本
     */
    public String chat(String system, String user) {
        ChatClient.Builder builder = chatClientBuilderProvider.getIfAvailable();
        if (!aiProperties.realMode() || builder == null) {
            throw BusinessException.aiError(
                    "未启用真实大模型，请检查 ai.enabled / ai.api-key 配置");
        }
        Instant start = Instant.now();
        try {
            ChatClient chatClient = builder.build();
            String content = chatClient.prompt()
                    .system(system)
                    .user(user)
                    .call()
                    .content();
            return content == null ? "" : content.trim();
        } catch (Exception e) {
            log.warn("大模型调用失败: {}", e.getMessage());
            throw BusinessException.aiError("AI 服务调用失败，请检查模型配置与网络");
        } finally {
            log.debug("LLM 调用耗时 {}ms", Duration.between(start, Instant.now()).toMillis());
        }
    }

    /**
     * 要求大模型返回 JSON：剥离 markdown 代码块并返回对象文本
     */
    public String chatJson(String system, String user) {
        String raw = chat(system, user);
        String cleaned = raw.replaceAll("```(?:json)?", "").replace("```", "").trim();
        if (!cleaned.startsWith("{")) {
            int start = cleaned.indexOf('{');
            int end = cleaned.lastIndexOf('}');
            if (start >= 0 && end > start) {
                cleaned = cleaned.substring(start, end + 1);
            }
        }
        return cleaned;
    }
}
