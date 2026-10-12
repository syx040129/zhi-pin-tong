package com.example.recruitment.ai.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * AI 简历优化请求
 */
@Data
public class OptimizeRequest {

    @NotNull(message = "请选择要优化的简历")
    private Long resumeId;

    @NotNull(message = "请选择目标岗位")
    private Long jobId;
}
