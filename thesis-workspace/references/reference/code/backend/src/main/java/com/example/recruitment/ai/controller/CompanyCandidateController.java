package com.example.recruitment.ai.controller;

import com.example.recruitment.ai.dto.CompanyCandidateMatchVO;
import com.example.recruitment.ai.dto.MatchResultVO;
import com.example.recruitment.ai.service.JobMatchService;
import com.example.recruitment.application.entity.JobApplication;
import com.example.recruitment.application.mapper.JobApplicationMapper;
import com.example.recruitment.common.Result;
import com.example.recruitment.common.exception.BusinessException;
import com.example.recruitment.company.entity.Company;
import com.example.recruitment.company.service.CompanyService;
import com.example.recruitment.job.entity.Job;
import com.example.recruitment.job.mapper.JobMapper;
import com.example.recruitment.resume.entity.Resume;
import com.example.recruitment.resume.mapper.ResumeMapper;
import com.example.recruitment.user.entity.SysUser;
import com.example.recruitment.user.mapper.SysUserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 企业端：候选人 AI 匹配分析
 */
@Tag(name = "企业候选人 AI 分析")
@RestController
@RequestMapping("/api/company/candidates")
@RequiredArgsConstructor
public class CompanyCandidateController {

    private final JobApplicationMapper applicationMapper;
    private final JobMapper jobMapper;
    private final ResumeMapper resumeMapper;
    private final SysUserMapper userMapper;
    private final CompanyService companyService;
    private final JobMatchService jobMatchService;

    @Operation(summary = "候选人 AI 匹配分析（优势/风险/建议）")
    @GetMapping("/{applicationId}/analysis")
    @PreAuthorize("hasRole('COMPANY')")
    public Result<CompanyCandidateMatchVO> analysis(@PathVariable Long applicationId) {
        Company company = companyService.currentCompany();
        JobApplication application = applicationMapper.selectById(applicationId);
        if (application == null) {
            throw BusinessException.notFound("投递记录不存在");
        }
        Job job = jobMapper.selectById(application.getJobId());
        if (job == null || !Objects.equals(job.getCompanyId(), company.getId())) {
            throw BusinessException.forbidden("无权查看该候选人");
        }
        Resume resume = application.getResumeId() == null ? null
                : resumeMapper.selectById(application.getResumeId());
        SysUser user = userMapper.selectById(application.getUserId());
        CompanyCandidateMatchVO vo = new CompanyCandidateMatchVO();
        vo.setApplicationId(applicationId);
        vo.setCandidateUserId(application.getUserId());
        vo.setCandidateName(user == null ? "" : user.getNickname());
        vo.setJobId(job.getId());
        vo.setJobName(job.getJobName());
        if (resume == null) {
            vo.setAdvice("候选人未提供简历，建议通过系统消息索取简历后再评估");
            return Result.ok(vo);
        }
        MatchResultVO match = jobMatchService.compute(resume, job, true);
        vo.setMatch(match);
        List<String> strengths = new ArrayList<>();
        for (int i = 0; i < Math.min(3, match.getMatchedSkills().size()); i++) {
            strengths.add("掌握岗位核心技能 " + match.getMatchedSkills().get(i));
        }
        if (match.getMatchScore() >= 80) {
            strengths.add("综合匹配度超过 80%，与岗位高度契合");
        }
        if (match.getExperienceScore() >= 85) {
            strengths.add("具备匹配的实践/项目经验");
        }
        if (strengths.isEmpty()) {
            strengths.add("候选人简历信息完整，可结合面试进一步考察");
        }
        List<String> risks = new ArrayList<>();
        for (int i = 0; i < Math.min(3, match.getMissingSkills().size()); i++) {
            risks.add("缺少 " + match.getMissingSkills().get(i) + " 相关技能，入职后可能需要短期学习");
        }
        if (match.getExperienceScore() < 70) {
            risks.add("项目/实践经验相对薄弱，建议面试时重点考察实际落地能力");
        }
        if (risks.isEmpty()) {
            risks.add("暂未发现明显短板，建议通过技术面试进一步验证真实水平");
        }
        List<String> suggestions = new ArrayList<>();
        suggestions.add("围绕 " + String.join("、", match.getMatchedSkills().subList(0,
                Math.min(3, match.getMatchedSkills().size())))
                + (match.getMatchedSkills().isEmpty() ? "其技能方向" : "") + " 设计 1-2 道深度追问");
        suggestions.add("核实项目经历中的角色贡献与量化成果");
        if (match.getMatchScore() >= 80) {
            vo.setAdvice("建议进入面试环节");
        } else if (match.getMatchScore() >= 60) {
            vo.setAdvice("可安排面试，重点考察学习能力与培养潜力");
        } else {
            vo.setAdvice("匹配度较低，建议结合简历真实性决定是否进入面试");
        }
        vo.setStrengths(strengths);
        vo.setRisks(risks);
        vo.setSuggestions(suggestions);
        return Result.ok(vo);
    }
}
