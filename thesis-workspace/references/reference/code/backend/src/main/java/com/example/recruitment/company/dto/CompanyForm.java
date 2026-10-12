package com.example.recruitment.company.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 企业信息维护请求
 */
@Data
public class CompanyForm {

    @NotBlank(message = "企业名称不能为空")
    @Size(max = 100, message = "企业名称最长 100 个字符")
    private String companyName;

    @Size(max = 50, message = "所属行业最长 50 个字符")
    private String industry;

    @Size(max = 50, message = "企业规模最长 50 个字符")
    private String scale;

    @Size(max = 255, message = "地址最长 255 个字符")
    private String address;

    private String description;

    @Size(max = 255, message = "官网最长 255 个字符")
    private String website;
}
