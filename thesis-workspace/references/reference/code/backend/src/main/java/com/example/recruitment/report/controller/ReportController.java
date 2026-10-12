package com.example.recruitment.report.controller;

import com.example.recruitment.common.PageResult;
import com.example.recruitment.common.Result;
import com.example.recruitment.common.utils.SecurityUtils;
import com.example.recruitment.log.annotation.OpLog;
import com.example.recruitment.report.dto.ReportForm;
import com.example.recruitment.report.dto.ReportVO;
import com.example.recruitment.report.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 举报接口
 */
@Tag(name = "举报模块")
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @Operation(summary = "举报岗位或企业")
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @OpLog(module = "举报管理", action = "提交举报")
    public Result<Void> create(@Valid @RequestBody ReportForm form) {
        reportService.create(form, SecurityUtils.currentUserId());
        return Result.ok();
    }

    @Operation(summary = "我的举报记录")
    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public Result<PageResult<ReportVO>> my(@RequestParam(defaultValue = "1") long page,
                                           @RequestParam(defaultValue = "10") long size) {
        return Result.ok(reportService.myPage(SecurityUtils.currentUserId(), page, size));
    }
}
