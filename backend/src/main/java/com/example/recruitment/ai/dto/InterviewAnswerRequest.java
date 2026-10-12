package com.example.recruitment.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 面试作答请求
 */
@Data
public class InterviewAnswerRequest {

    @NotNull(message = "题目参数缺失")
    private Long questionId;

    @NotBlank(message = "回答不能为空")
    @Size(min = 5, max = 4000, message = "回答内容长度需在 5-4000 字之间")
    private String answer;
}
