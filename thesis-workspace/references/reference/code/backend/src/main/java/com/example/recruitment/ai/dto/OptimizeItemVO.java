package com.example.recruitment.ai.dto;

import lombok.Data;

import java.util.List;

/**
 * 简历优化条目（原文 + AI 优化后）
 */
@Data
public class OptimizeItemVO {

    private String field;
    private String original;
    private String optimized;
    private String suggestion;
    private List<String> keywords;
}
