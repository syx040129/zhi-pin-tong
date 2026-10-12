package com.example.recruitment.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.recruitment.admin.dto.ResumeAdminVO;
import com.example.recruitment.common.PageResult;
import com.example.recruitment.common.exception.BusinessException;
import com.example.recruitment.resume.entity.Resume;
import com.example.recruitment.resume.entity.ResumeExperience;
import com.example.recruitment.resume.mapper.ResumeExperienceMapper;
import com.example.recruitment.resume.mapper.ResumeMapper;
import com.example.recruitment.user.entity.SysUser;
import com.example.recruitment.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 管理端简历服务
 */
@Service
@RequiredArgsConstructor
public class AdminResumeService {

    private final ResumeMapper resumeMapper;
    private final ResumeExperienceMapper experienceMapper;
    private final SysUserMapper userMapper;

    public PageResult<ResumeAdminVO> page(String keyword, long page, long size) {
        LambdaQueryWrapper<Resume> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            qw.and(w -> w.like(Resume::getName, keyword)
                    .or().like(Resume::getSchool, keyword)
                    .or().like(Resume::getMajor, keyword));
        }
        qw.orderByDesc(Resume::getId);
        Page<Resume> result = resumeMapper.selectPage(new Page<>(page, size), qw);
        List<Long> userIds = result.getRecords().stream().map(Resume::getUserId)
                .distinct().toList();
        Map<Long, SysUser> users = userIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, Function.identity()));
        List<ResumeAdminVO> vos = result.getRecords().stream().map(r -> {
            ResumeAdminVO vo = new ResumeAdminVO();
            vo.setId(r.getId());
            vo.setUserId(r.getUserId());
            SysUser user = users.get(r.getUserId());
            vo.setUserNickname(user == null ? "" : user.getNickname());
            vo.setUserName(r.getName());
            vo.setPhone(r.getPhone());
            vo.setEmail(r.getEmail());
            vo.setEducation(r.getEducation());
            vo.setSchool(r.getSchool());
            vo.setMajor(r.getMajor());
            vo.setSkills(r.getSkills());
            vo.setCompletionScore(r.getCompletionScore());
            vo.setResumeFileUrl(r.getResumeFileUrl());
            vo.setCreateTime(r.getCreateTime());
            vo.setUpdateTime(r.getUpdateTime());
            return vo;
        }).toList();
        return PageResult.map(result, vos);
    }

    public void delete(Long id) {
        Resume resume = resumeMapper.selectById(id);
        if (resume == null) {
            throw BusinessException.notFound("简历不存在");
        }
        resumeMapper.deleteById(id);
        experienceMapper.delete(new LambdaQueryWrapper<ResumeExperience>()
                .eq(ResumeExperience::getResumeId, id));
    }
}
