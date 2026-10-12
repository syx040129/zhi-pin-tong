package com.example.recruitment.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 模拟面试表
 */
@Data
@TableName("ai_interview")
public class AiInterview {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long jobId;
    private String title;
    private String difficulty;
    private Integer questionCount;
    private Integer totalScore;
    /** ONGOING / COMPLETED */
    private String status;
    /** 面试报告（JSON） */
    private String report;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
