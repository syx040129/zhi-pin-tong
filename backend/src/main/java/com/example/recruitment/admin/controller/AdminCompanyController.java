package com.example.recruitment.admin.controller;

import com.example.recruitment.admin.dto.AdminStatusRequest;
import com.example.recruitment.common.PageResult;
import com.example.recruitment.common.Result;
import com.example.recruitment.company.dto.CompanyVO;
import com.example.recruitment.company.service.CompanyService;
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
 * 管理端：企业管理
 */
@Tag(name = "管理端-企业")
@RestController
@RequestMapping("/api/admin/companies")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminCompanyController {

    private final CompanyService companyService;

    @Operation(summary = "企业分页查询")
    @GetMapping
    public Result<PageResult<CompanyVO>> page(@RequestParam(required = false) String keyword,
                                              @RequestParam(required = false) String status,
                                              @RequestParam(defaultValue = "1") long page,
                                              @RequestParam(defaultValue = "10") long size) {
        return Result.ok(companyService.adminPage(page, size, keyword, status));
    }

    @Operation(summary = "启用/禁用企业")
    @PutMapping("/{id}/status")
    @OpLog(module = "企业管理", action = "启用/禁用企业")
    public Result<Void> setStatus(@PathVariable Long id,
                                  @Valid @RequestBody AdminStatusRequest request) {
        companyService.setStatus(id, request.getStatus());
        return Result.ok();
    }

    @Operation(summary = "删除企业（连同账号）")
    @DeleteMapping("/{id}")
    @OpLog(module = "企业管理", action = "删除企业")
    public Result<Void> delete(@PathVariable Long id) {
        companyService.deleteCompany(id);
        return Result.ok();
    }
}
