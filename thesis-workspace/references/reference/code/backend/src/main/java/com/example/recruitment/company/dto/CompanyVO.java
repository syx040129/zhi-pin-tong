package com.example.recruitment.company.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 企业信息 VO
 */
@Data
public class CompanyVO {

    private Long id;
    private Long userId;
    private String companyName;
    private String logo;
    private String industry;
    private String scale;
    private String address;
    private String description;
    private String website;
    private String status;
    private LocalDateTime createTime;
    /** 在线岗位数量 */
    private Long jobCount;
    /** 后台管理用：账号 */
    private String username;
    private String contactPhone;
}
