package com.example.recruitment.application.controller;

import com.example.recruitment.application.dto.ApplicationStatusForm;
import com.example.recruitment.application.dto.ApplicationVO;
import com.example.recruitment.application.service.ApplicationService;
import com.example.recruitment.common.PageResult;
import com.example.recruitment.common.Result;
import com.example.recruitment.company.entity.Company;
import com.example.recruitment.company.service.CompanyService;
import com.example.recruitment.log.annotation.OpLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 企业端投递处理接口
 */
@Tag(name = "企业投递处理")
@RestController
@RequestMapping("/api/company/applications")
@RequiredArgsConstructor
public class CompanyApplicationController {

    private final ApplicationService applicationService;
    private final CompanyService companyService;

    @Operation(summary = "收到的投递列表")
    @GetMapping
    @PreAuthorize("hasRole('COMPANY')")
    public Result<PageResult<ApplicationVO>> list(@RequestParam(required = false) Long jobId,
                                                  @RequestParam(required = false) String status,
                                                  @RequestParam(defaultValue = "1") long page,
                                                  @RequestParam(defaultValue = "10") long size) {
        Company company = companyService.currentCompany();
        return Result.ok(applicationService.companyPage(company.getId(), jobId, status, page, size));
    }

    @Operation(summary = "投递/候选人详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('COMPANY')")
    public Result<ApplicationVO> detail(@PathVariable Long id) {
        Company company = companyService.currentCompany();
        return Result.ok(applicationService.companyDetail(id, company.getId()));
    }

    @Operation(summary = "处理投递状态（面试/录用/拒绝等）")
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('COMPANY')")
    @OpLog(module = "企业投递", action = "处理投递")
    public Result<Void> updateStatus(@PathVariable Long id,
                                     @Valid @RequestBody ApplicationStatusForm form) {
        Company company = companyService.currentCompany();
        applicationService.updateStatus(id, company.getId(), form.getStatus());
        return Result.ok();
    }
}
