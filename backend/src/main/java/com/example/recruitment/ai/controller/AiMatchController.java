package com.example.recruitment.ai.controller;

import com.example.recruitment.ai.dto.MatchRequest;
import com.example.recruitment.ai.dto.MatchResultVO;
import com.example.recruitment.ai.service.JobMatchService;
import com.example.recruitment.common.Result;
import com.example.recruitment.common.exception.BusinessException;
import com.example.recruitment.common.utils.SecurityUtils;
import com.example.recruitment.job.entity.Job;
import com.example.recruitment.job.service.JobService;
import com.example.recruitment.log.annotation.OpLog;
import com.example.recruitment.resume.entity.Resume;
import com.example.recruitment.resume.service.ResumeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 岗位匹配接口
 */
@Tag(name = "AI 岗位匹配")
@RestController
@RequestMapping("/api/ai/match")
@RequiredArgsConstructor
public class AiMatchController {

    private final ResumeService resumeService;
    private final JobService jobService;
    private final JobMatchService jobMatchService;
    private final com.example.recruitment.company.mapper.CompanyMapper companyMapper;

    @Operation(summary = "简历与岗位智能匹配", description = "语义 50% + 技能 25% + 经验 15% + 学历 10%")
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    @OpLog(module = "AI 岗位匹配", action = "执行匹配分析")
    public Result<MatchResultVO> match(@Valid @RequestBody MatchRequest request) {
        Long userId = SecurityUtils.currentUserId();
        Resume resume = request.getResumeId() == null ? resumeService.latestResume(userId)
                : resumeService.requireOwn(request.getResumeId());
        if (resume == null) {
            throw BusinessException.badRequest("请先创建并完善简历，再进行 AI 匹配");
        }
        Job job = jobService.requireOnlineJob(request.getJobId());
        MatchResultVO result = jobMatchService.compute(resume, job, true);
        var company = companyMapper.selectById(job.getCompanyId());
        if (company != null) {
            result.setCompanyName(company.getCompanyName());
            result.setCompanyLogo(company.getLogo());
        }

        return Result.ok(result);
    }
}
