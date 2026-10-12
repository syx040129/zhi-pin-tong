package com.example.recruitment.application.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.recruitment.application.entity.JobApplication;
import org.apache.ibatis.annotations.Mapper;

/**
 * 投递 Mapper
 */
@Mapper
public interface JobApplicationMapper extends BaseMapper<JobApplication> {
}
