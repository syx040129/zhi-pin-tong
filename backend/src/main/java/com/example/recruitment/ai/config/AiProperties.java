package com.example.recruitment.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * AI 配置属性（application.yml 中 ai.* 前缀）
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai")
public class AiProperties {

    /** 是否启用真实大模型 */
    private boolean enabled;
    /** Mock / 演示模式（无 API Key 也可运行全部 AI 功能） */
    private boolean mock = true;
    /** OpenAI 兼容接口地址 */
    private String baseUrl;
    private String apiKey;
    private String model;
    private String embeddingModel;
    /** 是否调用真实 Embedding 模型；关闭时使用本地向量 */
    private boolean embeddingEnabled = true;
    private double temperature = 0.4;
    private int maxTokens = 2000;
    private int timeoutSeconds = 30;
    private int recommendPoolSize = 20;
    private int recommendLimit = 10;

    /**
     * 是否处于真实大模型模式：开启且未 Mock 且配置了 API Key
     */
    public boolean realMode() {
        return enabled && !mock && StringUtils.hasText(apiKey);
    }

    public String displayMode() {
        if (realMode()) {
            return "REAL";
        }
        if (mock) {
            return "MOCK";
        }
        return "DISABLED";
    }
}
