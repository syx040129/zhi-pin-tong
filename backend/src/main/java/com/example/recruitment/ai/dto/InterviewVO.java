package com.example.recruitment.ai.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI 面试 VO
 */
@Data
public class InterviewVO {

    private Long id;
    private Long jobId;
    private String jobName;
    private String title;
    private String difficulty;
    private Integer questionCount;
    private Integer totalScore;
    private String status;
    private LocalDateTime createTime;
    private List<InterviewQuestionVO> questions;
}
