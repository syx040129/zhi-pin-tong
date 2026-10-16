package com.example.recruitment.company.dto;

import lombok.Data;

/** 求职端企业信息，仅包含允许公开的字段。 */
@Data
public class PublicCompanyVO {
    private Long id;
    private String companyName;
    private String logo;
    private String industry;
    private String scale;
    private String address;
    private String description;
    private String website;
    private Long jobCount;
}
