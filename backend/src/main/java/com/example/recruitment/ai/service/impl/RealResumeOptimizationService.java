package com.example.recruitment.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.recruitment.ai.dto.OptimizeItemVO;
import com.example.recruitment.ai.dto.OptimizeResultVO;
import com.example.recruitment.ai.llm.LlmClient;
import com.example.recruitment.ai.service.JobMatchService;
import com.example.recruitment.ai.service.ResumeOptimizationService;
import com.example.recruitment.common.exception.BusinessException;
import com.example.recruitment.common.utils.JsonUtils;
import com.example.recruitment.job.entity.Job;
import com.example.recruitment.job.entity.JobSkill;
import com.example.recruitment.job.mapper.JobMapper;
import com.example.recruitment.job.mapper.JobSkillMapper;
import com.example.recruitment.resume.entity.Resume;
import com.example.recruitment.resume.entity.ResumeExperience;
import com.example.recruitment.resume.mapper.ResumeExperienceMapper;
import com.example.recruitment.resume.mapper.ResumeMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 真实大模型简历优化：LLM 对照岗位 JD 逐项改写
 */
@Slf4j
public class RealResumeOptimizationService implements ResumeOptimizationService {

    private final ResumeMapper resumeMapper;
    private final ResumeExperienceMapper experienceMapper;
    private final JobMapper jobMapper;
    private final JobSkillMapper jobSkillMapper;
    private final LlmClient llmClient;

    public RealResumeOptimizationService(ResumeMapper resumeMapper,
                                         ResumeExperienceMapper experienceMapper,
                                         JobMapper jobMapper,
                                         JobSkillMapper jobSkillMapper,
                                         LlmClient llmClient) {
        this.resumeMapper = resumeMapper;
        this.experienceMapper = experienceMapper;
        this.jobMapper = jobMapper;
        this.jobSkillMapper = jobSkillMapper;
        this.llmClient = llmClient;
    }

    @Override
    public OptimizeResultVO optimize(Long resumeId, Long jobId) {
        Resume resume = resumeMapper.selectById(resumeId);
        if (resume == null) {
            throw BusinessException.notFound("简历不存在");
        }
        Job job = jobMapper.selectById(jobId);
        if (job == null) {
            throw BusinessException.notFound("岗位不存在");
        }
        List<ResumeExperience> experiences = experienceMapper.selectList(
                new LambdaQueryWrapper<ResumeExperience>()
                        .eq(ResumeExperience::getResumeId, resumeId));
        List<JobSkill> jobSkills = jobSkillMapper.selectList(new LambdaQueryWrapper<JobSkill>()
                .eq(JobSkill::getJobId, jobId));
        String resumeText = JobMatchService.buildResumeText(resume, experiences);
        String jobText = JobMatchService.buildJobText(job, jobSkills);
        try {
            String json = llmClient.chatJson(
                    "你是资深简历优化顾问。请对照目标岗位 JD 优化候选人简历，只输出 JSON："
                            + "{\"summary\":\"整体优化建议一句话\",\"items\":[{\"field\":\"个人简介|项目经历|技能描述|工作经历\","
                            + "\"original\":\"原文(直接引用输入内容)\",\"optimized\":\"改写后内容\","
                            + "\"suggestion\":\"为什么这么改\",\"keywords\":[\"建议补充的关键词\"]}]}。"
                            + "items 覆盖 2-4 个方面。只改善已有事实的表达，不得编造技能、任职、项目、学历或业绩数字。"
                            + "输入没有量化数据时只能建议用户补充真实数据，不得自行生成数字。原文必须直接引用输入。",
                    "目标岗位 JD：\n" + jobText + "\n\n候选人简历：\n" + resumeText);
            OptimizeResultVO parsed = JsonUtils.parse(json, OptimizeResultVO.class);
            if (parsed == null || parsed.getItems() == null || parsed.getItems().isEmpty()) {
                throw new IllegalStateException("模型输出格式异常");
            }
            parsed.setResumeId(resumeId);
            parsed.setJobId(jobId);
            parsed.setJobName(job.getJobName());
            return parsed;
        } catch (Exception e) {
            log.warn("LLM 简历优化失败，降级规则优化: {}", e.getMessage());
            MockResumeOptimizationService fallback = new MockResumeOptimizationService(
                    resumeMapper, experienceMapper, jobMapper, jobSkillMapper);
            return fallback.optimize(resumeId, jobId);
        }
    }
}
