package com.example.recruitment.ai.dto;

import lombok.Data;

import java.util.List;

/**
 * AI 简历优化结果
 */
@Data
public class OptimizeResultVO {

    private Long resumeId;
    private Long jobId;
    private String jobName;
    private String summary;
    private List<OptimizeItemVO> items;
}
