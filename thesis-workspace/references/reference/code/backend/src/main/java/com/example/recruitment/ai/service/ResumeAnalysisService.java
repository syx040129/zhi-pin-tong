package com.example.recruitment.ai.service;

import com.example.recruitment.ai.dto.AiAnalysisVO;

/**
 * AI 简历分析服务
 */
public interface ResumeAnalysisService {

    /**
     * 分析简历并保存结果
     */
    AiAnalysisVO analyze(Long resumeId);

    /**
     * 获取最近一次分析结果
     */
    AiAnalysisVO latest(Long resumeId);
}
