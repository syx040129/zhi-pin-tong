package com.example.recruitment.company.controller;

import com.example.recruitment.common.Result;
import com.example.recruitment.company.service.CompanyStatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 企业端数据看板接口
 */
@Tag(name = "企业数据看板")
@RestController
@RequestMapping("/api/company/stats")
@RequiredArgsConstructor
public class CompanyStatsController {

    private final CompanyStatsService companyStatsService;

    @Operation(summary = "招聘数据统计")
    @GetMapping
    @PreAuthorize("hasRole('COMPANY')")
    public Result<Map<String, Object>> stats() {
        return Result.ok(companyStatsService.stats());
    }
}
