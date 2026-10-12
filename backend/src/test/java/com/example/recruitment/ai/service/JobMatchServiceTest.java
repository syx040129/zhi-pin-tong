package com.example.recruitment.ai.service;

import com.example.recruitment.ai.config.AiProperties;
import com.example.recruitment.ai.embedding.EmbeddingClient;
import com.example.recruitment.ai.llm.LlmClient;
import com.example.recruitment.job.entity.Job;
import com.example.recruitment.job.entity.JobSkill;
import com.example.recruitment.job.mapper.JobSkillMapper;
import com.example.recruitment.resume.entity.Resume;
import com.example.recruitment.resume.mapper.ResumeExperienceMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JobMatchServiceTest {
    @Test
    void failedReasonIsReportedSeparatelyFromSuccessfulEmbedding() {
        var properties = new AiProperties(); properties.setEnabled(true); properties.setMock(false); properties.setApiKey("test");
        var embeddings = mock(EmbeddingClient.class);
        var llm = mock(LlmClient.class);
        var experiences = mock(ResumeExperienceMapper.class);
        when(experiences.selectList(any())).thenReturn(List.of());
        when(embeddings.similarity(anyString(), anyString()))
                .thenReturn(new EmbeddingClient.SimilarityResult(80, "EMBEDDING", "真实向量"));
        when(llm.chat(anyString(), anyString())).thenThrow(new IllegalStateException("offline"));
        var service = new JobMatchService(mock(JobSkillMapper.class), experiences, embeddings, properties, llm);
        Resume resume = new Resume(); resume.setId(9L); resume.setSkills("Java"); resume.setEducation("本科");
        Job job = new Job(); job.setId(7L); job.setJobName("Java"); job.setEducation("本科"); job.setExperience("应届");
        JobSkill skill = new JobSkill(); skill.setSkillName("Java"); skill.setWeight(1);
        var result = service.computeWithSkills(resume, job, List.of(skill), true);
        assertEquals("FALLBACK", result.getSource()); assertEquals("EMBEDDING", result.getSemanticSource());
        assertEquals("FALLBACK", result.getReasonSource()); assertTrue(result.getExecutionNote().contains("推荐理由生成失败"));
        assertEquals(84, result.getMatchScore());
        doReturn("成功生成推荐理由").when(llm).chat(anyString(), anyString());
        assertEquals("LLM", service.computeWithSkills(resume, job, List.of(skill), true).getSource());
    }
}
