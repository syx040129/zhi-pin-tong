package com.example.recruitment.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.recruitment.ai.config.AiProperties;
import com.example.recruitment.ai.dto.MatchResultVO;
import com.example.recruitment.ai.embedding.EmbeddingClient;
import com.example.recruitment.ai.llm.LlmClient;
import com.example.recruitment.job.entity.Job;
import com.example.recruitment.job.entity.JobSkill;
import com.example.recruitment.job.mapper.JobSkillMapper;
import com.example.recruitment.resume.entity.Resume;
import com.example.recruitment.resume.entity.ResumeExperience;
import com.example.recruitment.resume.mapper.ResumeExperienceMapper;
import com.example.recruitment.resume.service.ResumeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 智能岗位匹配核心服务。
 * 综合评分 = 语义匹配 × 50% + 技能匹配 × 25% + 经验匹配 × 15% + 学历匹配 × 10%
 * 语义匹配通过 Embedding 余弦相似度计算；技能/经验/学历使用规则引擎评分。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobMatchService {

    private static final Map<String, Integer> EDUCATION_RANK = Map.of(
            "大专", 1, "本科", 2, "硕士", 3, "博士", 4);

    private static final Map<String, Integer> EXPERIENCE_LEVEL = Map.of(
            "在校生", 1, "应届", 2, "1-3年", 3, "3-5年", 4, "5-10年", 5, "10年以上", 6);

    private final JobSkillMapper jobSkillMapper;
    private final ResumeExperienceMapper resumeExperienceMapper;
    private final EmbeddingClient embeddingClient;
    private final AiProperties aiProperties;
    private final LlmClient llmClient;

    public MatchResultVO compute(Resume resume, Job job, boolean wantReason) {
        List<JobSkill> jobSkills = jobSkillMapper.selectList(new LambdaQueryWrapper<JobSkill>()
                .eq(JobSkill::getJobId, job.getId()));
        return computeWithSkills(resume, job, jobSkills, wantReason);
    }

    public MatchResultVO computeWithSkills(Resume resume, Job job, List<JobSkill> jobSkills,
                                           boolean wantReason) {
        Set<String> resumeSkills = new HashSet<>();
        for (String s : ResumeService.splitSkills(resume.getSkills())) {
            resumeSkills.add(normalize(s));
        }
        List<String> matched = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        int totalWeight = 0;
        int matchedWeight = 0;
        if (jobSkills != null) {
            for (JobSkill skill : jobSkills) {
                int weight = skill.getWeight() == null ? 1 : Math.max(1, skill.getWeight());
                totalWeight += weight;
                String normalizedName = normalize(skill.getSkillName());
                if (resumeSkills.contains(normalizedName)) {
                    matchedWeight += weight;
                    matched.add(skill.getSkillName());
                } else {
                    missing.add(skill.getSkillName());
                }
            }
        }
        int skillScore = totalWeight == 0 ? 100
                : Math.round(matchedWeight * 100f / totalWeight);

        List<ResumeExperience> experiences = loadExperiences(resume.getId());
        int experienceScore = scoreExperience(job, experiences, resume);
        int educationScore = scoreEducation(job.getEducation(), resume.getEducation());
        var semantic = embeddingClient.similarity(buildResumeText(resume, experiences), buildJobText(job, jobSkills));
        int semanticScore = semantic.score();

        int matchScore = Math.round(semanticScore * 0.5f + skillScore * 0.25f
                + experienceScore * 0.15f + educationScore * 0.10f);

        MatchResultVO vo = new MatchResultVO();
        vo.setJobId(job.getId());
        vo.setJobName(job.getJobName());
        vo.setMatchScore(Math.max(0, Math.min(100, matchScore)));
        vo.setSkillScore(skillScore);
        vo.setExperienceScore(experienceScore);
        vo.setEducationScore(educationScore);
        vo.setSemanticScore(semanticScore);
        vo.setMatchedSkills(matched);
        vo.setMissingSkills(missing);
        vo.setSemanticSource(semantic.source());
        vo.setExecutionNote(semantic.explanation());
        var reason = buildReason(job, matched, missing, skillScore, semanticScore,
                wantReason && aiProperties.realMode());
        vo.setReason(reason.text());
        vo.setReasonSource(reason.source());
        if ("FALLBACK".equals(reason.source())) {
            vo.setExecutionNote(vo.getExecutionNote() + "；推荐理由生成失败，改用规则说明");
        }
        vo.setSource("FALLBACK".equals(semantic.source()) || "FALLBACK".equals(reason.source())
                ? "FALLBACK" : ("LLM".equals(reason.source()) || "EMBEDDING".equals(semantic.source()) ? "LLM" : "RULE"));
        return vo;
    }

    private record ReasonResult(String text, String source) {}

    private ReasonResult buildReason(Job job, List<String> matched, List<String> missing,
                               int skillScore, int semanticScore, boolean useLlm) {
        if (useLlm) {
            try {
                String generated = llmClient.chat(
                        "你是一名资深招聘专家。请结合岗位 JD 与候选人的匹配情况，用 80-150 字中文生成个性化推荐理由，"
                                + "语气专业、务实。不要输出 JSON，不要输出标题，直接输出正文。",
                        "岗位：" + job.getJobName() + "，技能匹配分：" + skillScore
                                + "，语义匹配分：" + semanticScore + "，命中技能：" + matched
                                + "，缺失技能：" + missing);
                if (!StringUtils.hasText(generated)) throw new IllegalStateException("Empty reason");
                return new ReasonResult(generated, "LLM");
            } catch (Exception e) {
                log.warn("LLM 生成推荐理由失败: {}", e.getMessage());
            }
        }
        StringBuilder sb = new StringBuilder();
        if (skillScore >= 70) {
            sb.append("你的技术栈与岗位要求高度匹配");
            if (!matched.isEmpty()) {
                sb.append("（如 ").append(String.join("、", matched)).append("）");
            }
            sb.append("。");
        } else if (skillScore >= 40) {
            sb.append("你已具备岗位的部分核心技能，基础较为扎实，可重点强化薄弱环节。");
        } else {
            sb.append("岗位所需核心技能与你当前技能栈重合度不高，建议针对性学习后再投递。");
        }
        if (!missing.isEmpty()) {
            sb.append("建议补充 ").append(String.join("、", missing))
                    .append(" 相关经验，可显著提升竞争力。");
        }
        if (semanticScore >= 75) {
            sb.append("简历整体经历与岗位方向契合，推荐投递。");
        }
        return new ReasonResult(sb.toString(), useLlm ? "FALLBACK" : "RULE");
    }

    private int scoreEducation(String required, String actual) {
        if (!StringUtils.hasText(required) || "不限".equals(required)) {
            return 100;
        }
        int requiredRank = EDUCATION_RANK.getOrDefault(required, 0);
        int actualRank = actual == null ? 0 : EDUCATION_RANK.getOrDefault(actual, 0);
        if (actualRank >= requiredRank) {
            return 100;
        }
        int diff = requiredRank - actualRank;
        return switch (diff) {
            case 1 -> 80;
            case 2 -> 60;
            default -> 40;
        };
    }

    private int scoreExperience(Job job, List<ResumeExperience> experiences, Resume resume) {
        long workCount = experiences.stream()
                .filter(e -> "WORK".equals(e.getType()) || "INTERNSHIP".equals(e.getType())).count();
        long projectCount = experiences.stream().filter(e -> "PROJECT".equals(e.getType())).count();
        int level = EXPERIENCE_LEVEL.getOrDefault(job.getExperience(), 0);
        int base;
        if (level == 0) {
            base = 95;
        } else if (level <= 2) {
            base = workCount >= 1 ? 95 : (projectCount >= 1 ? 80 : 60);
        } else if (level <= 4) {
            base = workCount >= 1 ? 88 : (projectCount >= 2 ? 78 : 55);
        } else {
            base = workCount >= 3 ? 92 : (workCount >= 1 ? 72 : 45);
        }
        if (StringUtils.hasText(resume.getSummary()) && resume.getSummary().length() > 80) {
            base = Math.min(100, base + 4);
        }
        return base;
    }

    private List<ResumeExperience> loadExperiences(Long resumeId) {
        return resumeExperienceMapper.selectList(new LambdaQueryWrapper<ResumeExperience>()
                .eq(ResumeExperience::getResumeId, resumeId)
                .orderByAsc(ResumeExperience::getSort));
    }

    public static String buildResumeText(Resume resume, List<ResumeExperience> experiences) {
        StringBuilder sb = new StringBuilder();
        appendIfPresent(sb, resume.getName());
        appendIfPresent(sb, resume.getEducation() == null ? "" : resume.getEducation());
        appendIfPresent(sb, resume.getSchool());
        appendIfPresent(sb, resume.getMajor());
        if (StringUtils.hasText(resume.getSkills())) {
            sb.append("技能：").append(resume.getSkills()).append("。");
        }
        if (StringUtils.hasText(resume.getSummary())) {
            sb.append("简介：").append(resume.getSummary()).append("。");
        }
        for (ResumeExperience exp : experiences) {
            sb.append('[').append(exp.getTitle()).append(']');
            if (StringUtils.hasText(exp.getCompany())) {
                sb.append(exp.getCompany()).append(' ');
            }
            if (StringUtils.hasText(exp.getStartDate())) {
                sb.append(exp.getStartDate()).append(" ~ ")
                        .append(StringUtils.hasText(exp.getEndDate()) ? exp.getEndDate() : "至今").append(' ');
            }
            if (StringUtils.hasText(exp.getContent())) {
                sb.append(exp.getContent());
            }
            sb.append('。');
        }
        return sb.toString();
    }

    public static String buildJobText(Job job, List<JobSkill> skills) {
        StringBuilder sb = new StringBuilder();
        appendIfPresent(sb, job.getJobName());
        appendIfPresent(sb, job.getJobType());
        appendIfPresent(sb, job.getCity());
        if (StringUtils.hasText(job.getDescription())) {
            sb.append("描述：").append(job.getDescription()).append("。");
        }
        if (StringUtils.hasText(job.getRequirements())) {
            sb.append("要求：").append(job.getRequirements()).append("。");
        }
        if (skills != null && !skills.isEmpty()) {
            sb.append("技能：");
            for (JobSkill skill : skills) {
                sb.append(skill.getSkillName()).append(' ');
            }
            sb.append('。');
        }
        return sb.toString();
    }

    private static void appendIfPresent(StringBuilder sb, String text) {
        if (StringUtils.hasText(text)) {
            sb.append(text).append(' ');
        }
    }

    private static String normalize(String skill) {
        if (skill == null) {
            return "";
        }
        return skill.trim().toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", "")
                .trim();
    }
}
