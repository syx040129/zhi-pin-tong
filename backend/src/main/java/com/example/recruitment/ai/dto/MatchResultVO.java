package com.example.recruitment.ai.dto;

import lombok.Data;

import java.util.List;

/**
 * 智能匹配结果 VO
 */
@Data
public class MatchResultVO {

    private Long jobId;
    private String jobName;
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
    /** LLM / RULE */
    private String source;
    private String semanticSource;
    private String reasonSource;
    private String executionNote;
}
