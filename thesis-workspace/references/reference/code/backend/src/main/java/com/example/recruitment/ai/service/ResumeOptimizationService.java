package com.example.recruitment.ai.service;

import com.example.recruitment.ai.dto.OptimizeResultVO;

/**
 * AI 简历优化服务
 */
public interface ResumeOptimizationService {

    /**
     * 针对目标岗位优化简历
     */
    OptimizeResultVO optimize(Long resumeId, Long jobId);
}
