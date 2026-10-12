package com.example.recruitment.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 岗位推荐结果表
 */
@Data
@TableName("ai_recommendation")
public class AiRecommendation {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long jobId;
    private Integer matchScore;
    private Integer skillScore;
    private Integer experienceScore;
    private Integer educationScore;
    private Integer semanticScore;
    private String recommendReason;
    private LocalDateTime createTime;
}
