package com.example.recruitment.ai.config;

import com.example.recruitment.ai.llm.LlmClient;
import com.example.recruitment.ai.mapper.AiResumeAnalysisMapper;
import com.example.recruitment.ai.service.ResumeAnalysisService;
import com.example.recruitment.ai.service.ResumeOptimizationService;
import com.example.recruitment.ai.service.impl.MockResumeAnalysisService;
import com.example.recruitment.ai.service.impl.MockResumeOptimizationService;
import com.example.recruitment.ai.service.impl.RealResumeAnalysisService;
import com.example.recruitment.ai.service.impl.RealResumeOptimizationService;
import com.example.recruitment.job.mapper.JobMapper;
import com.example.recruitment.job.mapper.JobSkillMapper;
import com.example.recruitment.resume.mapper.ResumeExperienceMapper;
import com.example.recruitment.resume.mapper.ResumeMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI 服务装配：根据配置（ai.enabled / ai.mock / api-key）自动选择
 * 真实大模型实现或 Mock 演示实现，业务代码无需感知。
 */
@Configuration
public class AiServiceConfig {

    @Bean
    public ResumeAnalysisService resumeAnalysisService(ResumeMapper resumeMapper,
                                                      ResumeExperienceMapper experienceMapper,
                                                      AiResumeAnalysisMapper analysisMapper,
                                                      LlmClient llmClient,
                                                      AiProperties aiProperties) {
        if (aiProperties.realMode()) {
            return new RealResumeAnalysisService(resumeMapper, analysisMapper,
                    experienceMapper, llmClient);
        }
        return new MockResumeAnalysisService(resumeMapper, analysisMapper, experienceMapper);
    }

    @Bean
    public ResumeOptimizationService resumeOptimizationService(ResumeMapper resumeMapper,
                                                              ResumeExperienceMapper experienceMapper,
                                                              JobMapper jobMapper,
                                                              JobSkillMapper jobSkillMapper,
                                                              LlmClient llmClient,
                                                              AiProperties aiProperties) {
        if (aiProperties.realMode()) {
            return new RealResumeOptimizationService(resumeMapper, experienceMapper,
                    jobMapper, jobSkillMapper, llmClient);
        }
        return new MockResumeOptimizationService(resumeMapper, experienceMapper, jobMapper, jobSkillMapper);
    }
}
