package com.example.recruitment.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.recruitment.ai.dto.OptimizeItemVO;
import com.example.recruitment.ai.dto.OptimizeResultVO;
import com.example.recruitment.ai.service.ResumeOptimizationService;
import com.example.recruitment.common.exception.BusinessException;
import com.example.recruitment.job.entity.Job;
import com.example.recruitment.job.entity.JobSkill;
import com.example.recruitment.job.mapper.JobMapper;
import com.example.recruitment.job.mapper.JobSkillMapper;
import com.example.recruitment.resume.entity.Resume;
import com.example.recruitment.resume.entity.ResumeExperience;
import com.example.recruitment.resume.mapper.ResumeExperienceMapper;
import com.example.recruitment.resume.mapper.ResumeMapper;
import com.example.recruitment.resume.service.ResumeService;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Mock 简历优化：规则化改写并给出关键词建议（无 API Key 也可演示对比效果）
 */
public class MockResumeOptimizationService implements ResumeOptimizationService {

    private final ResumeMapper resumeMapper;
    private final ResumeExperienceMapper experienceMapper;
    private final JobMapper jobMapper;
    private final JobSkillMapper jobSkillMapper;

    public MockResumeOptimizationService(ResumeMapper resumeMapper,
                                         ResumeExperienceMapper experienceMapper,
                                         JobMapper jobMapper,
                                         JobSkillMapper jobSkillMapper) {
        this.resumeMapper = resumeMapper;
        this.experienceMapper = experienceMapper;
        this.jobMapper = jobMapper;
        this.jobSkillMapper = jobSkillMapper;
    }

    @Override
    public OptimizeResultVO optimize(Long resumeId, Long jobId) {
        Resume resume = resumeMapper.selectById(resumeId);
        if (resume == null) {
            throw BusinessException.notFound("简历不存在");
        }
        Job job = jobMapper.selectById(jobId);
        if (job == null) {
            throw BusinessException.notFound("岗位不存在");
        }
        List<JobSkill> jobSkills = jobSkillMapper.selectList(new LambdaQueryWrapper<JobSkill>()
                .eq(JobSkill::getJobId, jobId));
        List<String> jobSkillNames = jobSkills.stream().map(JobSkill::getSkillName).toList();
        List<String> resumeSkills = ResumeService.splitSkills(resume.getSkills());
        List<String> missing = jobSkillNames.stream()
                .filter(s -> resumeSkills.stream().noneMatch(r -> r.equalsIgnoreCase(s)))
                .toList();
        List<ResumeExperience> experiences = experienceMapper.selectList(
                new LambdaQueryWrapper<ResumeExperience>()
                        .eq(ResumeExperience::getResumeId, resumeId)
                        .orderByAsc(ResumeExperience::getSort));
        ResumeExperience project = experiences.stream()
                .filter(e -> "PROJECT".equals(e.getType())).findFirst().orElse(null);
        ResumeExperience work = experiences.stream()
                .filter(e -> "WORK".equals(e.getType())).findFirst().orElse(null);

        OptimizeResultVO result = new OptimizeResultVO();
        result.setResumeId(resumeId);
        result.setJobId(jobId);
        result.setJobName(job.getJobName());
        result.setItems(new ArrayList<>());

        result.getItems().add(buildSummaryItem(resume, job, resumeSkills));
        result.getItems().add(buildProjectItem(project, work, job, jobSkills));
        result.getItems().add(buildKeywordItem(resume, missing));

        StringBuilder summary = new StringBuilder();
        summary.append("针对【").append(job.getJobName()).append("】岗位要求，重点优化了个人简介、")
                .append(project != null ? "项目经历" : "实习/工作经历")
                .append("与技能关键词。");
        if (!missing.isEmpty()) {
            summary.append("建议补充 ").append(String.join("、", missing)).append(" 相关技能与案例。");
        }
        summary.append("优化后简历与岗位 JD 的相关性将显著提升。");
        result.setSummary(summary.toString());
        return result;
    }

    private OptimizeItemVO buildSummaryItem(Resume resume, Job job, List<String> skills) {
        OptimizeItemVO item = new OptimizeItemVO();
        item.setField("个人简介");
        String original = StringUtils.hasText(resume.getSummary())
                ? resume.getSummary() : "（当前未填写个人简介）";
        item.setOriginal(original);
        String prefix = StringUtils.hasText(resume.getSchool())
                ? resume.getSchool() + (StringUtils.hasText(resume.getMajor()) ? resume.getMajor() + " 专业" : "") + "背景，"
                : "";
        String skillPart = skills.isEmpty() ? ""
                : "熟悉 " + String.join("、", skills.subList(0, Math.min(6, skills.size())));
        item.setOptimized(prefix + skillPart + (StringUtils.hasText(resume.getSummary())
                ? "。" + resume.getSummary().trim() : ""));
        item.setSuggestion("将个人简介与目标岗位 JD 对齐，突出 2-3 项最相关能力，用一句话说明你能带来的价值。");
        return item;
    }

    private OptimizeItemVO buildProjectItem(ResumeExperience project, ResumeExperience work,
                                            Job job, List<JobSkill> jobSkills) {
        OptimizeItemVO item = new OptimizeItemVO();
        item.setField("项目/工作经历");
        ResumeExperience source = project != null ? project : work;
        String original = source == null || !StringUtils.hasText(source.getContent())
                ? "（暂无可优化的项目描述，建议补充完整项目经历后重试）"
                : source.getContent();
        item.setOriginal(original);
        if (source == null) {
            item.setOptimized(original);
            item.setSuggestion("补充 1-2 个与" + job.getJobType() + "相关的项目，按 STAR 法则描述。");
            return item;
        }
        String body = original.replaceAll("\\s+", " ").trim();
        item.setOptimized(body);
        item.setSuggestion("保留真实经历，用“动词 + 技术方案 + 结果”整理表达；只有确有记录时才补充用户量、性能提升等数据，不要添加未使用的岗位技能。");
        return item;
    }

    private OptimizeItemVO buildKeywordItem(Resume resume, List<String> missing) {
        OptimizeItemVO item = new OptimizeItemVO();
        item.setField("技能关键词");
        String original = StringUtils.hasText(resume.getSkills()) ? resume.getSkills() : "（未填写技能）";
        item.setOriginal(original);
        item.setOptimized(original);
        item.setSuggestion(missing.isEmpty() ? "当前技能已基本覆盖岗位要求，可按熟练度整理。"
                : "岗位还要求 " + String.join("、", missing) + "；仅在实际掌握且能提供案例时加入简历，否则作为学习方向。");
        item.setKeywords(missing);
        return item;
    }
}
