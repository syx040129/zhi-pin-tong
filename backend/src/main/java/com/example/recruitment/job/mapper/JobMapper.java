package com.example.recruitment.job.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.recruitment.job.entity.Job;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 岗位 Mapper
 */
@Mapper
public interface JobMapper extends BaseMapper<Job> {
    @Update("UPDATE job SET view_count = COALESCE(view_count, 0) + 1 WHERE id = #{id} AND deleted = 0")
    int incrementViewCount(@Param("id") Long id);
}
