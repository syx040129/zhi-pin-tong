package com.example.recruitment.job.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.recruitment.ai.entity.AiRecommendation;
import com.example.recruitment.ai.mapper.AiRecommendationMapper;
import com.example.recruitment.application.entity.JobApplication;
import com.example.recruitment.application.mapper.JobApplicationMapper;
import com.example.recruitment.common.PageResult;
import com.example.recruitment.common.constant.CommonConstants;
import com.example.recruitment.common.exception.BusinessException;
import com.example.recruitment.common.security.AuthUser;
import com.example.recruitment.common.utils.SecurityUtils;
import com.example.recruitment.company.entity.Company;
import com.example.recruitment.company.mapper.CompanyMapper;
import com.example.recruitment.favorite.entity.Favorite;
import com.example.recruitment.favorite.mapper.FavoriteMapper;
import com.example.recruitment.job.dto.JobForm;
import com.example.recruitment.job.dto.JobQuery;
import com.example.recruitment.job.dto.JobSkillDTO;
import com.example.recruitment.job.dto.JobVO;
import com.example.recruitment.job.entity.Job;
import com.example.recruitment.job.entity.JobSkill;
import com.example.recruitment.job.mapper.JobMapper;
import com.example.recruitment.job.mapper.JobSkillMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 岗位服务：搜索、详情、发布、上下架等
 */
@Service
@RequiredArgsConstructor
public class JobService {

    private final JobMapper jobMapper;
    private final JobSkillMapper jobSkillMapper;
    private final CompanyMapper companyMapper;
    private final FavoriteMapper favoriteMapper;
    private final JobApplicationMapper applicationMapper;
    private final AiRecommendationMapper recommendationMapper;

    /**
     * 求职端岗位搜索（仅在线岗位）
     */
    public PageResult<JobVO> publicPage(JobQuery query) {
        Page<Job> page = jobMapper.selectPage(new Page<>(query.getPage(), query.getSize()),
                buildPublicWrapper(query));
        return buildPageResult(page, true);
    }

    private LambdaQueryWrapper<Job> buildPublicWrapper(JobQuery query) {
        LambdaQueryWrapper<Job> qw = new LambdaQueryWrapper<>();
        qw.eq(Job::getStatus, CommonConstants.JOB_ONLINE);
        if (StringUtils.hasText(query.getKeyword())) {
            qw.and(w -> w.like(Job::getJobName, query.getKeyword())
                    .or().like(Job::getJobType, query.getKeyword()));
        }
        qw.like(StringUtils.hasText(query.getCity()), Job::getCity, query.getCity());
        qw.eq(StringUtils.hasText(query.getJobType()), Job::getJobType, query.getJobType());
        qw.eq(StringUtils.hasText(query.getEducation()), Job::getEducation, query.getEducation());
        qw.eq(StringUtils.hasText(query.getExperience()), Job::getExperience, query.getExperience());
        if (query.getSalaryMin() != null) {
            qw.ge(Job::getSalaryMax, query.getSalaryMin());
        }
        if (query.getSalaryMax() != null) {
            qw.le(Job::getSalaryMin, query.getSalaryMax());
        }
        String sort = query.getSort() == null ? "default" : query.getSort();
        switch (sort) {
            case "hot" -> qw.orderByDesc(Job::getViewCount);
            case "salary" -> qw.orderByDesc(Job::getSalaryMax);
            default -> qw.orderByDesc(Job::getCreateTime);
        }
        return qw;
    }

    public PageResult<JobVO> companyJobPage(long page, long size, String keyword, String status) {
        Long companyId = currentCompanyId();
        LambdaQueryWrapper<Job> qw = new LambdaQueryWrapper<Job>()
                .eq(Job::getCompanyId, companyId)
                .like(StringUtils.hasText(keyword), Job::getJobName, keyword)
                .eq(StringUtils.hasText(status), Job::getStatus, status)
                .orderByDesc(Job::getCreateTime);
        Page<Job> result = jobMapper.selectPage(new Page<>(page, size), qw);
        return buildPageResult(result, false);
    }

