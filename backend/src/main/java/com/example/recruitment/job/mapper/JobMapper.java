package com.example.recruitment.job.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.recruitment.job.entity.Job;
import org.apache.ibatis.annotations.Mapper;

/**
 * 岗位 Mapper
 */
@Mapper
public interface JobMapper extends BaseMapper<Job> {
}
