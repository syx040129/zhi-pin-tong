package com.example.recruitment.ai.controller;

import com.example.recruitment.ai.dto.AiAnalysisVO;
import com.example.recruitment.ai.dto.OptimizeRequest;
import com.example.recruitment.ai.dto.OptimizeResultVO;
import com.example.recruitment.ai.service.ResumeAnalysisService;
import com.example.recruitment.ai.service.ResumeOptimizationService;
import com.example.recruitment.common.Result;
import com.example.recruitment.common.utils.SecurityUtils;
import com.example.recruitment.log.annotation.OpLog;
import com.example.recruitment.resume.service.ResumeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 简历分析 / 优化接口
 */
@Tag(name = "AI 简历分析优化")
@RestController
@RequestMapping("/api/ai/resume")
@RequiredArgsConstructor
public class AiResumeController {

    private final ResumeAnalysisService resumeAnalysisService;
    private final ResumeOptimizationService resumeOptimizationService;
    private final ResumeService resumeService;

    @Operation(summary = "AI 分析简历")
    @PostMapping("/analyze/{resumeId}")
    @PreAuthorize("hasRole('USER')")
    @OpLog(module = "AI 简历分析", action = "分析简历")
    public Result<AiAnalysisVO> analyze(@PathVariable Long resumeId) {
        resumeService.requireOwn(resumeId);
        return Result.ok(resumeAnalysisService.analyze(resumeId));
    }

    @Operation(summary = "获取最近一次简历分析结果")
    @GetMapping("/analysis/{resumeId}")
    @PreAuthorize("hasRole('USER')")
    public Result<AiAnalysisVO> latest(@PathVariable Long resumeId) {
        resumeService.requireOwn(resumeId);
        return Result.ok(resumeAnalysisService.latest(resumeId));
    }

    @Operation(summary = "AI 优化简历（针对目标岗位）")
    @PostMapping("/optimize")
    @PreAuthorize("hasRole('USER')")
    @OpLog(module = "AI 简历优化", action = "优化简历")
    public Result<OptimizeResultVO> optimize(@Valid @RequestBody OptimizeRequest request) {
        resumeService.requireOwn(request.getResumeId());
        return Result.ok(resumeOptimizationService.optimize(
                request.getResumeId(), request.getJobId()));
    }
}
