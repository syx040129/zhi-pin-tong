package com.example.recruitment.company.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.recruitment.application.entity.JobApplication;
import com.example.recruitment.application.mapper.JobApplicationMapper;
import com.example.recruitment.common.mapper.StatsMapper;
import com.example.recruitment.company.entity.Company;
import com.example.recruitment.job.entity.Job;
import com.example.recruitment.job.mapper.JobMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 企业数据看板服务
 */
@Service
@RequiredArgsConstructor
public class CompanyStatsService {

    private final CompanyService companyService;
    private final JobMapper jobMapper;
    private final JobApplicationMapper applicationMapper;
    private final StatsMapper statsMapper;

    public Map<String, Object> stats() {
        Company company = companyService.currentCompany();
        List<Long> jobIds = jobMapper.selectList(new LambdaQueryWrapper<Job>()
                        .eq(Job::getCompanyId, company.getId()))
                .stream().map(Job::getId).toList();
        long jobCount = jobMapper.selectCount(new LambdaQueryWrapper<Job>()
                .eq(Job::getCompanyId, company.getId()));
        long onlineCount = jobMapper.selectCount(new LambdaQueryWrapper<Job>()
                .eq(Job::getCompanyId, company.getId())
                .eq(Job::getStatus, "ONLINE"));
        long applicationCount = 0;
        long pendingCount = 0;
        long interviewCount = 0;
        long hiredCount = 0;
        if (!jobIds.isEmpty()) {
            applicationCount = applicationMapper.selectCount(new LambdaQueryWrapper<JobApplication>()
                    .in(JobApplication::getJobId, jobIds));
            pendingCount = applicationMapper.selectCount(new LambdaQueryWrapper<JobApplication>()
                    .in(JobApplication::getJobId, jobIds).eq(JobApplication::getStatus, "PENDING"));
            interviewCount = applicationMapper.selectCount(new LambdaQueryWrapper<JobApplication>()
                    .in(JobApplication::getJobId, jobIds).eq(JobApplication::getStatus, "INTERVIEW"));
            hiredCount = applicationMapper.selectCount(new LambdaQueryWrapper<JobApplication>()
                    .in(JobApplication::getJobId, jobIds).eq(JobApplication::getStatus, "HIRED"));
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("jobCount", jobCount);
        data.put("onlineJobCount", onlineCount);
        data.put("applicationCount", applicationCount);
        data.put("pendingCount", pendingCount);
        data.put("interviewCount", interviewCount);
        data.put("hiredCount", hiredCount);
        data.put("applicationStatus", statsMapper.companyApplicationStatus(company.getId()));
        return data;
    }
}
