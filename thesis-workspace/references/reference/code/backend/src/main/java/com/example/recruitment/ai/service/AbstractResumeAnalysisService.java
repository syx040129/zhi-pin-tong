package com.example.recruitment.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.recruitment.ai.dto.AiAnalysisVO;
import com.example.recruitment.ai.entity.AiResumeAnalysis;
import com.example.recruitment.ai.mapper.AiResumeAnalysisMapper;
import com.example.recruitment.common.utils.JsonUtils;
import com.example.recruitment.resume.entity.Resume;
import com.example.recruitment.resume.mapper.ResumeMapper;

import java.util.List;

/**
 * 简历分析公共逻辑：结果落库、VO 组装
 */
public abstract class AbstractResumeAnalysisService implements ResumeAnalysisService {

    protected final ResumeMapper resumeMapper;
    protected final AiResumeAnalysisMapper analysisMapper;

    protected AbstractResumeAnalysisService(ResumeMapper resumeMapper,
                                            AiResumeAnalysisMapper analysisMapper) {
        this.resumeMapper = resumeMapper;
        this.analysisMapper = analysisMapper;
    }

    protected void persist(Long resumeId, AiAnalysisVO vo) {
        analysisMapper.delete(new LambdaQueryWrapper<AiResumeAnalysis>()
                .eq(AiResumeAnalysis::getResumeId, resumeId));
        AiResumeAnalysis entity = new AiResumeAnalysis();
        entity.setResumeId(resumeId);
        entity.setOverallScore(vo.getOverallScore());
        entity.setSkillScore(vo.getSkillScore());
        entity.setExperienceScore(vo.getExperienceScore());
        entity.setEducationScore(vo.getEducationScore());
        entity.setCompletenessScore(vo.getCompletenessScore());
        entity.setStrengths(JsonUtils.fromList(vo.getStrengths()));
        entity.setWeaknesses(JsonUtils.fromList(vo.getWeaknesses()));
        entity.setSuggestions(JsonUtils.fromList(vo.getSuggestions()));
        entity.setAnalysisResult(vo.getTargetJobs() == null ? ""
                : String.join("、", vo.getTargetJobs()));
        analysisMapper.insert(entity);
        vo.setId(entity.getId());
    }

    @Override
    public AiAnalysisVO latest(Long resumeId) {
        AiResumeAnalysis entity = analysisMapper.selectOne(new LambdaQueryWrapper<AiResumeAnalysis>()
                .eq(AiResumeAnalysis::getResumeId, resumeId)
                .orderByDesc(AiResumeAnalysis::getId).last("limit 1"));
        if (entity == null) {
            return null;
        }
        Resume resume = resumeMapper.selectById(resumeId);
        AiAnalysisVO vo = new AiAnalysisVO();
        vo.setId(entity.getId());
        vo.setResumeId(resumeId);
        vo.setResumeName(resume == null ? "" : resume.getName());
        vo.setOverallScore(entity.getOverallScore());
        vo.setSkillScore(entity.getSkillScore());
        vo.setExperienceScore(entity.getExperienceScore());
        vo.setEducationScore(entity.getEducationScore());
        vo.setCompletenessScore(entity.getCompletenessScore());
        vo.setStrengths(JsonUtils.toList(entity.getStrengths()));
        vo.setWeaknesses(JsonUtils.toList(entity.getWeaknesses()));
        vo.setSuggestions(JsonUtils.toList(entity.getSuggestions()));
        String targets = entity.getAnalysisResult();
        vo.setTargetJobs(targets == null || targets.isBlank() ? List.of() : List.of(targets.split("、")));
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }
}
