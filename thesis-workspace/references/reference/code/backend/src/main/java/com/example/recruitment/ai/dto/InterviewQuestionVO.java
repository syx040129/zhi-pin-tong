package com.example.recruitment.ai.dto;

import lombok.Data;

/**
 * 面试题目 VO
 */
@Data
public class InterviewQuestionVO {

    private Long id;
    private String question;
    private String questionType;
    private String difficulty;
    private Integer sort;
    /** 用户回答（回答后返回） */
    private String answer;
    private Integer score;
    private String feedback;
}
