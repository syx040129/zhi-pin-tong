package com.example.recruitment.company.controller;

import com.example.recruitment.common.Result;
import com.example.recruitment.company.dto.CompanyForm;
import com.example.recruitment.company.dto.CompanyVO;
import com.example.recruitment.company.service.CompanyService;
import com.example.recruitment.file.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 企业端接口
 */
@Tag(name = "企业模块")
@RestController
@RequestMapping("/api/company")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;
    private final FileService fileService;

    @Operation(summary = "企业信息详情")
    @GetMapping("/profile")
    @PreAuthorize("hasRole('COMPANY')")
    public Result<CompanyVO> profile() {
        return Result.ok(companyService.currentProfile());
    }

    @Operation(summary = "修改企业信息")
    @PutMapping("/profile")
    @PreAuthorize("hasRole('COMPANY')")
    public Result<CompanyVO> updateProfile(@Valid @RequestBody CompanyForm form) {
        return Result.ok(companyService.updateProfile(form));
    }

    @Operation(summary = "上传企业 Logo")
    @PostMapping("/logo")
    @PreAuthorize("hasRole('COMPANY')")
    public Result<String> uploadLogo(@RequestParam("file") MultipartFile file) {
        String url = fileService.upload(file, "logo");
        companyService.updateLogo(url);
        return Result.ok(url);
    }
}
