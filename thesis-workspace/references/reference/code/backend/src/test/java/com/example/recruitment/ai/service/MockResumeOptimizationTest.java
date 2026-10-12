package com.example.recruitment.ai.service;

import com.example.recruitment.ai.service.impl.MockResumeOptimizationService;
import com.example.recruitment.job.entity.Job;
import com.example.recruitment.job.entity.JobSkill;
import com.example.recruitment.job.mapper.JobMapper;
import com.example.recruitment.job.mapper.JobSkillMapper;
import com.example.recruitment.resume.entity.Resume;
import com.example.recruitment.resume.entity.ResumeExperience;
import com.example.recruitment.resume.mapper.ResumeExperienceMapper;
import com.example.recruitment.resume.mapper.ResumeMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MockResumeOptimizationTest {
    @Test
    void optimizedContentDoesNotInventMetricsOrJobSkills() {
        var resumes = mock(ResumeMapper.class); var experiences = mock(ResumeExperienceMapper.class);
        var jobs = mock(JobMapper.class); var skills = mock(JobSkillMapper.class);
        Resume resume = new Resume(); resume.setSkills("Java"); resume.setSummary("学习 Java");
        Job job = new Job(); job.setJobName("开发工程师"); job.setJobType("后端开发");
        JobSkill skill = new JobSkill(); skill.setSkillName("Kubernetes");
        ResumeExperience project = new ResumeExperience(); project.setType("PROJECT"); project.setTitle("课程项目");
        project.setContent("参与课程项目的需求讨论和 Java 代码编写，负责实现用户信息查询与修改功能。");
        when(resumes.selectById(9L)).thenReturn(resume); when(jobs.selectById(7L)).thenReturn(job);
        when(skills.selectList(any())).thenReturn(List.of(skill)); when(experiences.selectList(any())).thenReturn(List.of(project));
        var result = new MockResumeOptimizationService(resumes, experiences, jobs, skills).optimize(9L, 7L);
        for (var item : result.getItems()) {
            assertFalse(item.getOptimized().contains("30%"));
            assertFalse(item.getOptimized().contains("Kubernetes"));
        }
        assertEquals(project.getContent(), result.getItems().get(1).getOptimized());
    }
}
