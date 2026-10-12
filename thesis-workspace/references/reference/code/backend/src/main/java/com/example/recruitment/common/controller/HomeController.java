package com.example.recruitment.common.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.recruitment.common.Result;
import com.example.recruitment.common.config.AppCacheService;
import com.example.recruitment.common.constant.CommonConstants;
import com.example.recruitment.common.constant.RoleConstants;
import com.example.recruitment.common.utils.JsonUtils;
import com.example.recruitment.company.entity.Company;
import com.example.recruitment.company.mapper.CompanyMapper;
import com.example.recruitment.job.entity.Job;
import com.example.recruitment.job.mapper.JobMapper;
import com.example.recruitment.application.entity.JobApplication;
import com.example.recruitment.application.mapper.JobApplicationMapper;
import com.example.recruitment.user.entity.SysUser;
import com.example.recruitment.user.mapper.SysUserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 首页统计接口（游客可访问）
 */
@Tag(name = "首页统计")
@RestController
@RequestMapping("/api/home")
@RequiredArgsConstructor
public class HomeController {

    private final SysUserMapper userMapper;
    private final CompanyMapper companyMapper;
    private final JobMapper jobMapper;
    private final JobApplicationMapper applicationMapper;
    private final AppCacheService cacheService;

    @Operation(summary = "平台数据统计")
    @GetMapping("/stats")
    public Result<Map<String, Long>> stats() {
        String cached = cacheService.get("home:stats");
        if (cached != null) {
            Map<String, Long> parsed = JsonUtils.parse(cached,
                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, Long>>() {
                    });
            if (parsed != null) {
                return Result.ok(parsed);
            }
        }
        Map<String, Long> data = new LinkedHashMap<>();
        data.put("jobCount", jobMapper.selectCount(new LambdaQueryWrapper<Job>()
                .eq(Job::getStatus, CommonConstants.JOB_ONLINE)));
        data.put("companyCount", companyMapper.selectCount(new LambdaQueryWrapper<Company>()
                .eq(Company::getStatus, CommonConstants.COMPANY_ACTIVE)));
        data.put("userCount", userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getRole, RoleConstants.USER)
                .eq(SysUser::getStatus, CommonConstants.USER_ENABLED)));
        data.put("applicationCount", applicationMapper.selectCount(
                new LambdaQueryWrapper<JobApplication>()));
        cacheService.set("home:stats", JsonUtils.toJson(data), 300);
        return Result.ok(data);
    }
}
