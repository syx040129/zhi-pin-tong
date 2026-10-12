package com.example.recruitment.file.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.recruitment.application.entity.JobApplication;
import com.example.recruitment.application.mapper.JobApplicationMapper;
import com.example.recruitment.common.exception.BusinessException;
import com.example.recruitment.common.utils.SecurityUtils;
import com.example.recruitment.company.entity.Company;
import com.example.recruitment.company.mapper.CompanyMapper;
import com.example.recruitment.job.entity.Job;
import com.example.recruitment.job.mapper.JobMapper;
import com.example.recruitment.resume.entity.Resume;
import com.example.recruitment.resume.mapper.ResumeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ResumeFileAccessService {
    private final FileService fileService;
    private final ResumeMapper resumeMapper;
    private final CompanyMapper companyMapper;
    private final JobMapper jobMapper;
    private final JobApplicationMapper applicationMapper;

    public Path requireFile(String url) {
        var user = SecurityUtils.currentUser();
        List<Resume> resumes = resumeMapper.selectList(new LambdaQueryWrapper<Resume>()
                .eq(Resume::getResumeFileUrl, url));
        boolean allowed = "ADMIN".equals(user.getRole()) || resumes.stream()
                .anyMatch(r -> Objects.equals(r.getUserId(), user.getId()));
        if (!allowed && "COMPANY".equals(user.getRole()) && !resumes.isEmpty()) {
            Company company = companyMapper.selectOne(new LambdaQueryWrapper<Company>()
                    .eq(Company::getUserId, user.getId()).last("limit 1"));
            if (company != null) {
                List<Long> jobIds = jobMapper.selectList(new LambdaQueryWrapper<Job>()
                        .eq(Job::getCompanyId, company.getId())).stream().map(Job::getId).toList();
                if (!jobIds.isEmpty()) {
                    allowed = applicationMapper.selectCount(new LambdaQueryWrapper<JobApplication>()
                            .in(JobApplication::getJobId, jobIds)
                            .in(JobApplication::getResumeId, resumes.stream().map(Resume::getId).toList())) > 0;
                }
            }
        }
        if (!allowed) throw BusinessException.notFound("简历附件不存在或无权访问");
        Path path = fileService.resolveByUrl(url);
        if (path == null || !Files.isRegularFile(path)) {
            throw BusinessException.notFound("简历附件不存在");
        }
        return path;
    }
}
