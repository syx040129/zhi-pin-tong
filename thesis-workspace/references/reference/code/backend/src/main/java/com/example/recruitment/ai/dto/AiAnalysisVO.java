package com.example.recruitment.ai.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI 简历分析结果 VO
 */
@Data
public class AiAnalysisVO {

    private Long id;
    private Long resumeId;
    private String resumeName;
    private Integer overallScore;
    private Integer skillScore;
    private Integer experienceScore;
    private Integer educationScore;
    private Integer completenessScore;
    private List<String> strengths;
    private List<String> weaknesses;
    private List<String> suggestions;
    /** 适配岗位方向 */
    private List<String> targetJobs;
    /** 来源：LLM / RULE */
    private String source;
    private LocalDateTime createTime;
}