    private PageResult<JobVO> buildPageResult(Page<Job> page, boolean enrichUserFlags) {
        return PageResult.map(page, toVOList(page.getRecords(), enrichUserFlags));
    }

    public List<JobVO> toVOList(List<Job> jobs, boolean userFlags) {
        if (jobs.isEmpty()) {
            return List.of();
        }
        AuthUser current = tryCurrentUser();
        List<Long> companyIds = jobs.stream().map(Job::getCompanyId).distinct().toList();
        Map<Long, Company> companies = companyMapper.selectList(
                        new LambdaQueryWrapper<Company>().in(Company::getId, companyIds))
                .stream().collect(Collectors.toMap(Company::getId, Function.identity()));
        Map<Long, List<JobSkill>> skillMap = jobSkillMapper.selectList(
                        new LambdaQueryWrapper<JobSkill>()
                                .in(JobSkill::getJobId, jobs.stream().map(Job::getId).toList()))
                .stream().collect(Collectors.groupingBy(JobSkill::getJobId));

        Set<Long> favoriteIds = Set.of();
        Set<Long> appliedIds = Set.of();
        Map<Long, Integer> matchMap = Map.of();
        if (userFlags && current != null) {
            favoriteIds = favoriteMapper.selectList(new LambdaQueryWrapper<Favorite>()
                            .eq(Favorite::getUserId, current.getId()))
                    .stream().map(Favorite::getJobId).collect(Collectors.toSet());
            appliedIds = applicationMapper.selectList(new LambdaQueryWrapper<JobApplication>()
                            .eq(JobApplication::getUserId, current.getId()))
                    .stream().map(JobApplication::getJobId).collect(Collectors.toSet());
            matchMap = recommendationMapper.selectList(new LambdaQueryWrapper<AiRecommendation>()
                            .eq(AiRecommendation::getUserId, current.getId()))
                    .stream().collect(Collectors.toMap(AiRecommendation::getJobId,
                            AiRecommendation::getMatchScore, (a, b) -> a));
        }
        Set<Long> favIds = favoriteIds;
        Set<Long> appIds = appliedIds;
        Map<Long, Integer> mMap = matchMap;
        return jobs.stream().map(job -> toVO(job, companies.get(job.getCompanyId()),
                skillMap.getOrDefault(job.getId(), List.of()),
                current, favIds.contains(job.getId()),
                appIds.contains(job.getId()),
                mMap.get(job.getId()))).toList();
    }

    public JobVO toVO(Job job, Company company, List<JobSkill> skills, AuthUser current,
                      boolean favorite, boolean applied, Integer cachedMatch) {
        JobVO vo = new JobVO();
        vo.setId(job.getId());
        vo.setCompanyId(job.getCompanyId());
        if (company != null) {
            vo.setCompanyName(company.getCompanyName());
            vo.setCompanyLogo(company.getLogo());
            vo.setCompanyIndustry(company.getIndustry());
        }
        vo.setJobName(job.getJobName());
        vo.setJobType(job.getJobType());
        vo.setSalaryMin(job.getSalaryMin());
        vo.setSalaryMax(job.getSalaryMax());
        vo.setCity(job.getCity());
        vo.setEducation(job.getEducation());
        vo.setExperience(job.getExperience());
        vo.setDescription(job.getDescription());
        vo.setRequirements(job.getRequirements());
        vo.setStatus(job.getStatus());
        vo.setViewCount(job.getViewCount());
        vo.setCreateTime(job.getCreateTime());
        vo.setSkills(skills.stream().map(s -> {
            JobSkillDTO dto = new JobSkillDTO();
            dto.setId(s.getId());
            dto.setSkillName(s.getSkillName());
            dto.setRequiredLevel(s.getRequiredLevel());
            dto.setWeight(s.getWeight());
            return dto;
        }).toList());
        if (current != null) {
            vo.setFavorite(favorite);
            vo.setMatchScore(cachedMatch);
            if (applied) {
                JobApplication app = applicationMapper.selectOne(new LambdaQueryWrapper<JobApplication>()
                        .eq(JobApplication::getUserId, current.getId())
                        .eq(JobApplication::getJobId, job.getId())
                        .orderByDesc(JobApplication::getId).last("limit 1"));
                if (app != null) {
                    vo.setApplicationStatus(app.getStatus());
                }
            }
        }
        return vo;
    }

