package com.example.recruitment.resume.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.recruitment.common.exception.BusinessException;
import com.example.recruitment.common.utils.SecurityUtils;
import com.example.recruitment.file.service.FileService;
import com.example.recruitment.resume.dto.ExperienceForm;
import com.example.recruitment.resume.dto.ResumeForm;
import com.example.recruitment.resume.dto.ResumeParseVO;
import com.example.recruitment.resume.dto.ResumeVO;
import com.example.recruitment.resume.entity.Resume;
import com.example.recruitment.resume.entity.ResumeExperience;
import com.example.recruitment.resume.mapper.ResumeExperienceMapper;
import com.example.recruitment.resume.mapper.ResumeMapper;
import com.example.recruitment.resume.parse.ParsedResume;
import com.example.recruitment.resume.parse.ResumeTextParserService;
import com.example.recruitment.resume.util.DocumentTextExtractor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * 简历服务
 */
@Service
@RequiredArgsConstructor
public class ResumeService {

    private final ResumeMapper resumeMapper;
    private final ResumeExperienceMapper experienceMapper;
    private final ResumeTextParserService resumeTextParserService;
    private final FileService fileService;
    private final com.example.recruitment.ai.mapper.AiRecommendationMapper recommendationMapper;

    private void invalidateRecommendations(Long userId) {
        recommendationMapper.delete(new LambdaQueryWrapper<com.example.recruitment.ai.entity.AiRecommendation>()
                .eq(com.example.recruitment.ai.entity.AiRecommendation::getUserId, userId));
    }

    public List<ResumeVO> myResumes() {
        List<Resume> resumes = resumeMapper.selectList(new LambdaQueryWrapper<Resume>()
                .eq(Resume::getUserId, SecurityUtils.currentUserId())
                .orderByDesc(Resume::getId));
        return resumes.stream().map(this::toVO).toList();
    }

    public ResumeVO detail(Long id) {
        return toVO(requireOwn(id));
    }

    @Transactional
    public ResumeVO create(ResumeForm form) {
        Resume resume = new Resume();
        resume.setUserId(SecurityUtils.currentUserId());
        applyForm(resume, form);
        resume.setCompletionScore(0);
        resumeMapper.insert(resume);
        saveExperiences(resume.getId(), form.getExperiences());
        recomputeScore(resume.getId());
        invalidateRecommendations(resume.getUserId());
        return detail(resume.getId());
    }

    @Transactional
    public ResumeVO update(Long id, ResumeForm form) {
        Resume resume = requireOwn(id);
        applyForm(resume, form);
        resumeMapper.updateById(resume);
        experienceMapper.delete(new LambdaQueryWrapper<ResumeExperience>()
                .eq(ResumeExperience::getResumeId, id));
        saveExperiences(id, form.getExperiences());
        recomputeScore(id);
        invalidateRecommendations(resume.getUserId());
        return detail(id);
    }

    @Transactional
    public void delete(Long id) {
        Resume resume = requireOwn(id);
        invalidateRecommendations(resume.getUserId());
        resumeMapper.deleteById(id);
        experienceMapper.delete(new LambdaQueryWrapper<ResumeExperience>()
                .eq(ResumeExperience::getResumeId, id));
    }

    /**
     * 上传简历文件
     */
    @Transactional
    public ResumeVO uploadFile(Long id, MultipartFile file) {
        Resume resume = requireOwn(id);
        String url = fileService.upload(file, "resume");
        resume.setResumeFileUrl(url);
        resumeMapper.updateById(resume);
        recomputeScore(id);
        invalidateRecommendations(resume.getUserId());
        return detail(id);
    }

    /**
     * 简历文件 -> 文本 -> 解析预览；保存由用户确认后调用 update 完成
     */
    @Transactional
    public ResumeParseVO parse(Long id) {
        Resume resume = requireOwn(id);
        if (!StringUtils.hasText(resume.getResumeFileUrl())) {
            throw BusinessException.badRequest("请先上传简历文件");
        }
        Path path = fileService.resolveByUrl(resume.getResumeFileUrl());
        if (path == null || !path.toFile().exists()) {
            throw BusinessException.badRequest("简历文件不存在，请重新上传");
        }
        String url = resume.getResumeFileUrl();
        String ext = url.substring(url.lastIndexOf('.') + 1).toLowerCase();
        String text = DocumentTextExtractor.extract(path, ext);
        ParsedResume parsed = resumeTextParserService.parse(text);

        ResumeParseVO vo = new ResumeParseVO();
        vo.setResumeId(id);
        vo.setFileUrl(resume.getResumeFileUrl());
        vo.setFileName(url.substring(url.lastIndexOf('/') + 1));
        vo.setText(text.length() > 3000 ? text.substring(0, 3000) : text);
        vo.setFilled(false);
        vo.setName(parsed.getName());
        vo.setPhone(parsed.getPhone());
        vo.setEmail(parsed.getEmail());
        vo.setEducation(parsed.getEducation());
        vo.setSchool(parsed.getSchool());
        vo.setMajor(parsed.getMajor());
        vo.setSummary(parsed.getSummary());
        vo.setSkills(parsed.getSkills() == null ? List.of() : parsed.getSkills());
        vo.setExperiences(parsed.getExperiences() == null ? List.of() : parsed.getExperiences().stream().map(exp -> {
            ExperienceForm form = new ExperienceForm();
            form.setType(exp.getType()); form.setTitle(exp.getTitle()); form.setCompany(exp.getCompany());
            form.setContent(exp.getContent()); form.setStartDate(exp.getStartDate()); form.setEndDate(exp.getEndDate());
            return form;
        }).toList());
        return vo;
    }

