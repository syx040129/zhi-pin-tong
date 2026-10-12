package com.example.recruitment.admin.controller;

import com.example.recruitment.ai.config.AiProperties;
import com.example.recruitment.ai.llm.LlmClient;
import com.example.recruitment.common.Result;
import com.example.recruitment.log.annotation.OpLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 管理端：AI 配置查看与连通性测试
 */
@Tag(name = "管理端-AI 配置")
@RestController
@RequestMapping("/api/admin/ai")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminAiConfigController {

    private final AiProperties aiProperties;
    private final LlmClient llmClient;

    @Operation(summary = "查看 AI 配置（密钥脱敏）")
    @GetMapping("/config")
    public Result<Map<String, Object>> config() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("mode", aiProperties.displayMode());
        data.put("enabled", aiProperties.isEnabled());
        data.put("mock", aiProperties.isMock());
        data.put("baseUrl", aiProperties.getBaseUrl());
        data.put("model", aiProperties.getModel());
        data.put("embeddingModel", aiProperties.getEmbeddingModel());
        data.put("embeddingEnabled", aiProperties.isEmbeddingEnabled());
        data.put("hasApiKey", aiProperties.getApiKey() != null && !aiProperties.getApiKey().isBlank());
        data.put("recommendPoolSize", aiProperties.getRecommendPoolSize());
        data.put("recommendLimit", aiProperties.getRecommendLimit());
        return Result.ok(data);
    }

    @Operation(summary = "测试 AI 连通性")
    @PostMapping("/test")
    @OpLog(module = "AI 配置", action = "测试 AI 连通性")
    public Result<Map<String, Object>> test() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("mode", aiProperties.displayMode());
        if (aiProperties.realMode()) {
            Instant start = Instant.now();
            try {
                String reply = llmClient.chat("你是连通性测试助手，只回复 ok 两个字母。", "ping");
                data.put("ok", true);
                data.put("reply", reply.length() > 50 ? reply.substring(0, 50) + "..." : reply);
                data.put("latencyMs", Duration.between(start, Instant.now()).toMillis());
                data.put("message", "大模型连通正常");
            } catch (Exception e) {
                data.put("ok", false);
                data.put("message", e.getMessage());
            }
        } else {
            data.put("ok", true);
            data.put("message", "当前为 Mock 演示模式，无需调用外部模型即可运行全部 AI 功能；"
                    + "如需真实模型请在 application.yml 配置 ai.enabled=true、mock=false 与 api-key。");
        }
        return Result.ok(data);
    }
}
