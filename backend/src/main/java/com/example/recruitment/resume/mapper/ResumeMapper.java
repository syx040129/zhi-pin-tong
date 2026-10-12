package com.example.recruitment.resume.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.recruitment.resume.entity.Resume;
import org.apache.ibatis.annotations.Mapper;

/**
 * 简历 Mapper
 */
@Mapper
public interface ResumeMapper extends BaseMapper<Resume> {
}