    /**
     * 当前用户最近一份简历（投递/匹配默认使用）
     */
    public Resume latestResume(Long userId) {
        return resumeMapper.selectOne(new LambdaQueryWrapper<Resume>()
                .eq(Resume::getUserId, userId)
                .orderByDesc(Resume::getId).last("limit 1"));
    }

    public Resume requireOwn(Long id) {
        Resume resume = resumeMapper.selectById(id);
        if (resume == null || !Objects.equals(resume.getUserId(), SecurityUtils.currentUserId())) {
            throw BusinessException.notFound("简历不存在");
        }
        return resume;
    }

    private void applyForm(Resume resume, ResumeForm form) {
        if (form.getName() != null) {
            resume.setName(form.getName());
        }
        if (form.getPhone() != null) {
            resume.setPhone(form.getPhone());
        }
        if (form.getEmail() != null) {
            resume.setEmail(form.getEmail());
        }
        if (form.getEducation() != null) {
            resume.setEducation(form.getEducation());
        }
        if (form.getSchool() != null) {
            resume.setSchool(form.getSchool());
        }
        if (form.getMajor() != null) {
            resume.setMajor(form.getMajor());
        }
        if (form.getSkills() != null) {
            resume.setSkills(String.join(",", form.getSkills()));
        }
        if (form.getSummary() != null) {
            resume.setSummary(form.getSummary());
        }
    }

    private void saveExperiences(Long resumeId, List<ExperienceForm> experiences) {
        if (experiences == null) {
            return;
        }
        int sort = 0;
        for (ExperienceForm form : experiences) {
            ResumeExperience exp = new ResumeExperience();
            exp.setResumeId(resumeId);
            exp.setType(form.getType());
            exp.setTitle(form.getTitle());
            exp.setCompany(form.getCompany());
            exp.setContent(form.getContent());
            exp.setStartDate(form.getStartDate());
            exp.setEndDate(form.getEndDate());
            exp.setSort(form.getSort() == null ? sort : form.getSort());
            experienceMapper.insert(exp);
            sort++;
        }
    }

    /**
     * 依据简历字段完整度计算完成度评分
     */
    public void recomputeScore(Long resumeId) {
        Resume resume = resumeMapper.selectById(resumeId);
        if (resume == null) {
            return;
        }
        int score = 0;
        if (StringUtils.hasText(resume.getName())) {
            score += 10;
        }
        if (StringUtils.hasText(resume.getPhone())) {
            score += 10;
        }
        if (StringUtils.hasText(resume.getEmail())) {
            score += 10;
        }
        if (StringUtils.hasText(resume.getSchool())) {
            score += 10;
        }
        if (StringUtils.hasText(resume.getEducation())) {
            score += 10;
        }
        if (StringUtils.hasText(resume.getMajor())) {
            score += 10;
        }
        if (splitSkills(resume.getSkills()).size() >= 3) {
            score += 10;
        }
        if (StringUtils.hasText(resume.getSummary()) && resume.getSummary().length() > 30) {
            score += 10;
        }
        Long expCount = experienceMapper.selectCount(new LambdaQueryWrapper<ResumeExperience>()
                .eq(ResumeExperience::getResumeId, resumeId));
        if (expCount > 0) {
            score += 10;
        }
        if (StringUtils.hasText(resume.getResumeFileUrl())) {
            score += 10;
        }
        resume.setCompletionScore(Math.min(100, score));
        resumeMapper.updateById(resume);
    }

    public ResumeVO toVO(Resume resume) {
        ResumeVO vo = new ResumeVO();
        vo.setId(resume.getId());
        vo.setUserId(resume.getUserId());
        vo.setName(resume.getName());
        vo.setPhone(resume.getPhone());
        vo.setEmail(resume.getEmail());
        vo.setEducation(resume.getEducation());
        vo.setSchool(resume.getSchool());
        vo.setMajor(resume.getMajor());
        vo.setSkills(splitSkills(resume.getSkills()));
        vo.setSummary(resume.getSummary());
        vo.setResumeFileUrl(resume.getResumeFileUrl());
        vo.setCompletionScore(resume.getCompletionScore());
        vo.setCreateTime(resume.getCreateTime());
        vo.setUpdateTime(resume.getUpdateTime());
        vo.setExperiences(loadExperiences(resume.getId()));
        return vo;
    }

    public List<ResumeExperience> loadExperiences(Long resumeId) {
        return experienceMapper.selectList(new LambdaQueryWrapper<ResumeExperience>()
                .eq(ResumeExperience::getResumeId, resumeId)
                .orderByAsc(ResumeExperience::getSort));
    }

    public List<ResumeExperience> loadExperiencesForResume(Long resumeId) {
        return loadExperiences(resumeId);
    }

    public static List<String> splitSkills(String skills) {
        if (!StringUtils.hasText(skills)) {
            return List.of();
        }
        return java.util.Arrays.stream(skills.split("[,，]"))
                .map(String::trim).filter(s -> !s.isBlank()).distinct().toList();
    }

}
