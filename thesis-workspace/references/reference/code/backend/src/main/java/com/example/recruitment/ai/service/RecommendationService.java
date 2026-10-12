package com.example.recruitment.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.recruitment.ai.config.AiProperties;
import com.example.recruitment.ai.dto.MatchResultVO;
import com.example.recruitment.ai.dto.RecommendItemVO;
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
        List<AiRecommendation> cached = recommendationMapper.selectList(
                new LambdaQueryWrapper<AiRecommendation>()
                        .eq(AiRecommendation::getUserId, userId)
                        .orderByDesc(AiRecommendation::getMatchScore)
                        .orderByDesc(AiRecommendation::getId)
                        .last("limit " + aiProperties.getRecommendLimit()));
        Resume resume = resumeMapper.selectOne(new LambdaQueryWrapper<Resume>()
                .eq(Resume::getUserId, userId).orderByDesc(Resume::getId).last("limit 1"));
        if (resume == null) return List.of();
        if (!force && !cached.isEmpty() && cached.stream().allMatch(rec -> isFresh(rec)
                && (resume.getUpdateTime() == null || rec.getCreateTime().isAfter(resume.getUpdateTime())))) {
            return toItems(cached, resume);
        }
        return refresh(userId);
    }

    @org.springframework.transaction.annotation.Transactional
    public List<RecommendItemVO> refresh(Long userId) {
        Resume resume = resumeMapper.selectOne(new LambdaQueryWrapper<Resume>()
                .eq(Resume::getUserId, userId).orderByDesc(Resume::getId).last("limit 1"));
        if (resume == null) {
            return List.of();
        }
        List<Job> onlineJobs = jobMapper.selectList(new LambdaQueryWrapper<Job>()
                .eq(Job::getStatus, CommonConstants.JOB_ONLINE));
        if (onlineJobs.isEmpty()) {
            return List.of();
        }
        List<JobSkill> allSkills = jobSkillMapper.selectList(new LambdaQueryWrapper<JobSkill>()
                .in(JobSkill::getJobId, onlineJobs.stream().map(Job::getId).toList()));
        Map<Long, List<JobSkill>> skillMap = allSkills.stream()
                .collect(Collectors.groupingBy(JobSkill::getJobId));

        List<String> resumeSkills = ResumeService.splitSkills(resume.getSkills()).stream()
                .map(s -> s.toLowerCase(Locale.ROOT).replaceAll("\\s+", "")).toList();
        List<Job> candidates = new ArrayList<>();
        for (Job job : onlineJobs) {
            List<JobSkill> skills = skillMap.getOrDefault(job.getId(), List.of());
            boolean hit = skills.stream().anyMatch(s -> resumeSkills.contains(
                    s.getSkillName().toLowerCase(Locale.ROOT).replaceAll("\\s+", "")));
            if (hit) {
                candidates.add(job);
            }
            if (candidates.size() >= aiProperties.getRecommendPoolSize()) {
                break;
            }
        }
        if (candidates.isEmpty()) {
            candidates = onlineJobs.stream()
                    .limit(aiProperties.getRecommendPoolSize()).toList();
        }

        List<RecommendItemVO> items = new ArrayList<>();
        for (Job job : candidates) {
            MatchResultVO match = jobMatchService.compute(resume, job, false);
            items.add(toItem(job, match));
        }
        items.sort(Comparator.comparingInt(RecommendItemVO::getMatchScore).reversed());
        List<RecommendItemVO> top = items.stream()
                .limit(aiProperties.getRecommendLimit()).toList();

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
        log.info("AI 推荐完成 userId={}, 计算 {} 个岗位，返回 {} 条", userId, candidates.size(), top.size());
        return top;
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

    private RecommendItemVO toItem(Job job, MatchResultVO match) {
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
        Company company = companyMapper.selectById(job.getCompanyId());
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
        return item;
    }
}
