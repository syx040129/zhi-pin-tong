package com.example.recruitment.favorite.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 收藏岗位 VO
 */
@Data
public class FavoriteVO {

    private Long id;
    private Long jobId;
    private String jobName;
    private String jobType;
    private String city;
    private Integer salaryMin;
    private Integer salaryMax;
    private String education;
    private String experience;
    private String jobStatus;
    private Long companyId;
    private String companyName;
    private String companyLogo;
    private LocalDateTime createTime;
}