    /**
     * 岗位详情（公开，浏览量 +1）
     */
    public JobVO detail(Long jobId) {
        Job job = jobMapper.selectById(jobId);
        if (job == null) {
            throw BusinessException.notFound("岗位不存在");
        }
        AuthUser current = tryCurrentUser();
        if (current == null && !CommonConstants.JOB_ONLINE.equals(job.getStatus())) {
            throw BusinessException.notFound("岗位不存在");
        }
        job.setViewCount((job.getViewCount() == null ? 0 : job.getViewCount()) + 1);
        jobMapper.updateById(job);
        Company company = companyMapper.selectById(job.getCompanyId());
        List<JobSkill> skills = jobSkillMapper.selectList(new LambdaQueryWrapper<JobSkill>()
                .eq(JobSkill::getJobId, jobId));
        boolean favorite = false;
        boolean applied = false;
        Integer cachedMatch = null;
        if (current != null) {
            favorite = favoriteMapper.selectCount(new LambdaQueryWrapper<Favorite>()
                    .eq(Favorite::getUserId, current.getId())
                    .eq(Favorite::getJobId, jobId)) > 0;
            JobApplication app = applicationMapper.selectOne(new LambdaQueryWrapper<JobApplication>()
                    .eq(JobApplication::getUserId, current.getId())
                    .eq(JobApplication::getJobId, jobId).last("limit 1"));
            applied = app != null;
            if (app != null) {
                applied = true;
            }
            AiRecommendation rec = recommendationMapper.selectOne(
                    new LambdaQueryWrapper<AiRecommendation>()
                            .eq(AiRecommendation::getUserId, current.getId())
                            .eq(AiRecommendation::getJobId, jobId)
                            .orderByDesc(AiRecommendation::getId).last("limit 1"));
            if (rec != null) {
                cachedMatch = rec.getMatchScore();
            }
        }
        return toVO(job, company, skills, current, favorite, applied, cachedMatch);
    }

    @Transactional
    public JobVO create(JobForm form) {
        Long companyId = currentCompanyId();
        validateSalary(form);
        Job job = new Job();
        applyForm(job, form);
        job.setCompanyId(companyId);
        job.setStatus(CommonConstants.JOB_ONLINE);
        job.setViewCount(0);
        jobMapper.insert(job);
        saveSkills(job.getId(), form.getSkills());
        invalidateRecommendations();
        return detail(job.getId());
    }

    @Transactional
    public JobVO update(Long jobId, JobForm form) {
        Job job = getOwnJob(jobId);
        validateSalary(form);
        applyForm(job, form);
        jobMapper.updateById(job);
        jobSkillMapper.delete(new LambdaQueryWrapper<JobSkill>().eq(JobSkill::getJobId, jobId));
        saveSkills(jobId, form.getSkills());
        invalidateRecommendations();
        return detail(jobId);
    }

    @Transactional
    public void setStatus(Long jobId, String status) {
        if (!(CommonConstants.JOB_ONLINE.equals(status) || CommonConstants.JOB_OFFLINE.equals(status))) {
            throw BusinessException.badRequest("岗位状态参数不正确");
        }
        Job job = getOwnJob(jobId);
        invalidateRecommendations();
        job.setStatus(status);
        jobMapper.updateById(job);
    }

    @Transactional
    public void delete(Long jobId) {
        Job job = getOwnJob(jobId);
        invalidateRecommendations();
        jobMapper.deleteById(jobId);
        jobSkillMapper.delete(new LambdaQueryWrapper<JobSkill>().eq(JobSkill::getJobId, jobId));
    }

    private void applyForm(Job job, JobForm form) {
        job.setJobName(form.getJobName());
        job.setJobType(form.getJobType());
        job.setSalaryMin(form.getSalaryMin());
        job.setSalaryMax(form.getSalaryMax());
        job.setCity(form.getCity());
        job.setEducation(form.getEducation());
        job.setExperience(form.getExperience());
        job.setDescription(form.getDescription());
        job.setRequirements(form.getRequirements());
    }

