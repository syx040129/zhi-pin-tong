package com.example.recruitment.ai.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * AI 面试报告 VO
 */
@Data
public class InterviewReportVO {

    private Long interviewId;
    private String title;
    private Integer totalScore;
    private Integer answeredCount;
    private Integer questionCount;
    private Map<String, Integer> dimensionScores;
    private List<String> strengths;
    private List<String> weaknesses;
    private List<String> suggestions;
}
