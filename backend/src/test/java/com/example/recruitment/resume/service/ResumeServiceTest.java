package com.example.recruitment.resume.service;

import com.example.recruitment.ai.mapper.AiRecommendationMapper;
import com.example.recruitment.common.exception.BusinessException;
import com.example.recruitment.common.security.AuthUser;
import com.example.recruitment.file.service.FileService;
import com.example.recruitment.resume.entity.Resume;
import com.example.recruitment.resume.mapper.ResumeExperienceMapper;
import com.example.recruitment.resume.mapper.ResumeMapper;
import com.example.recruitment.resume.parse.ParsedResume;
import com.example.recruitment.resume.parse.ResumeTextParserService;
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

class ResumeServiceTest {
    @TempDir Path directory;
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    @Test
    void parsePreviewsWithoutChangingStoredFieldsOrExperiences() throws Exception {
        ResumeMapper resumes = mock(ResumeMapper.class);
        ResumeExperienceMapper experiences = mock(ResumeExperienceMapper.class);
        ResumeTextParserService parser = mock(ResumeTextParserService.class);
        AiRecommendationMapper recommendations = mock(AiRecommendationMapper.class);
        FileService files = new FileService(directory.toString());
        String url = "/api/files/resume/202610/test.txt";
        Path file = files.resolveByUrl(url); Files.createDirectories(file.getParent()); Files.writeString(file, "new resume");
        Resume resume = new Resume(); resume.setId(9L); resume.setUserId(2L); resume.setName("Original"); resume.setResumeFileUrl(url);
        when(resumes.selectById(9L)).thenReturn(resume);
        ParsedResume parsed = new ParsedResume(); parsed.setName("Parsed"); parsed.setSummary("Preview summary");
        when(parser.parse(anyString())).thenReturn(parsed);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthUser(2L, "test", "test", "USER"), null, List.of()));
        var service = new ResumeService(resumes, experiences, parser, files, recommendations);
        var preview = service.parse(9L);
        assertFalse(preview.isFilled()); assertEquals("Parsed", preview.getName());
        assertEquals("Preview summary", preview.getSummary()); assertEquals("Original", resume.getName());
        verify(resumes, never()).updateById(any(Resume.class));
        verifyNoInteractions(experiences, recommendations);
        resume.setUserId(3L);
        assertThrows(BusinessException.class, () -> service.parse(9L));
    }
}
