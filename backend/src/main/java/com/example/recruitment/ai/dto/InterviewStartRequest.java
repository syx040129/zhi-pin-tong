package com.example.recruitment.ai.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * AI 模拟面试开始请求
 */
@Data
public class InterviewStartRequest {

    /** 目标岗位 id（可选，默认按求职方向） */
    private Long jobId;

    @NotBlank(message = "请选择面试难度")
    @Pattern(regexp = "^(简单|中等|困难)$", message = "面试难度只能为 简单/中等/困难")
    private String difficulty;

    @Min(value = 3, message = "题目数量最少 3 道")
    @Max(value = 10, message = "题目数量最多 10 道")
    private Integer questionCount = 5;

    private String title;
}
