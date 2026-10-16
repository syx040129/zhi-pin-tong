package com.example.recruitment.ai.service;

import com.example.recruitment.ai.config.AiProperties;
import com.example.recruitment.ai.dto.MatchResultVO;
import com.example.recruitment.ai.dto.RecommendationQuery;
import com.example.recruitment.common.exception.BusinessException;
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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RecommendationServiceTest {
    private final AiRecommendationMapper recs = mock(AiRecommendationMapper.class);
    private final JobMapper jobs = mock(JobMapper.class);
    private final JobSkillMapper skills = mock(JobSkillMapper.class);
    private final CompanyMapper companies = mock(CompanyMapper.class);
    private final ResumeMapper resumes = mock(ResumeMapper.class);
    private final JobMatchService matches = mock(JobMatchService.class);
    private final AiProperties properties = new AiProperties();
    private final RecommendationService service = new RecommendationService(recs, jobs, skills, companies,
            resumes, matches, properties);

    @Test
    void personalizedResumeMustBelongToCurrentUser() {
        Resume foreign = new Resume(); foreign.setUserId(3L);
        when(resumes.selectById(99L)).thenReturn(foreign);
        RecommendationQuery query = new RecommendationQuery(); query.setResumeId(99L);
        assertThrows(BusinessException.class, () -> service.recommendations(2L, false, query));
        verifyNoInteractions(jobs, recs, matches);
    }

    @Test
    void skillCoverageRanksEntirePoolAndPersonalizationDoesNotOverwriteDefaultCache() {
        Resume resume = new Resume(); resume.setId(99L); resume.setUserId(2L); resume.setSkills("Java, Spring Boot");
        when(resumes.selectById(99L)).thenReturn(resume);
        Job weak = new Job(); weak.setId(10L); weak.setCompanyId(5L);
        Job strong = new Job(); strong.setId(1L); strong.setCompanyId(5L);
        JobSkill java = new JobSkill(); java.setJobId(1L); java.setSkillName("Java");
        JobSkill python = new JobSkill(); python.setJobId(10L); python.setSkillName("Python");
        when(jobs.selectList(any())).thenReturn(List.of(weak, strong));
        when(skills.selectList(any())).thenReturn(List.of(java, python));
        when(companies.selectBatchIds(any())).thenReturn(List.of());
        MatchResultVO match = new MatchResultVO(); match.setMatchScore(90); match.setSource("RULE");
        when(matches.computeBatch(eq(resume), anyList(), anyMap())).thenReturn(Map.of(1L, match));
        properties.setRecommendPoolSize(1);
        RecommendationQuery query = new RecommendationQuery(); query.setResumeId(99L); query.setCity("北京");
        var results = service.recommendations(2L, false, query);
        assertEquals(1L, results.get(0).getJobId());
        assertEquals(99L, results.get(0).getResumeId());
        assertEquals("RULE", results.get(0).getSource());
        verify(matches).computeBatch(eq(resume), eq(List.of(strong)), anyMap());
        verifyNoInteractions(recs);
        verify(companies, never()).selectById(any());
    }

    @Test
    void invalidSalaryRangeRejectedBeforeDatabaseAccess() {
        RecommendationQuery query = new RecommendationQuery(); query.setSalaryMin(20); query.setSalaryMax(10);
        assertThrows(BusinessException.class, () -> service.recommendations(2L, false, query));
        verifyNoInteractions(resumes, recs, jobs);
    }

    @Test
    void cachedSkillsUseUsersResumeAndOfflineJobsAreFiltered() {
        Resume resume = new Resume(); resume.setId(99L); resume.setUserId(2L); resume.setSkills("Spring Boot,Java");
        AiRecommendation first = cached(7L); AiRecommendation second = cached(8L);
        // MySQL DATETIME 为秒精度：同一秒完成的简历保存与推荐写入仍可使用缓存。
        resume.setUpdateTime(first.getCreateTime()); second.setCreateTime(first.getCreateTime());
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
        assertEquals("CACHED", results.get(0).getSource());
        verify(jobs, never()).selectList(any());
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
