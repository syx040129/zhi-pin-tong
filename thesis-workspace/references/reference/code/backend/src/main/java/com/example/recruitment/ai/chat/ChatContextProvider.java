package com.example.recruitment.ai.chat;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.recruitment.ai.entity.AiRecommendation;
import com.example.recruitment.ai.mapper.AiRecommendationMapper;
import com.example.recruitment.application.entity.JobApplication;
import com.example.recruitment.application.mapper.JobApplicationMapper;
import com.example.recruitment.common.constant.CommonConstants;
import com.example.recruitment.common.constant.RoleConstants;
import com.example.recruitment.company.entity.Company;
import com.example.recruitment.company.mapper.CompanyMapper;
import com.example.recruitment.job.entity.Job;
import com.example.recruitment.job.entity.JobSkill;
import com.example.recruitment.job.mapper.JobMapper;
import com.example.recruitment.job.mapper.JobSkillMapper;
import com.example.recruitment.report.entity.Report;
import com.example.recruitment.report.mapper.ReportMapper;
import com.example.recruitment.resume.entity.Resume;
import com.example.recruitment.resume.entity.ResumeExperience;
import com.example.recruitment.resume.service.ResumeService;
import com.example.recruitment.user.entity.SysUser;
import com.example.recruitment.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * AI 智能助手上下文收集器：把当前用户相关的简历、推荐、投递（或企业岗位、平台统计）
 * 汇总为 {@link ChatContext}，使「智能问答」具备数据感知能力。
 * 任何子查询失败都只记 WARN，不影响对话主链路。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatContextProvider {

    private final ResumeService resumeService;
    private final AiRecommendationMapper recommendationMapper;
    private final JobApplicationMapper applicationMapper;
    private final JobMapper jobMapper;
    private final JobSkillMapper jobSkillMapper;
    private final CompanyMapper companyMapper;
    private final ReportMapper reportMapper;
    private final SysUserMapper userMapper;

    public ChatContext collect(Long userId, String role) {
        ChatContext context = new ChatContext();
        context.setRole(role);
        if (userId == null) {
            return context;
        }
        try {
            if (RoleConstants.USER.equals(role)) {
                fillUser(context, userId);
            } else if (RoleConstants.COMPANY.equals(role)) {
                fillCompany(context, userId);
            } else if (RoleConstants.ADMIN.equals(role)) {
                fillAdmin(context);
            }
        } catch (Exception e) {
            log.warn("收集 AI 助手上下文失败，降级为通用回答: {}", e.getMessage());
        }
        return context;
    }

    private void fillUser(ChatContext context, Long userId) {
        Resume resume = resumeService.latestResume(userId);
        if (resume != null) {
            context.setResumeId(resume.getId());
            context.setResumeName(resume.getName());
            context.setEducation(resume.getEducation());
            context.setSchool(resume.getSchool());
            context.setMajor(resume.getMajor());
            context.setSkills(ResumeService.splitSkills(resume.getSkills()));
            context.setCompletionScore(resume.getCompletionScore());
            context.setSummary(resume.getSummary());
            List<ResumeExperience> experiences = resumeService.loadExperiences(resume.getId());
            context.setExperienceCount(experiences == null ? 0 : experiences.size());
            List<String> titles = new ArrayList<>();
            if (experiences != null) {
                for (ResumeExperience experience : experiences) {
                    if (StringUtils.hasText(experience.getTitle())) {
                        titles.add(experience.getTitle());
                    }
                }
            }
            context.setExperienceTitles(titles);
        }
        context.setApplicationCount(count(applicationMapper.selectCount(
                new LambdaQueryWrapper<JobApplication>().eq(JobApplication::getUserId, userId))));
        context.setInterviewCount(count(applicationMapper.selectCount(
                new LambdaQueryWrapper<JobApplication>()
                        .eq(JobApplication::getUserId, userId)
                        .eq(JobApplication::getStatus, CommonConstants.APP_INTERVIEW))));

        List<AiRecommendation> recommendations = recommendationMapper.selectList(
                new LambdaQueryWrapper<AiRecommendation>()
                        .eq(AiRecommendation::getUserId, userId)
                        .orderByDesc(AiRecommendation::getMatchScore)
                        .last("limit 5"));
        Map<String, Integer> topJobs = new LinkedHashMap<>();
        for (AiRecommendation item : recommendations) {
            Job job = jobMapper.selectById(item.getJobId());
            String name = job == null ? "岗位#" + item.getJobId() : job.getJobName();
            topJobs.put(name, item.getMatchScore());
        }
        context.setTopJobs(topJobs);
    }

    private void fillCompany(ChatContext context, Long userId) {
        Company company = companyMapper.selectOne(new LambdaQueryWrapper<Company>()
                .eq(Company::getUserId, userId).last("limit 1"));
        if (company == null) {
            return;
        }
        context.setCompanyId(company.getId());
        context.setCompanyName(company.getCompanyName());
        List<Job> jobs = jobMapper.selectList(new LambdaQueryWrapper<Job>()
                .eq(Job::getCompanyId, company.getId()));
        context.setCompanyJobCount(jobs.size());
        context.setCompanyOnlineJobCount((int) jobs.stream()
                .filter(job -> CommonConstants.JOB_ONLINE.equals(job.getStatus())).count());
        List<Long> jobIds = jobs.stream().map(Job::getId).toList();
        if (!jobIds.isEmpty()) {
            context.setCompanyPendingApplicationCount(count(applicationMapper.selectCount(
                    new LambdaQueryWrapper<JobApplication>()
                            .in(JobApplication::getJobId, jobIds)
                            .eq(JobApplication::getStatus, CommonConstants.APP_PENDING))));
        }
    }

    private void fillAdmin(ChatContext context) {
        context.setPlatformUserCount(count(userMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getRole, RoleConstants.USER))));
        context.setPlatformCompanyCount(count(companyMapper.selectCount(
                new LambdaQueryWrapper<Company>())));
        context.setPlatformOnlineJobCount(count(jobMapper.selectCount(
                new LambdaQueryWrapper<Job>().eq(Job::getStatus, CommonConstants.JOB_ONLINE))));
        context.setPlatformPendingReportCount(count(reportMapper.selectCount(
                new LambdaQueryWrapper<Report>().eq(Report::getStatus, CommonConstants.REPORT_PENDING))));
    }

    /**
     * 岗位详情页上下文：用于“问这个岗位”场景（含命中/缺失技能对比）
     */
    public ChatContext withJob(ChatContext context, Long jobId, Long userId) {
        if (jobId == null) {
            return context;
        }
        try {
            Job job = jobMapper.selectById(jobId);
            if (job == null) {
                return context;
            }
            context.setFocusJobId(job.getId());
            context.setFocusJobName(job.getJobName());
            context.setFocusJobCity(job.getCity());
            context.setFocusJobEducation(job.getEducation());
            context.setFocusJobExperience(job.getExperience());
            context.setFocusJobSalary(formatSalary(job));
            context.setFocusJobDescription(job.getDescription());
            List<String> jobSkills = jobSkillMapper.selectList(new LambdaQueryWrapper<JobSkill>()
                            .eq(JobSkill::getJobId, jobId))
                    .stream().map(JobSkill::getSkillName).filter(Objects::nonNull).toList();
            context.setFocusJobSkills(jobSkills);
            if (job.getCompanyId() != null) {
                Company company = companyMapper.selectById(job.getCompanyId());
                if (company != null) {
                    context.setFocusJobCompany(company.getCompanyName());
                }
            }
            if (userId != null && RoleConstants.USER.equals(context.getRole())
                    && context.getSkills() != null && !context.getSkills().isEmpty()) {
                List<String> matched = new ArrayList<>();
                List<String> missing = new ArrayList<>();
                for (String skill : jobSkills) {
                    boolean hit = context.getSkills().stream()
                            .anyMatch(owned -> owned.equalsIgnoreCase(skill));
                    if (hit) {
                        matched.add(skill);
                    } else {
                        missing.add(skill);
                    }
                }
                context.setFocusMatchedSkills(matched);
                context.setFocusMissingSkills(missing);
            }
        } catch (Exception e) {
            log.warn("加载岗位上下文失败: {}", e.getMessage());
        }
        return context;
    }

    private String formatSalary(Job job) {
        if (job.getSalaryMin() == null && job.getSalaryMax() == null) {
            return "面议";
        }
        return (job.getSalaryMin() == null ? "?" : job.getSalaryMin() / 1000 + "K")
                + "-" + (job.getSalaryMax() == null ? "?" : job.getSalaryMax() / 1000 + "K");
    }

    private int count(Long value) {
        return value == null ? 0 : value.intValue();
    }
}
