package com.example.recruitment.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 简历分析结果表
 */
@Data
@TableName("ai_resume_analysis")
public class AiResumeAnalysis {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long resumeId;
    private Integer overallScore;
    private Integer skillScore;
    private Integer experienceScore;
    private Integer educationScore;
    private Integer completenessScore;
    /** JSON 数组 */
    private String strengths;
    /** JSON 数组 */
    private String weaknesses;
    /** JSON 数组 */
    private String suggestions;
    /** 完整分析文本 */
    private String analysisResult;
    private LocalDateTime createTime;
}
