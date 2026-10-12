package com.example.recruitment.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.recruitment.common.mapper.StatsMapper;
import com.example.recruitment.common.constant.RoleConstants;
import com.example.recruitment.report.entity.Report;
import com.example.recruitment.report.mapper.ReportMapper;
import com.example.recruitment.ai.entity.AiInterview;
import com.example.recruitment.ai.mapper.AiInterviewMapper;
import com.example.recruitment.resume.entity.Resume;
import com.example.recruitment.resume.mapper.ResumeMapper;
import com.example.recruitment.user.entity.SysUser;
import com.example.recruitment.user.mapper.SysUserMapper;
import com.example.recruitment.company.entity.Company;
import com.example.recruitment.company.mapper.CompanyMapper;
import com.example.recruitment.job.entity.Job;
import com.example.recruitment.job.mapper.JobMapper;
import com.example.recruitment.application.entity.JobApplication;
import com.example.recruitment.application.mapper.JobApplicationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理端数据统计服务
 */
@Service
@RequiredArgsConstructor
public class AdminStatsService {

    private final SysUserMapper userMapper;
    private final CompanyMapper companyMapper;
    private final JobMapper jobMapper;
    private final JobApplicationMapper applicationMapper;
    private final ResumeMapper resumeMapper;
    private final AiInterviewMapper interviewMapper;
    private final ReportMapper reportMapper;
    private final StatsMapper statsMapper;

    public Map<String, Object> overview() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("totalUsers", userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getRole, RoleConstants.USER)));
        data.put("totalCompanies", companyMapper.selectCount(new LambdaQueryWrapper<Company>()));
        data.put("totalJobs", jobMapper.selectCount(new LambdaQueryWrapper<Job>()));
        data.put("totalApplications", applicationMapper.selectCount(
                new LambdaQueryWrapper<JobApplication>()));
        data.put("totalResumes", resumeMapper.selectCount(new LambdaQueryWrapper<Resume>()));
        data.put("totalInterviews", interviewMapper.selectCount(new LambdaQueryWrapper<AiInterview>()));
        data.put("pendingReports", reportMapper.selectCount(new LambdaQueryWrapper<Report>()
                .eq(Report::getStatus, "PENDING")));
        data.put("jobTypeDistribution", statsMapper.jobTypeDistribution());
        data.put("companyJobTop", statsMapper.companyJobTop());
        return data;
    }

    public Map<String, Object> trends(int days) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("users", toSeries(statsMapper.userTrend(days)));
        data.put("jobs", toSeries(statsMapper.jobTrend(days)));
        data.put("applications", toSeries(statsMapper.applicationTrend(days)));
        return data;
    }

    private List<Map<String, Object>> toSeries(List<Map<String, Object>> rows) {
        return rows.stream().map(row -> {
            Map<String, Object> item = new LinkedHashMap<>();
            Object date = row.get("d");
            Object count = row.get("cnt");
            if (date instanceof Date sqlDate) {
                item.put("date", sqlDate.toLocalDate().toString());
            } else {
                item.put("date", String.valueOf(date));
            }
            item.put("count", count instanceof Number n ? n.longValue() : 0L);
            return item;
        }).toList();
    }
}
