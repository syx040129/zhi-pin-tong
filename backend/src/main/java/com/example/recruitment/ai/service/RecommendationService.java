package com.example.recruitment.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.recruitment.ai.config.AiProperties;
import com.example.recruitment.ai.dto.MatchResultVO;
import com.example.recruitment.ai.dto.RecommendItemVO;
import com.example.recruitment.ai.dto.RecommendationQuery;
import com.example.recruitment.common.exception.BusinessException;
import com.example.recruitment.ai.entity.AiRecommendation;
import com.example.recruitment.ai.mapper.AiRecommendationMapper;
import com.example.recruitment.common.constant.CommonConstants;
import com.example.recruitment.company.entity.Company;
import com.example.recruitment.company.mapper.CompanyMapper;
import com.example.recruitment.job.entity.Job;
import com.example.recruitment.job.entity.JobSkill;
import com.example.recruitment.job.mapper.JobMapper;
import com.example.recruitment.job.mapper.JobSkillMapper;
import com.example.recruitment.resume.entity.Resume;
import com.example.recruitment.resume.mapper.ResumeMapper;
import com.example.recruitment.resume.service.ResumeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * AI 智能岗位推荐服务：基于简历技能画像与岗位技能/语义相似度计算候选池，
 * 将匹配结果持久化到 ai_recommendation，支持 6 小时内缓存。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final AiRecommendationMapper recommendationMapper;
    private final JobMapper jobMapper;
    private final JobSkillMapper jobSkillMapper;
    private final CompanyMapper companyMapper;
    private final ResumeMapper resumeMapper;
    private final JobMatchService jobMatchService;
    private final AiProperties aiProperties;

    @org.springframework.transaction.annotation.Transactional
    public List<RecommendItemVO> recommendations(Long userId, boolean force) {
        return recommendations(userId, force, new RecommendationQuery());
    }

    @org.springframework.transaction.annotation.Transactional
    public List<RecommendItemVO> recommendations(Long userId, boolean force, RecommendationQuery query) {
        if (!query.isSalaryRangeValid()) throw BusinessException.badRequest("最高薪资不能低于最低薪资");
        Resume resume = selectResume(userId, query.getResumeId());
        if (resume == null) return List.of();
        // 自选简历和意向不写入默认缓存，避免影响其他页面显示的匹配分。
        if (query.personalized()) return refresh(userId, resume, query, false);
        List<AiRecommendation> cached = recommendationMapper.selectList(
                new LambdaQueryWrapper<AiRecommendation>()
                        .eq(AiRecommendation::getUserId, userId)
                        .orderByDesc(AiRecommendation::getMatchScore)
                        .orderByDesc(AiRecommendation::getId)
                        .last("limit " + aiProperties.getRecommendLimit()));
        if (!force && !cached.isEmpty() && cached.stream().allMatch(rec -> isFresh(rec)
                && (resume.getUpdateTime() == null || !rec.getCreateTime().isBefore(resume.getUpdateTime())))) {
            return toItems(cached, resume);
        }
        return refresh(userId, resume, query, true);
    }

    @org.springframework.transaction.annotation.Transactional
    public List<RecommendItemVO> refresh(Long userId) {
        return recommendations(userId, true);
    }

    private Resume selectResume(Long userId, Long resumeId) {
        if (resumeId == null) return resumeMapper.selectOne(new LambdaQueryWrapper<Resume>()
                .eq(Resume::getUserId, userId).orderByDesc(Resume::getId).last("limit 1"));
        Resume resume = resumeMapper.selectById(resumeId);
        if (resume == null || !userId.equals(resume.getUserId())) {
            throw BusinessException.notFound("简历不存在或无权访问");
        }
        return resume;
    }

    private List<RecommendItemVO> refresh(Long userId, Resume resume, RecommendationQuery query, boolean persist) {
        List<Job> onlineJobs = jobMapper.selectList(new LambdaQueryWrapper<Job>()
                .eq(Job::getStatus, CommonConstants.JOB_ONLINE)
                .eq(StringUtils.hasText(query.getCity()), Job::getCity,
                        query.getCity() == null ? null : query.getCity().trim())
                .eq(StringUtils.hasText(query.getJobType()), Job::getJobType,
                        query.getJobType() == null ? null : query.getJobType().trim())
                .ge(query.getSalaryMin() != null, Job::getSalaryMax, query.getSalaryMin())
                .le(query.getSalaryMax() != null, Job::getSalaryMin, query.getSalaryMax())
                .orderByDesc(Job::getId));
        if (onlineJobs.isEmpty()) {
            return List.of();
        }
        List<JobSkill> allSkills = jobSkillMapper.selectList(new LambdaQueryWrapper<JobSkill>()
                .in(JobSkill::getJobId, onlineJobs.stream().map(Job::getId).toList()));
        Map<Long, List<JobSkill>> skillMap = allSkills.stream()
                .collect(Collectors.groupingBy(JobSkill::getJobId));

        List<String> resumeSkills = ResumeService.splitSkills(resume.getSkills()).stream()
                .map(s -> s.toLowerCase(Locale.ROOT).replaceAll("\\s+", "")).toList();
        List<Job> candidates = onlineJobs.stream().sorted(Comparator
                .<Job>comparingDouble(job -> skillCoverage(resumeSkills, skillMap.getOrDefault(job.getId(), List.of())))
                .reversed().thenComparing(Job::getId, Comparator.reverseOrder()))
                .limit(Math.max(1, aiProperties.getRecommendPoolSize())).toList();

        Map<Long, MatchResultVO> matches = jobMatchService.computeBatch(resume, candidates, skillMap);
        List<Long> companyIds = candidates.stream().map(Job::getCompanyId).distinct().toList();
        Map<Long, Company> companies = companyMapper.selectBatchIds(companyIds).stream()
                .collect(Collectors.toMap(Company::getId, Function.identity()));
        List<RecommendItemVO> items = new ArrayList<>();
        for (Job job : candidates) {
            RecommendItemVO item = toItem(job, matches.get(job.getId()), companies.get(job.getCompanyId()));
            item.setResumeId(resume.getId());
            items.add(item);
        }
        items.sort(Comparator.comparingInt(RecommendItemVO::getMatchScore).reversed());
        List<RecommendItemVO> top = items.stream()
                .limit(aiProperties.getRecommendLimit()).toList();

        if (persist) {
            recommendationMapper.delete(new LambdaQueryWrapper<AiRecommendation>()
                    .eq(AiRecommendation::getUserId, userId));
            for (RecommendItemVO item : top) {
                AiRecommendation rec = new AiRecommendation();
                rec.setUserId(userId);
                rec.setJobId(item.getJobId());
                rec.setMatchScore(item.getMatchScore());
                rec.setSkillScore(item.getSkillScore());
                rec.setExperienceScore(item.getExperienceScore());
                rec.setEducationScore(item.getEducationScore());
                rec.setSemanticScore(item.getSemanticScore());
                rec.setRecommendReason(item.getReason());
                recommendationMapper.insert(rec);
            }
        }
        log.info("AI 推荐完成 userId={}, 计算 {} 个岗位，返回 {} 条", userId, candidates.size(), top.size());
        return top;
    }

    private double skillCoverage(List<String> resumeSkills, List<JobSkill> skills) {
        int total = 0;
        int matched = 0;
        for (JobSkill skill : skills) {
            int weight = skill.getWeight() == null ? 1 : Math.max(1, skill.getWeight());
            total += weight;
            if (skill.getSkillName() != null && resumeSkills.contains(
                    skill.getSkillName().toLowerCase(Locale.ROOT).replaceAll("\\s+", ""))) matched += weight;
        }
        return total == 0 ? 0 : (double) matched / total;
    }

    private boolean isFresh(AiRecommendation first) {
        return first.getCreateTime() != null
                && first.getCreateTime().isAfter(LocalDateTime.now().minusHours(6));
    }

    private List<RecommendItemVO> toItems(List<AiRecommendation> recs, Resume resume) {
        List<Long> jobIds = recs.stream().map(AiRecommendation::getJobId).distinct().toList();
        Map<Long, Job> jobs = jobMapper.selectBatchIds(jobIds).stream()
                .collect(Collectors.toMap(Job::getId, Function.identity()));
        List<Long> companyIds = jobs.values().stream().map(Job::getCompanyId).distinct().toList();
        Map<Long, Company> companies = companyIds.isEmpty() ? Map.of()
                : companyMapper.selectBatchIds(companyIds).stream()
                .collect(Collectors.toMap(Company::getId, Function.identity()));
        Map<Long, List<JobSkill>> skillMap = new HashMap<>();
        List<JobSkill> skills = jobSkillMapper.selectList(new LambdaQueryWrapper<JobSkill>()
                .in(JobSkill::getJobId, jobIds));
        for (JobSkill s : skills) {
            skillMap.computeIfAbsent(s.getJobId(), k -> new ArrayList<>()).add(s);
        }
        return recs.stream().filter(rec -> jobs.containsKey(rec.getJobId())
                && CommonConstants.JOB_ONLINE.equals(jobs.get(rec.getJobId()).getStatus())).map(rec -> {
            Job job = jobs.get(rec.getJobId());
            RecommendItemVO item = new RecommendItemVO();
            item.setJobId(rec.getJobId());
            item.setMatchScore(rec.getMatchScore());
            item.setSkillScore(rec.getSkillScore());
            item.setExperienceScore(rec.getExperienceScore());
            item.setEducationScore(rec.getEducationScore());
            item.setSemanticScore(rec.getSemanticScore());
            item.setReason(rec.getRecommendReason());
            item.setResumeId(resume.getId());
            item.setSource("CACHED");
            item.setSemanticSource("CACHED");
            item.setExecutionNote("使用 6 小时内的推荐缓存；刷新可查看本次计算来源");
            if (job != null) {
                item.setJobName(job.getJobName());
                item.setJobType(job.getJobType());
                item.setCity(job.getCity());
                item.setSalaryMin(job.getSalaryMin());
                item.setSalaryMax(job.getSalaryMax());
                item.setEducation(job.getEducation());
                item.setExperience(job.getExperience());
                item.setCompanyId(job.getCompanyId());
                Company company = companies.get(job.getCompanyId());
                if (company != null) {
                    item.setCompanyName(company.getCompanyName());
                    item.setCompanyLogo(company.getLogo());
                }
                List<JobSkill> jobSkills = skillMap.getOrDefault(job.getId(), List.of());
                List<String> resumeSkills = ResumeService.splitSkills(
                        resume == null ? null : resume.getSkills()).stream()
                        .map(s -> s.toLowerCase(Locale.ROOT).replaceAll("\\s+", "")).toList();
                List<String> matched = new ArrayList<>();
                List<String> missing = new ArrayList<>();
                for (JobSkill s : jobSkills) {
                    if (resumeSkills.contains(s.getSkillName().toLowerCase(Locale.ROOT).replaceAll("\\s+", ""))) {
                        matched.add(s.getSkillName());
                    } else {
                        missing.add(s.getSkillName());
                    }
                }
                item.setMatchedSkills(matched);
                item.setMissingSkills(missing);
            }
            return item;
        }).toList();
    }

    private RecommendItemVO toItem(Job job, MatchResultVO match, Company company) {
        RecommendItemVO item = new RecommendItemVO();
        item.setJobId(job.getId());
        item.setJobName(job.getJobName());
        item.setJobType(job.getJobType());
        item.setCity(job.getCity());
        item.setSalaryMin(job.getSalaryMin());
        item.setSalaryMax(job.getSalaryMax());
        item.setEducation(job.getEducation());
        item.setExperience(job.getExperience());
        item.setCompanyId(job.getCompanyId());
        if (company != null) {
            item.setCompanyName(company.getCompanyName());
            item.setCompanyLogo(company.getLogo());
        }
        item.setMatchScore(match.getMatchScore());
        item.setSkillScore(match.getSkillScore());
        item.setExperienceScore(match.getExperienceScore());
        item.setEducationScore(match.getEducationScore());
        item.setSemanticScore(match.getSemanticScore());
        item.setMatchedSkills(match.getMatchedSkills());
        item.setMissingSkills(match.getMissingSkills());
        item.setReason(match.getReason());
        item.setSource(match.getSource());
        item.setSemanticSource(match.getSemanticSource());
        item.setExecutionNote(match.getExecutionNote());
        return item;
    }
}
