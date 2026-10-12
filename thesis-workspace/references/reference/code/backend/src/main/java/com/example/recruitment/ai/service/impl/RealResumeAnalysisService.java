package com.example.recruitment.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.recruitment.ai.dto.AiAnalysisVO;
import com.example.recruitment.ai.llm.LlmClient;
import com.example.recruitment.ai.mapper.AiResumeAnalysisMapper;
import com.example.recruitment.ai.service.AbstractResumeAnalysisService;
import com.example.recruitment.ai.service.JobMatchService;
import com.example.recruitment.common.exception.BusinessException;
import com.example.recruitment.common.utils.JsonUtils;
import com.example.recruitment.resume.entity.Resume;
import com.example.recruitment.resume.entity.ResumeExperience;
import com.example.recruitment.resume.mapper.ResumeExperienceMapper;
import com.example.recruitment.resume.mapper.ResumeMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 真实大模型简历分析
 */
@Slf4j
public class RealResumeAnalysisService extends AbstractResumeAnalysisService {

    private final ResumeExperienceMapper experienceMapper;
    private final LlmClient llmClient;

    public RealResumeAnalysisService(ResumeMapper resumeMapper,
                                     AiResumeAnalysisMapper analysisMapper,
                                     ResumeExperienceMapper experienceMapper,
                                     LlmClient llmClient) {
        super(resumeMapper, analysisMapper);
        this.experienceMapper = experienceMapper;
        this.llmClient = llmClient;
    }

    @Override
    public AiAnalysisVO analyze(Long resumeId) {
        Resume resume = resumeMapper.selectById(resumeId);
        if (resume == null) {
            throw BusinessException.notFound("简历不存在");
        }
        List<ResumeExperience> experiences = experienceMapper.selectList(
                new LambdaQueryWrapper<ResumeExperience>()
                        .eq(ResumeExperience::getResumeId, resumeId));
        String context = JobMatchService.buildResumeText(resume, experiences);
        try {
            String json = llmClient.chatJson(
                    "你是一名资深 Java 全栈招聘顾问。请对候选人简历进行专业分析并只输出 JSON："
                            + "{\"overallScore\":0-100整数,\"skillScore\":0-100整数,\"experienceScore\":0-100整数,"
                            + "\"educationScore\":0-100整数,\"completenessScore\":0-100整数,"
                            + "\"strengths\":[\"优势\"],\"weaknesses\":[\"不足\"],"
                            + "\"suggestions\":[\"建议\"],\"targetJobs\":[\"适配岗位\"]}。"
                            + "数组使用中文短句，评分必须是整数。",
                    context);
            AiAnalysisVO vo = JsonUtils.parse(json, AiAnalysisVO.class);
            if (vo == null || vo.getOverallScore() == null) {
                throw new IllegalStateException("模型输出格式异常");
            }
            vo.setResumeId(resumeId);
            vo.setResumeName(resume.getName());
            vo.setSource("LLM");
            persist(resumeId, vo);
            return vo;
        } catch (Exception e) {
            log.warn("LLM 简历分析失败，降级规则分析: {}", e.getMessage());
            MockResumeAnalysisService fallback = new MockResumeAnalysisService(resumeMapper,
                    analysisMapper, experienceMapper);
            return fallback.analyze(resumeId);
        }
    }
}
