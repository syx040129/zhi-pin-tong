package com.example.recruitment.job.controller;

import com.example.recruitment.common.PageResult;
import com.example.recruitment.common.Result;
import com.example.recruitment.job.dto.JobForm;
import com.example.recruitment.job.dto.JobVO;
import com.example.recruitment.job.service.JobService;
import com.example.recruitment.log.annotation.OpLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 企业端岗位管理接口
 */
@Tag(name = "企业岗位管理")
@RestController
@RequestMapping("/api/company/jobs")
@RequiredArgsConstructor
public class CompanyJobController {

    private final JobService jobService;

    @Operation(summary = "我的岗位列表")
    @GetMapping
    @PreAuthorize("hasRole('COMPANY')")
    public Result<PageResult<JobVO>> list(@RequestParam(defaultValue = "1") long page,
                                          @RequestParam(defaultValue = "10") long size,
                                          @RequestParam(required = false) String keyword,
                                          @RequestParam(required = false) String status) {
        return Result.ok(jobService.companyJobPage(page, size, keyword, status));
    }

    @Operation(summary = "发布岗位")
    @PostMapping
    @PreAuthorize("hasRole('COMPANY')")
    @OpLog(module = "企业岗位", action = "发布岗位")
    public Result<JobVO> create(@Valid @RequestBody JobForm form) {
        return Result.ok(jobService.create(form));
    }

    @Operation(summary = "修改岗位")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('COMPANY')")
    @OpLog(module = "企业岗位", action = "修改岗位")
    public Result<JobVO> update(@PathVariable Long id, @Valid @RequestBody JobForm form) {
        return Result.ok(jobService.update(id, form));
    }

    @Operation(summary = "上架/下架岗位")
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('COMPANY')")
    @OpLog(module = "企业岗位", action = "岗位上下架")
    public Result<Void> setStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        jobService.setStatus(id, body.get("status"));
        return Result.ok();
    }

    @Operation(summary = "删除岗位")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('COMPANY')")
    @OpLog(module = "企业岗位", action = "删除岗位")
    public Result<Void> delete(@PathVariable Long id) {
        jobService.delete(id);
        return Result.ok();
    }
}
