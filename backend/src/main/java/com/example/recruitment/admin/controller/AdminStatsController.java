package com.example.recruitment.admin.controller;

import com.example.recruitment.admin.service.AdminStatsService;
import com.example.recruitment.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 管理端：数据统计
 */
@Tag(name = "管理端-统计")
@RestController
@RequestMapping("/api/admin/stats")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminStatsController {

    private final AdminStatsService adminStatsService;

    @Operation(summary = "核心指标与分布")
    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        return Result.ok(adminStatsService.overview());
    }

    @Operation(summary = "增长趋势")
    @GetMapping("/trends")
    public Result<Map<String, Object>> trends(@RequestParam(defaultValue = "30") int days) {
        return Result.ok(adminStatsService.trends(days));
    }
}
