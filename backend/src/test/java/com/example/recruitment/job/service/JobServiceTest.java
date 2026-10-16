package com.example.recruitment.job.service;

import com.example.recruitment.ai.mapper.AiRecommendationMapper;
import com.example.recruitment.application.mapper.JobApplicationMapper;
import com.example.recruitment.common.exception.BusinessException;
import com.example.recruitment.common.security.AuthUser;
import com.example.recruitment.company.entity.Company;
import com.example.recruitment.company.mapper.CompanyMapper;
import com.example.recruitment.favorite.mapper.FavoriteMapper;
import com.example.recruitment.job.entity.Job;
import com.example.recruitment.job.mapper.JobMapper;
import com.example.recruitment.job.mapper.JobSkillMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JobServiceTest {
    private final JobMapper jobs = mock(JobMapper.class);
    private final CompanyMapper companies = mock(CompanyMapper.class);
    private final JobApplicationMapper applications = mock(JobApplicationMapper.class);
    private final JobService service = new JobService(jobs, mock(JobSkillMapper.class), companies,
            mock(FavoriteMapper.class), applications, mock(AiRecommendationMapper.class));

    @AfterEach
    void clearSecurity() { SecurityContextHolder.clearContext(); }

    private void login(String role, long id) {
        var user = new AuthUser(id, "test", "测试", role);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of()));
    }

    private Job job(String status) {
        Job job = new Job(); job.setId(7L); job.setCompanyId(5L); job.setStatus(status); job.setViewCount(10);
        when(jobs.selectById(7L)).thenReturn(job);
        return job;
    }

    @Test
    void offlineJobHiddenFromGuestAndUnrelatedUser() {
        job("OFFLINE");
        assertEquals(404, assertThrows(BusinessException.class, () -> service.detail(7L)).getCode());
        login("USER", 2);
        assertEquals(404, assertThrows(BusinessException.class, () -> service.detail(7L)).getCode());
        verify(jobs, never()).incrementViewCount(any());
        verify(jobs, never()).updateById(any(Job.class));
    }

    @Test
    void onlyOwningCompanyCanReadOfflineJob() {
        job("OFFLINE");
        Company company = new Company(); company.setUserId(9L);
        when(companies.selectById(5L)).thenReturn(company);
        login("COMPANY", 8);
        assertThrows(BusinessException.class, () -> service.detail(7L));
        login("COMPANY", 9);
        assertEquals(7L, service.detail(7L).getId());
        verify(jobs).incrementViewCount(7L);
    }

    @Test
    void adminAndHistoricalApplicantCanReadOfflineJob() {
        job("OFFLINE");
        login("ADMIN", 1);
        assertEquals(7L, service.detail(7L).getId());
        login("USER", 2);
        when(applications.selectCount(any())).thenReturn(1L);
        assertEquals(7L, service.detail(7L).getId());
    }

    @Test
    void publicDetailOnlyIncrementsCounterWithoutWritingJobFields() {
        Job job = job("ONLINE"); job.setJobName("原始岗位");
        assertEquals(11, service.detail(7L).getViewCount());
        verify(jobs).incrementViewCount(7L);
        verify(jobs, never()).updateById(any(Job.class));
    }
}
