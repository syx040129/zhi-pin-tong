package com.example.recruitment.admin.controller;

import com.example.recruitment.common.PageResult;
import com.example.recruitment.common.Result;
import com.example.recruitment.log.annotation.OpLog;
import com.example.recruitment.report.dto.ReportVO;
import com.example.recruitment.report.service.ReportService;
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

import java.util.Map;

/**
 * 管理端：举报管理
 */
@Tag(name = "管理端-举报")
@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminReportController {

    private final ReportService reportService;

    @Operation(summary = "举报分页查询")
    @GetMapping
    public Result<PageResult<ReportVO>> page(@RequestParam(required = false) String status,
                                             @RequestParam(defaultValue = "1") long page,
                                             @RequestParam(defaultValue = "10") long size) {
        return Result.ok(reportService.adminPage(status, page, size));
    }

    @Operation(summary = "处理/忽略举报")
    @PutMapping("/{id}/status")
    @OpLog(module = "举报管理", action = "处理举报")
    public Result<Void> setStatus(@PathVariable Long id,
                                  @Valid @RequestBody Map<String, String> body) {
        reportService.setStatus(id, body.get("status"), body.get("handleRemark"));
        return Result.ok();
    }

    @Operation(summary = "删除举报记录")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        reportService.adminDelete(id);
        return Result.ok();
    }
}
