package com.example.recruitment.job.controller;

import com.example.recruitment.common.PageResult;
import com.example.recruitment.common.Result;
import com.example.recruitment.job.dto.JobQuery;
import com.example.recruitment.job.dto.JobVO;
import com.example.recruitment.job.service.JobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 岗位公开接口
 */
@Tag(name = "岗位搜索")
@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class PublicJobController {

    private final JobService jobService;

    @Operation(summary = "岗位搜索/分页", description = "支持关键词、城市、类型、学历、经验、薪资区间筛选")
    @GetMapping("/page")
    public Result<PageResult<JobVO>> page(JobQuery query) {
        return Result.ok(jobService.publicPage(query));
    }

    @Operation(summary = "岗位详情")
    @GetMapping("/{id}")
    public Result<JobVO> detail(@PathVariable Long id) {
        return Result.ok(jobService.detail(id));
    }
}
