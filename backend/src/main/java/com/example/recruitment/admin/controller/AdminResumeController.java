package com.example.recruitment.admin.controller;

import com.example.recruitment.admin.dto.ResumeAdminVO;
import com.example.recruitment.admin.service.AdminResumeService;
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
 * 管理端：简历管理
 */
@Tag(name = "管理端-简历")
@RestController
@RequestMapping("/api/admin/resumes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminResumeController {

    private final AdminResumeService adminResumeService;

    @Operation(summary = "简历分页查询")
    @GetMapping
    public Result<PageResult<ResumeAdminVO>> page(@RequestParam(required = false) String keyword,
                                                  @RequestParam(defaultValue = "1") long page,
                                                  @RequestParam(defaultValue = "10") long size) {
        return Result.ok(adminResumeService.page(keyword, page, size));
    }

    @Operation(summary = "删除简历")
    @DeleteMapping("/{id}")
    @OpLog(module = "简历管理", action = "删除简历")
    public Result<Void> delete(@PathVariable Long id) {
        adminResumeService.delete(id);
        return Result.ok();
    }
}
