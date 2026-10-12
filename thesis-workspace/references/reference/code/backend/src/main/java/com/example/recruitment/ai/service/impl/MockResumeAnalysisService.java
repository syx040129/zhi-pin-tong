package com.example.recruitment.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.recruitment.ai.dto.AiAnalysisVO;
import com.example.recruitment.ai.mapper.AiResumeAnalysisMapper;
import com.example.recruitment.ai.service.AbstractResumeAnalysisService;
import com.example.recruitment.common.exception.BusinessException;
import com.example.recruitment.resume.entity.Resume;
import com.example.recruitment.resume.entity.ResumeExperience;
import com.example.recruitment.resume.mapper.ResumeExperienceMapper;
import com.example.recruitment.resume.mapper.ResumeMapper;
import com.example.recruitment.resume.service.ResumeService;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Mock 简历分析：基于规则与内容特征给出可解释的分析（无 API Key 也可演示）
 */
public class MockResumeAnalysisService extends AbstractResumeAnalysisService {

    private final ResumeExperienceMapper experienceMapper;

    public MockResumeAnalysisService(ResumeMapper resumeMapper,
                                     AiResumeAnalysisMapper analysisMapper,
                                     ResumeExperienceMapper experienceMapper) {
        super(resumeMapper, analysisMapper);
        this.experienceMapper = experienceMapper;
    }

    @Override
    public AiAnalysisVO analyze(Long resumeId) {
        Resume resume = resumeMapper.selectById(resumeId);
        if (resume == null) {
            throw BusinessException.notFound("简历不存在");
        }
        List<String> skills = ResumeService.splitSkills(resume.getSkills());
        List<ResumeExperience> experiences = experienceMapper.selectList(
                new LambdaQueryWrapper<ResumeExperience>()
                        .eq(ResumeExperience::getResumeId, resumeId));

        AiAnalysisVO vo = new AiAnalysisVO();
        vo.setResumeId(resumeId);
        vo.setResumeName(resume.getName());
        vo.setCompletenessScore(resume.getCompletionScore() == null ? 0 : resume.getCompletionScore());
        vo.setSkillScore(Math.min(96, 45 + skills.size() * 8 + (containsHotSkill(skills) ? 12 : 0)));
        vo.setEducationScore(educationBase(resume.getEducation()));
        vo.setExperienceScore(experienceBase(resume, experiences));
        vo.setOverallScore(Math.round(vo.getSkillScore() * 0.3f + vo.getExperienceScore() * 0.3f
                + vo.getEducationScore() * 0.2f + vo.getCompletenessScore() * 0.2f));
        vo.setStrengths(buildStrengths(skills, resume));
        vo.setWeaknesses(buildWeaknesses(resume, experiences));
        vo.setSuggestions(buildSuggestions(experiences, skills));
        vo.setTargetJobs(detectTargetJobs(skills));
        vo.setSource("RULE");
        persist(resumeId, vo);
        return vo;
    }

    private boolean containsHotSkill(List<String> skills) {
        List<String> hot = List.of("java", "spring", "mysql", "redis", "vue", "python", "docker");
        String joined = String.join(",", skills).toLowerCase();
        return hot.stream().anyMatch(joined::contains);
    }

    private int educationBase(String education) {
        return switch (education == null ? "" : education) {
            case "博士" -> 96;
            case "硕士" -> 92;
            case "本科" -> 86;
            case "大专" -> 74;
            default -> 58;
        };
    }

    private int experienceBase(Resume resume, List<ResumeExperience> experiences) {
        long work = experiences.stream()
                .filter(e -> "WORK".equals(e.getType()) || "INTERNSHIP".equals(e.getType())).count();
        long project = experiences.stream().filter(e -> "PROJECT".equals(e.getType())).count();
        int base = work >= 1 ? 84 : (project >= 2 ? 80 : (project >= 1 ? 72 : 55));
        if (StringUtils.hasText(resume.getSummary()) && resume.getSummary().length() > 100) {
            base = Math.min(100, base + 8);
        }
        return base;
    }

    private List<String> buildStrengths(List<String> skills, Resume resume) {
        List<String> list = new ArrayList<>();
        if (!skills.isEmpty()) {
            list.add("已掌握 " + String.join("、", skills.subList(0, Math.min(5, skills.size())))
                    + " 等关键技术");
        }
        if (StringUtils.hasText(resume.getSchool())) {
            list.add("毕业于 " + resume.getSchool() + (StringUtils.hasText(resume.getMajor())
                    ? "，" + resume.getMajor() + " 专业" : "") + "，专业基础扎实");
        }
        if (StringUtils.hasText(resume.getSummary()) && resume.getSummary().length() > 50) {
            list.add("个人简介信息量充足，能清楚表达职业定位与能力");
        }
        if (list.isEmpty()) {
            list.add("具备学习意愿与可塑性，建议完善简历信息后继续分析");
        }
        return list;
    }

    private List<String> buildWeaknesses(Resume resume, List<ResumeExperience> experiences) {
        List<String> list = new ArrayList<>();
        if (experiences.stream().noneMatch(e -> "WORK".equals(e.getType())
                || "INTERNSHIP".equals(e.getType()))) {
            list.add("缺少正式工作/实习经历，需突出项目经历弥补");
        }
        if (experiences.stream().filter(e -> "PROJECT".equals(e.getType())).count() < 2) {
            list.add("项目经历数量偏少，建议补充 1-2 个有代表性的项目");
        }
        if (!StringUtils.hasText(resume.getEmail())) {
            list.add("未填写邮箱，可能影响企业联系");
        }
        if (list.isEmpty()) {
            list.add("项目描述多为职责罗列，建议补充量化成果（性能提升/用户规模等）");
        }
        return list;
    }

    private List<String> buildSuggestions(List<ResumeExperience> experiences, List<String> skills) {
        List<String> list = new ArrayList<>();
        if (experiences.isEmpty()) {
            list.add("补充教育经历与项目经历，使用 STAR 法则描述（背景-任务-行动-结果）");
        }
        list.add("用数据量化项目成果，例如“将接口 QPS 提升 40%”“支撑 10 万用户访问”");
        if (skills.size() < 5) {
            list.add("丰富技能关键词，补充主流框架/中间件以提升简历检索命中率");
        }
        list.add("围绕目标岗位定制个人简介，突出与该岗位最相关的 2-3 项核心能力");
        return list;
    }

    private List<String> detectTargetJobs(List<String> skills) {
        List<String> jobs = new ArrayList<>();
        if (containsIgnoreCase(skills, "java") || containsIgnoreCase(skills, "spring")) {
            jobs.add("Java 开发工程师");
        }
        if (containsIgnoreCase(skills, "vue") || containsIgnoreCase(skills, "react")
                || containsIgnoreCase(skills, "javascript")) {
            jobs.add("前端开发工程师");
        }
        if (containsIgnoreCase(skills, "python") || containsIgnoreCase(skills, "tensorflow")) {
            jobs.add("数据分析 / 算法工程师");
        }
        if (containsIgnoreCase(skills, "mysql") || containsIgnoreCase(skills, "redis")
                || containsIgnoreCase(skills, "docker")) {
            jobs.add("后端开发 / 运维开发工程师");
        }
        if (jobs.isEmpty()) {
            jobs.add("软件开发工程师");
        }
        return jobs;
    }

    private boolean containsIgnoreCase(List<String> skills, String keyword) {
        return skills.stream().anyMatch(s -> s.toLowerCase().contains(keyword));
    }
}
