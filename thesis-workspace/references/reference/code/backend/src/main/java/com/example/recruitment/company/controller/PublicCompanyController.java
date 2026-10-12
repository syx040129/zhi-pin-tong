package com.example.recruitment.company.controller;

import com.example.recruitment.common.Result;
import com.example.recruitment.company.dto.CompanyVO;
import com.example.recruitment.company.service.CompanyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 企业公开信息接口（求职者查看）
 */
@Tag(name = "企业公开信息")
@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
public class PublicCompanyController {

    private final CompanyService companyService;

    @Operation(summary = "企业详情（含在线岗位数）")
    @GetMapping("/{id}")
    public Result<CompanyVO> detail(@PathVariable Long id) {
        return Result.ok(companyService.publicDetail(id));
    }
}
