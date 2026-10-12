package com.example.recruitment.file.service;

import com.example.recruitment.application.mapper.JobApplicationMapper;
import com.example.recruitment.common.exception.BusinessException;
import com.example.recruitment.common.security.AuthUser;
import com.example.recruitment.company.entity.Company;
import com.example.recruitment.company.mapper.CompanyMapper;
import com.example.recruitment.job.entity.Job;
import com.example.recruitment.job.mapper.JobMapper;
import com.example.recruitment.resume.entity.Resume;
import com.example.recruitment.resume.mapper.ResumeMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ResumeFileAccessServiceTest {
    @TempDir Path directory;
    @AfterEach void clearSecurity() { SecurityContextHolder.clearContext(); }

    @Test
    void ownerAndReceivingCompanyOnly() throws Exception {
        FileService files = new FileService(directory.toString());
        ResumeMapper resumes = mock(ResumeMapper.class);
        CompanyMapper companies = mock(CompanyMapper.class);
        JobMapper jobs = mock(JobMapper.class);
        JobApplicationMapper applications = mock(JobApplicationMapper.class);
        var service = new ResumeFileAccessService(files, resumes, companies, jobs, applications);
        String url = "/api/files/resume/202610/test.txt";
        Path file = files.resolveByUrl(url); Files.createDirectories(file.getParent()); Files.writeString(file, "resume");
        Resume resume = new Resume(); resume.setId(9L); resume.setUserId(2L);
        when(resumes.selectList(any())).thenReturn(List.of(resume));
        assertThrows(BusinessException.class, () -> service.requireFile(url));
        login(2L, "USER"); assertEquals(file, service.requireFile(url));
        login(3L, "USER"); assertThrows(BusinessException.class, () -> service.requireFile(url));
        login(4L, "COMPANY");
        Company company = new Company(); company.setId(5L);
        Job job = new Job(); job.setId(7L);
        when(companies.selectOne(any())).thenReturn(company);
        when(jobs.selectList(any())).thenReturn(List.of(job));
        when(applications.selectCount(any())).thenReturn(0L);
        assertThrows(BusinessException.class, () -> service.requireFile(url));
        when(applications.selectCount(any())).thenReturn(1L);
        assertEquals(file, service.requireFile(url));
        login(1L, "ADMIN"); assertEquals(file, service.requireFile(url));
    }

    @Test
    void traversalCannotEscapeUploadDirectory() {
        var files = new FileService(directory.toString());
        assertNull(files.resolveByUrl("/api/files/../../outside.txt"));
        assertNull(files.resolveByUrl("https://example.com/resume.txt"));
    }

    private void login(Long id, String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthUser(id, "test", "test", role), null, List.of()));
    }
}
