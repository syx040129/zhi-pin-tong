package com.example.recruitment.common.controller;

import com.example.recruitment.ai.config.AiProperties;
import com.example.recruitment.common.Result;
import com.example.recruitment.common.config.AppCacheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 系统健康检查
 */
@Tag(name = "系统健康")
@RestController
@RequestMapping("/api/common")
@RequiredArgsConstructor
public class HealthController {

    private final DataSource dataSource;
    private final AppCacheService cacheService;
    private final AiProperties aiProperties;

    @Operation(summary = "健康检查（MySQL / Redis / AI 模式）")
    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        boolean mysql = checkMysql();
        boolean redis = cacheService.redisAvailable();
        Map<String, Object> ai = new LinkedHashMap<>();
        ai.put("mode", aiProperties.displayMode());
        ai.put("enabled", aiProperties.isEnabled());
        ai.put("mock", aiProperties.isMock());
        ai.put("hasApiKey", aiProperties.getApiKey() != null && !aiProperties.getApiKey().isBlank());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", mysql ? "UP" : "DEGRADED");
        data.put("mysql", mysql);
        data.put("redis", redis);
        data.put("ai", ai);
        return Result.ok(data);
    }

    private boolean checkMysql() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(2);
        } catch (Exception e) {
            return false;
        }
    }
}
