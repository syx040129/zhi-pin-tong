package com.example.recruitment.application.controller;

import com.example.recruitment.application.dto.ApplicationForm;
import com.example.recruitment.application.dto.ApplicationVO;
import com.example.recruitment.application.service.ApplicationService;
import com.example.recruitment.common.PageResult;
import com.example.recruitment.common.Result;
import com.example.recruitment.common.utils.SecurityUtils;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 求职者投递接口
 */
@Tag(name = "投递模块")
@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    @Operation(summary = "投递岗位")
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    @OpLog(module = "投递管理", action = "投递岗位")
    public Result<ApplicationVO> apply(@Valid @RequestBody ApplicationForm form) {
        return Result.ok(applicationService.apply(form, SecurityUtils.currentUserId()));
    }

    @Operation(summary = "我的投递记录")
    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public Result<PageResult<ApplicationVO>> myApplications(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return Result.ok(applicationService.myPage(SecurityUtils.currentUserId(), status, page, size));
    }

    @Operation(summary = "投递详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public Result<ApplicationVO> detail(@PathVariable Long id) {
        return Result.ok(applicationService.detail(id, SecurityUtils.currentUserId()));
    }

    @Operation(summary = "撤销投递（仅待处理状态）")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    @OpLog(module = "投递管理", action = "撤销投递")
    public Result<Void> cancel(@PathVariable Long id) {
        applicationService.cancel(id, SecurityUtils.currentUserId());
        return Result.ok();
    }
}
