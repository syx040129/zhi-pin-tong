package com.example.recruitment.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.recruitment.ai.dto.MatchResultVO;
import com.example.recruitment.ai.service.JobMatchService;
import com.example.recruitment.application.dto.ApplicationForm;
import com.example.recruitment.application.dto.ApplicationVO;
import com.example.recruitment.application.entity.JobApplication;
import com.example.recruitment.application.mapper.JobApplicationMapper;
import com.example.recruitment.common.PageResult;
import com.example.recruitment.common.constant.CommonConstants;
import com.example.recruitment.common.exception.BusinessException;
import com.example.recruitment.company.entity.Company;
import com.example.recruitment.company.mapper.CompanyMapper;
import com.example.recruitment.job.entity.Job;
import com.example.recruitment.job.mapper.JobMapper;
import com.example.recruitment.job.service.JobService;
import com.example.recruitment.resume.entity.Resume;
import com.example.recruitment.resume.mapper.ResumeMapper;
import com.example.recruitment.resume.service.ResumeService;
import com.example.recruitment.user.entity.SysUser;
import com.example.recruitment.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

/**
 * 投递服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final JobApplicationMapper applicationMapper;
    private final JobMapper jobMapper;
    private final CompanyMapper companyMapper;
    private final ResumeMapper resumeMapper;
    private final SysUserMapper userMapper;
    private final ResumeService resumeService;
    private final JobService jobService;
    private final JobMatchService jobMatchService;

    @Transactional
    public ApplicationVO apply(ApplicationForm form, Long userId) {
        Job job = jobService.requireOnlineJob(form.getJobId());
        if (isApplied(userId, job.getId())) {
            throw BusinessException.conflict("您已投递过该岗位，请勿重复投递");
        }
        Resume resume;
        if (form.getResumeId() != null) {
            resume = resumeMapper.selectById(form.getResumeId());
            if (resume == null || !Objects.equals(resume.getUserId(), userId)) {
                throw BusinessException.badRequest("请选择本人简历");
            }
        } else {
            resume = resumeService.latestResume(userId);
        }
        if (resume == null) {
            throw BusinessException.badRequest("请先创建并完善简历后再投递");
        }

        Integer matchScore = null;
        try {
            MatchResultVO match = jobMatchService.compute(resume, job, false);
            matchScore = match.getMatchScore();
        } catch (Exception e) {
            log.warn("投递时智能匹配失败，忽略: {}", e.getMessage());
        }

        JobApplication application = new JobApplication();
        application.setJobId(job.getId());
        application.setUserId(userId);
        application.setResumeId(resume.getId());
        application.setStatus(CommonConstants.APP_PENDING);
        application.setCoverLetter(form.getCoverLetter());
        application.setMatchScore(matchScore);
        try {
            applicationMapper.insert(application);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            throw BusinessException.conflict("您已投递过该岗位，请勿重复投递");
        }
        return toVO(application, job, null, resume, null);
    }

    private boolean isApplied(Long userId, Long jobId) {
        return applicationMapper.selectCount(new LambdaQueryWrapper<JobApplication>()
                .eq(JobApplication::getUserId, userId)
                .eq(JobApplication::getJobId, jobId)) > 0;
    }

    public PageResult<ApplicationVO> myPage(Long userId, String status, long page, long size) {
        LambdaQueryWrapper<JobApplication> qw = new LambdaQueryWrapper<JobApplication>()
                .eq(JobApplication::getUserId, userId)
                .eq(StringUtils.hasText(status), JobApplication::getStatus, status)
                .orderByDesc(JobApplication::getId);
        Page<JobApplication> result = applicationMapper.selectPage(new Page<>(page, size), qw);
        return buildPage(result, true, null);
    }

    /**
     * 企业侧投递列表
     */
    public PageResult<ApplicationVO> companyPage(Long companyId, Long jobId, String status,
                                                 long page, long size) {
        List<Long> jobIds = jobMapper.selectList(new LambdaQueryWrapper<Job>()
                        .eq(Job::getCompanyId, companyId))
                .stream().map(Job::getId).toList();
        if (jobIds.isEmpty()) {
            return PageResult.of(List.of(), 0, page, size);
        }
        LambdaQueryWrapper<JobApplication> qw = new LambdaQueryWrapper<JobApplication>()
                .in(JobApplication::getJobId, jobIds)
                .eq(jobId != null, JobApplication::getJobId, jobId)
                .eq(StringUtils.hasText(status), JobApplication::getStatus, status)
                .orderByDesc(JobApplication::getId);
        Page<JobApplication> result = applicationMapper.selectPage(new Page<>(page, size), qw);
        return buildPage(result, false, null);
    }

    public PageResult<ApplicationVO> adminPage(String keyword, String status, long page, long size) {
        LambdaQueryWrapper<JobApplication> qw = new LambdaQueryWrapper<JobApplication>()
                .eq(StringUtils.hasText(status), JobApplication::getStatus, status)
                .orderByDesc(JobApplication::getId);
        if (StringUtils.hasText(keyword)) {
            List<Long> matchedJobIds = jobMapper.selectList(new LambdaQueryWrapper<Job>()
                            .like(Job::getJobName, keyword))
                    .stream().map(Job::getId).toList();
            if (matchedJobIds.isEmpty()) {
                return PageResult.of(List.of(), 0, page, size);
            }
            qw.in(JobApplication::getJobId, matchedJobIds);
        }
        Page<JobApplication> result = applicationMapper.selectPage(new Page<>(page, size), qw);
        return buildPage(result, false, null);
    }

    public void cancel(Long id, Long userId) {
        JobApplication application = applicationMapper.selectById(id);
        if (application == null || !Objects.equals(application.getUserId(), userId)) {
            throw BusinessException.notFound("投递记录不存在");
        }
        if (!CommonConstants.APP_PENDING.equals(application.getStatus())) {
            throw BusinessException.badRequest("该投递已被处理，无法撤销");
        }
        applicationMapper.deleteById(id);
    }

    public ApplicationVO detail(Long id, Long userId) {
        JobApplication application = applicationMapper.selectById(id);
        if (application == null || !Objects.equals(application.getUserId(), userId)) {
            throw BusinessException.notFound("投递记录不存在");
        }
        return toVO(application, null, null, null, null);
    }

    /**
     * 企业更新投递状态
     */
    public void updateStatus(Long id, Long companyId, String status) {
        JobApplication application = applicationMapper.selectById(id);
        if (application == null) {
            throw BusinessException.notFound("投递记录不存在");
        }
        Job job = jobMapper.selectById(application.getJobId());
        if (job == null || !Objects.equals(job.getCompanyId(), companyId)) {
            throw BusinessException.forbidden("无权处理该投递");
        }
        if (status == null || !java.util.Set.of("VIEWED", "INTERVIEW", "REJECTED", "HIRED").contains(status)) {
            throw BusinessException.badRequest("状态参数不正确");
        }
        application.setStatus(status);
        applicationMapper.updateById(application);
    }

    public ApplicationVO companyDetail(Long id, Long companyId) {
        JobApplication application = applicationMapper.selectById(id);
        if (application == null) {
            throw BusinessException.notFound("投递记录不存在");
        }
        Job job = jobMapper.selectById(application.getJobId());
        if (job == null || !Objects.equals(job.getCompanyId(), companyId)) {
            throw BusinessException.forbidden("无权查看该投递");
        }
        return toVO(application, null, null, null, null);
    }

    public void adminDelete(Long id) {
        if (applicationMapper.selectById(id) == null) {
            throw BusinessException.notFound("投递记录不存在");
        }
        applicationMapper.deleteById(id);
    }

    private PageResult<ApplicationVO> buildPage(Page<JobApplication> page, boolean mine, Long companyId) {
        List<JobApplication> records = page.getRecords();
        List<ApplicationVO> vos = records.stream()
                .map(a -> toVO(a, null, null, null, null))
                .toList();
        return PageResult.map(page, vos);
    }

    private ApplicationVO toVO(JobApplication application, Job job, Company company,
                               Resume resume, SysUser user) {
        Job jobEntity = job != null ? job : jobMapper.selectById(application.getJobId());
        Company companyEntity = company;
        Resume resumeEntity = resume;
        SysUser userEntity = user;
        if (companyEntity == null && jobEntity != null) {
            companyEntity = companyMapper.selectById(jobEntity.getCompanyId());
        }
        if (resumeEntity == null && application.getResumeId() != null) {
            resumeEntity = resumeMapper.selectById(application.getResumeId());
        }
        if (userEntity == null) {
            userEntity = userMapper.selectById(application.getUserId());
        }
        ApplicationVO vo = new ApplicationVO();
        vo.setId(application.getId());
        vo.setJobId(application.getJobId());
        if (jobEntity != null) {
            vo.setJobName(jobEntity.getJobName());
            vo.setJobType(jobEntity.getJobType());
            vo.setCity(jobEntity.getCity());
            vo.setSalaryMin(jobEntity.getSalaryMin());
            vo.setSalaryMax(jobEntity.getSalaryMax());
        }
        if (companyEntity != null) {
            vo.setCompanyId(companyEntity.getId());
            vo.setCompanyName(companyEntity.getCompanyName());
            vo.setCompanyLogo(companyEntity.getLogo());
        }
        vo.setUserId(application.getUserId());
        if (userEntity != null) {
            vo.setCandidateName(userEntity.getNickname());
            vo.setCandidateAvatar(userEntity.getAvatar());
            vo.setCandidatePhone(userEntity.getPhone());
            vo.setCandidateEmail(userEntity.getEmail());
        }
        vo.setResumeId(application.getResumeId());
        if (resumeEntity != null) {
            vo.setResumeName(resumeEntity.getName());
        }
        vo.setStatus(application.getStatus());
        vo.setCoverLetter(application.getCoverLetter());
        vo.setMatchScore(application.getMatchScore());
        vo.setCreateTime(application.getCreateTime());
        vo.setUpdateTime(application.getUpdateTime());
        return vo;
    }
}
