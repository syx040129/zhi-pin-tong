package com.example.recruitment.job.dto;

import lombok.Data;

/**
 * 岗位搜索条件
 */
@Data
public class JobQuery {

    private String keyword;
    private String city;
    private String jobType;
    private String education;
    private String experience;
    private Integer salaryMin;
    private Integer salaryMax;
    /** default / hot / salary */
    private String sort;
    private long page = 1;
    private long size = 10;
}
