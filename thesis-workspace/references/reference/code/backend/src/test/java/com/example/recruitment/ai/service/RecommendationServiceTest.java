package com.example.recruitment.ai.service;

import com.example.recruitment.ai.config.AiProperties;
import com.example.recruitment.ai.entity.AiRecommendation;
import com.example.recruitment.ai.mapper.AiRecommendationMapper;
import com.example.recruitment.company.mapper.CompanyMapper;
import com.example.recruitment.job.entity.Job;
import com.example.recruitment.job.entity.JobSkill;
import com.example.recruitment.job.mapper.JobMapper;
import com.example.recruitment.job.mapper.JobSkillMapper;
import com.example.recruitment.resume.entity.Resume;
import com.example.recruitment.resume.mapper.ResumeMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RecommendationServiceTest {
    private final AiRecommendationMapper recs = mock(AiRecommendationMapper.class);
    private final JobMapper jobs = mock(JobMapper.class);
    private final JobSkillMapper skills = mock(JobSkillMapper.class);
    private final CompanyMapper companies = mock(CompanyMapper.class);
    private final ResumeMapper resumes = mock(ResumeMapper.class);
    private final RecommendationService service = new RecommendationService(recs, jobs, skills, companies,
            resumes, mock(JobMatchService.class), new AiProperties());

    @Test
    void cachedSkillsUseUsersResumeAndOfflineJobsAreFiltered() {
        Resume resume = new Resume(); resume.setId(99L); resume.setUserId(2L); resume.setSkills("Spring Boot,Java");
        AiRecommendation first = cached(7L); AiRecommendation second = cached(8L);
        Job online = new Job(); online.setId(7L); online.setStatus("ONLINE"); online.setCompanyId(5L);
        Job offline = new Job(); offline.setId(8L); offline.setStatus("OFFLINE"); offline.setCompanyId(5L);
        JobSkill skill = new JobSkill(); skill.setJobId(7L); skill.setSkillName("springboot");
        when(recs.selectList(any())).thenReturn(List.of(first, second));
        when(resumes.selectOne(any())).thenReturn(resume);
        when(jobs.selectBatchIds(any())).thenReturn(List.of(online, offline));
        when(companies.selectBatchIds(any())).thenReturn(List.of());
        when(skills.selectList(any())).thenReturn(List.of(skill));
        var results = service.recommendations(2L, false);
        assertEquals(1, results.size());
        assertEquals(List.of("springboot"), results.get(0).getMatchedSkills());
        verify(resumes, never()).selectById(2L);
    }

    @Test
    void anyStaleRowOrResumeChangeForcesRefresh() {
        Resume resume = new Resume(); resume.setId(99L); resume.setUserId(2L);
        resume.setUpdateTime(LocalDateTime.now().minusMinutes(10));
        AiRecommendation newer = cached(7L);
        AiRecommendation older = cached(8L); older.setCreateTime(LocalDateTime.now().minusHours(8));
        when(recs.selectList(any())).thenReturn(List.of(newer, older));
        when(resumes.selectOne(any())).thenReturn(resume);
        when(jobs.selectList(any())).thenReturn(List.of());
        assertTrue(service.recommendations(2L, false).isEmpty());
        verify(jobs).selectList(any());
        clearInvocations(jobs);
        when(recs.selectList(any())).thenReturn(List.of(newer));
        resume.setUpdateTime(LocalDateTime.now());
        assertTrue(service.recommendations(2L, false).isEmpty());
        verify(jobs).selectList(any());
    }

    private AiRecommendation cached(Long jobId) {
        AiRecommendation rec = new AiRecommendation(); rec.setJobId(jobId); rec.setUserId(2L);
        rec.setMatchScore(80); rec.setCreateTime(LocalDateTime.now().minusMinutes(1)); return rec;
    }
}
