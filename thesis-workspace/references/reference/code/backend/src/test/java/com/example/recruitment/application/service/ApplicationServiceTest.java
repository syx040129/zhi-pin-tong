package com.example.recruitment.application.service;

import com.example.recruitment.ai.service.JobMatchService;
import com.example.recruitment.application.dto.ApplicationForm;
import com.example.recruitment.application.entity.JobApplication;
import com.example.recruitment.application.mapper.JobApplicationMapper;
import com.example.recruitment.common.exception.BusinessException;
import com.example.recruitment.job.entity.Job;
import com.example.recruitment.job.mapper.JobMapper;
import com.example.recruitment.job.service.JobService;
import com.example.recruitment.resume.entity.Resume;
import com.example.recruitment.resume.mapper.ResumeMapper;
import com.example.recruitment.resume.service.ResumeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {
    @Mock JobApplicationMapper applicationMapper;
    @Mock JobMapper jobMapper;
    @Mock ResumeMapper resumeMapper;
    @Mock ResumeService resumeService;
    @Mock JobService jobService;
    @Mock JobMatchService jobMatchService;
    @InjectMocks ApplicationService service;

    @Test
    void cannotHandleAnotherCompanyApplication() {
        JobApplication application = new JobApplication(); application.setJobId(7L);
        Job job = new Job(); job.setCompanyId(5L);
        when(applicationMapper.selectById(1L)).thenReturn(application);
        when(jobMapper.selectById(7L)).thenReturn(job);
        assertEquals(403, assertThrows(BusinessException.class, () -> service.updateStatus(1L, 6L, "HIRED")).getCode());
        assertThrows(BusinessException.class, () -> service.companyDetail(1L, 6L));
        verify(applicationMapper, never()).updateById(any(JobApplication.class));
    }

    @Test
    void duplicateAndForeignResumeAreRejected() {
        Job job = new Job(); job.setId(7L);
        ApplicationForm form = new ApplicationForm(); form.setJobId(7L); form.setResumeId(9L);
        when(jobService.requireOnlineJob(7L)).thenReturn(job);
        when(applicationMapper.selectCount(any())).thenReturn(1L);
        assertEquals(409, assertThrows(BusinessException.class, () -> service.apply(form, 2L)).getCode());
        when(applicationMapper.selectCount(any())).thenReturn(0L);
        Resume foreign = new Resume(); foreign.setUserId(3L);
        when(resumeMapper.selectById(9L)).thenReturn(foreign);
        assertThrows(BusinessException.class, () -> service.apply(form, 2L));
        verify(applicationMapper, never()).insert(any(JobApplication.class));
    }

    @Test
    void processedOrForeignApplicationCannotBeWithdrawn() {
        JobApplication application = new JobApplication(); application.setUserId(2L); application.setStatus("HIRED");
        when(applicationMapper.selectById(1L)).thenReturn(application);
        assertThrows(BusinessException.class, () -> service.cancel(1L, 2L));
        assertThrows(BusinessException.class, () -> service.cancel(1L, 3L));
        assertThrows(BusinessException.class, () -> service.detail(1L, 3L));
        verify(applicationMapper, never()).deleteById(1L);
    }
}
