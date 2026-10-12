package com.example.recruitment.ai.controller;

import com.example.recruitment.ai.dto.InterviewAnswerRequest;
import com.example.recruitment.ai.dto.InterviewQuestionVO;
import com.example.recruitment.ai.dto.InterviewReportVO;
import com.example.recruitment.ai.dto.InterviewStartRequest;
import com.example.recruitment.ai.dto.InterviewVO;
import com.example.recruitment.ai.service.AiInterviewService;
import com.example.recruitment.common.Result;
import com.example.recruitment.common.utils.SecurityUtils;
import com.example.recruitment.log.annotation.OpLog;
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

import java.util.List;

/**
 * AI 模拟面试接口
 */
@Tag(name = "AI 模拟面试")
@RestController
@RequestMapping("/api/ai/interview")
@RequiredArgsConstructor
public class AiInterviewController {

    private final AiInterviewService aiInterviewService;

    @Operation(summary = "开始一场 AI 模拟面试")
    @PostMapping("/start")
    @PreAuthorize("hasRole('USER')")
    @OpLog(module = "AI 模拟面试", action = "开始面试")
    public Result<InterviewVO> start(@Valid @RequestBody InterviewStartRequest request) {
        return Result.ok(aiInterviewService.start(request, SecurityUtils.currentUserId()));
    }

    @Operation(summary = "我的面试历史")
    @GetMapping("/list")
    @PreAuthorize("hasRole('USER')")
    public Result<List<InterviewVO>> history() {
        return Result.ok(aiInterviewService.history(SecurityUtils.currentUserId()));
    }

    @Operation(summary = "面试详情（含题目与作答状态）")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public Result<InterviewVO> detail(@PathVariable Long id) {
        return Result.ok(aiInterviewService.detail(id, SecurityUtils.currentUserId()));
    }

    @Operation(summary = "作答并由 AI 评分")
    @PostMapping("/{id}/answer")
    @PreAuthorize("hasRole('USER')")
    public Result<InterviewQuestionVO> answer(@PathVariable Long id,
                                              @Valid @RequestBody InterviewAnswerRequest request) {
        return Result.ok(aiInterviewService.answer(id, request.getQuestionId(),
                request.getAnswer(), SecurityUtils.currentUserId()));
    }

    @Operation(summary = "结束面试并生成报告")
    @PostMapping("/{id}/finish")
    @PreAuthorize("hasRole('USER')")
    @OpLog(module = "AI 模拟面试", action = "完成面试")
    public Result<InterviewReportVO> finish(@PathVariable Long id) {
        return Result.ok(aiInterviewService.finish(id, SecurityUtils.currentUserId()));
    }

    @Operation(summary = "AI 面试报告")
    @GetMapping("/{id}/report")
    @PreAuthorize("hasRole('USER')")
    public Result<InterviewReportVO> report(@PathVariable Long id) {
        return Result.ok(aiInterviewService.report(id, SecurityUtils.currentUserId()));
    }
}
