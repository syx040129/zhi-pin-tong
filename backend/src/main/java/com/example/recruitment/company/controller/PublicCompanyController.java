package com.example.recruitment.company.controller;

import com.example.recruitment.common.Result;
import com.example.recruitment.common.PageResult;
import com.example.recruitment.company.dto.CompanyQuery;
import com.example.recruitment.company.dto.CompanyFiltersVO;
import com.example.recruitment.company.dto.PublicCompanyVO;
import com.example.recruitment.company.service.CompanyService;
import jakarta.validation.Valid;
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

    @Operation(summary = "企业搜索/分页", description = "支持名称、行业、规模筛选；默认仅展示有在线岗位的正常企业")
    @GetMapping
    public Result<PageResult<PublicCompanyVO>> page(@Valid CompanyQuery query) {
        return Result.ok(companyService.publicPage(query));
    }

    @Operation(summary = "公开企业筛选选项")
    @GetMapping("/filters")
    public Result<CompanyFiltersVO> filters() {
        return Result.ok(companyService.publicFilters());
    }

    @Operation(summary = "企业详情（含在线岗位数）")
    @GetMapping("/{id}")
    public Result<PublicCompanyVO> detail(@PathVariable Long id) {
        return Result.ok(companyService.publicDetail(id));
    }
}