    private void validateSalary(JobForm form) {
        if (form.getSalaryMax() < form.getSalaryMin()) {
            throw BusinessException.badRequest("最高薪资不能低于最低薪资");
        }
    }

    private void saveSkills(Long jobId, List<JobSkillDTO> skills) {
        if (skills == null) {
            return;
        }
        int i = 0;
        for (JobSkillDTO dto : skills) {
            if (dto.getSkillName() == null || dto.getSkillName().isBlank()) {
                continue;
            }
            JobSkill skill = new JobSkill();
            skill.setJobId(jobId);
            skill.setSkillName(dto.getSkillName().trim());
            skill.setRequiredLevel(dto.getRequiredLevel() == null ? 3 : dto.getRequiredLevel());
            skill.setWeight(dto.getWeight() == null ? 1 : dto.getWeight());
            jobSkillMapper.insert(skill);
            i++;
        }
    }

    private Long currentCompanyId() {
        Long userId = SecurityUtils.currentUserId();
        Company company = companyMapper.selectOne(new LambdaQueryWrapper<Company>()
                .eq(Company::getUserId, userId).last("limit 1"));
        if (company == null) {
            throw BusinessException.notFound("企业档案不存在，请先完善企业信息");
        }
        return company.getId();
    }

    private Job getOwnJob(Long jobId) {
        Job job = jobMapper.selectById(jobId);
        if (job == null) {
            throw BusinessException.notFound("岗位不存在");
        }
        Long companyId = currentCompanyId();
        if (!Objects.equals(job.getCompanyId(), companyId)) {
            throw BusinessException.forbidden("无权操作该岗位");
        }
        return job;
    }

    public Job requireOnlineJob(Long jobId) {
        Job job = jobMapper.selectById(jobId);
        if (job == null || !CommonConstants.JOB_ONLINE.equals(job.getStatus())) {
            throw BusinessException.notFound("岗位不存在或已下架");
        }
        return job;
    }

    public Job getJob(Long jobId) {
        Job job = jobMapper.selectById(jobId);
        if (job == null) {
            throw BusinessException.notFound("岗位不存在");
        }
        return job;
    }
    /**
     * 管理端岗位分页（含上下架岗位）
     */
    public PageResult<JobVO> adminPage(String keyword, String jobType, String status,
                                       long page, long size) {
        LambdaQueryWrapper<Job> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            qw.and(w -> w.like(Job::getJobName, keyword).or().like(Job::getCity, keyword));
        }
        qw.eq(StringUtils.hasText(jobType), Job::getJobType, jobType);
        qw.eq(StringUtils.hasText(status), Job::getStatus, status);
        qw.orderByDesc(Job::getId);
        Page<Job> result = jobMapper.selectPage(new Page<>(page, size), qw);
        return PageResult.map(result, toVOList(result.getRecords(), false));
    }

    /**
     * 管理端上下架岗位
     */
    @Transactional
    public void adminSetStatus(Long jobId, String status) {
        if (!(CommonConstants.JOB_ONLINE.equals(status) || CommonConstants.JOB_OFFLINE.equals(status))) {
            throw BusinessException.badRequest("岗位状态参数不正确");
        }
        Job job = getJob(jobId);
        invalidateRecommendations();
        job.setStatus(status);
        jobMapper.updateById(job);
    }

    /**
     * 管理端删除岗位
     */
    @Transactional
    public void adminDelete(Long jobId) {
        Job job = getJob(jobId);
        invalidateRecommendations();
        jobMapper.deleteById(jobId);
        jobSkillMapper.delete(new LambdaQueryWrapper<JobSkill>().eq(JobSkill::getJobId, jobId));
    }

    private void invalidateRecommendations() {
        recommendationMapper.delete(new LambdaQueryWrapper<AiRecommendation>());
    }

    private AuthUser tryCurrentUser() {
        try {
            return SecurityUtils.currentUser();
        } catch (Exception e) {
            return null;
        }
    }
}
