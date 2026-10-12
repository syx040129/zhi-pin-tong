package com.example.recruitment.ai.dto;

import lombok.Data;

import java.util.List;

/**
 * AI 岗位推荐项
 */
@Data
public class RecommendItemVO {

    private Long jobId;
    private String jobName;
    private String jobType;
    private String city;
    private Integer salaryMin;
    private Integer salaryMax;
    private String education;
    private String experience;
    private Long companyId;
    private String companyName;
    private String companyLogo;
    private Integer matchScore;
    private Integer skillScore;
    private Integer experienceScore;
    private Integer educationScore;
    private Integer semanticScore;
    private List<String> matchedSkills;
    private List<String> missingSkills;
    private String reason;
}
