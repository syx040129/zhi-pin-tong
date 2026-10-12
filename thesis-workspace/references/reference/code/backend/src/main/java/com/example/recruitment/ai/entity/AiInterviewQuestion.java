package com.example.recruitment.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * AI 面试题目表
 */
@Data
@TableName("ai_interview_question")
public class AiInterviewQuestion {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long interviewId;
    private String question;
    private String questionType;
    private String difficulty;
    /** 用户回答 */
    private String answer;
    private Integer score;
    private String feedback;
    private Integer sort;
}
