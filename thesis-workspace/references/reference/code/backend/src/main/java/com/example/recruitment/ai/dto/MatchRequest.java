package com.example.recruitment.ai.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * AI 匹配请求
 */
@Data
public class MatchRequest {

    @NotNull(message = "请选择要匹配的岗位")
    private Long jobId;
    private Long resumeId;
}
