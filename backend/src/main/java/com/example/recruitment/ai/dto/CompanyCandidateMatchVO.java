package com.example.recruitment.ai.dto;

import lombok.Data;

import java.util.List;

/**
 * 企业侧候选人 AI 分析 VO
 */
@Data
public class CompanyCandidateMatchVO {

    private Long applicationId;
    private Long candidateUserId;
    private String candidateName;
    private Long jobId;
    private String jobName;
    private MatchResultVO match;
    private List<String> strengths;
    private List<String> risks;
    private List<String> suggestions;
    /** 建议：建议进入面试等 */
    private String advice;
}
