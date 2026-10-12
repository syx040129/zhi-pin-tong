package com.example.recruitment.admin.controller;

import com.example.recruitment.application.dto.ApplicationVO;
import com.example.recruitment.application.service.ApplicationService;
import com.example.recruitment.common.PageResult;
import com.example.recruitment.common.Result;
import com.example.recruitment.log.annotation.OpLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端：投递管理
 */
@Tag(name = "管理端-投递")
@RestController
@RequestMapping("/api/admin/applications")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminApplicationController {

    private final ApplicationService applicationService;

    @Operation(summary = "投递记录分页")
    @GetMapping
    public Result<PageResult<ApplicationVO>> page(@RequestParam(required = false) String keyword,
                                                  @RequestParam(required = false) String status,
                                                  @RequestParam(defaultValue = "1") long page,
                                                  @RequestParam(defaultValue = "10") long size) {
        return Result.ok(applicationService.adminPage(keyword, status, page, size));
    }

    @Operation(summary = "删除投递记录")
    @DeleteMapping("/{id}")
    @OpLog(module = "投递管理", action = "删除投递")
    public Result<Void> delete(@PathVariable Long id) {
        applicationService.adminDelete(id);
        return Result.ok();
    }
}
