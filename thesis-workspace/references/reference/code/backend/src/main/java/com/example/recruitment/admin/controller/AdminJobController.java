package com.example.recruitment.admin.controller;

import com.example.recruitment.admin.dto.AdminStatusRequest;
import com.example.recruitment.common.PageResult;
import com.example.recruitment.common.Result;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端：岗位管理
 */
@Tag(name = "管理端-岗位")
@RestController
@RequestMapping("/api/admin/jobs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminJobController {

    private final JobService jobService;

    @Operation(summary = "岗位分页查询")
    @GetMapping
    public Result<PageResult<JobVO>> page(@RequestParam(required = false) String keyword,
                                          @RequestParam(required = false) String jobType,
                                          @RequestParam(required = false) String status,
                                          @RequestParam(defaultValue = "1") long page,
                                          @RequestParam(defaultValue = "10") long size) {
        return Result.ok(jobService.adminPage(keyword, jobType, status, page, size));
    }

    @Operation(summary = "上架/下架岗位")
    @PutMapping("/{id}/status")
    @OpLog(module = "岗位管理", action = "上下架岗位")
    public Result<Void> setStatus(@PathVariable Long id,
                                  @Valid @RequestBody AdminStatusRequest request) {
        jobService.adminSetStatus(id, request.getStatus());
        return Result.ok();
    }

    @Operation(summary = "删除岗位")
    @DeleteMapping("/{id}")
    @OpLog(module = "岗位管理", action = "删除岗位")
    public Result<Void> delete(@PathVariable Long id) {
        jobService.adminDelete(id);
        return Result.ok();
    }
}
