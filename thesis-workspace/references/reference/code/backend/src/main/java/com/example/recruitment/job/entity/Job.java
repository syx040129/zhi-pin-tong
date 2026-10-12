package com.example.recruitment.job.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 岗位表
 */
@Data
@TableName("job")
public class Job {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long companyId;
    private String jobName;
    private String jobType;
    private Integer salaryMin;
    private Integer salaryMax;
    private String city;
    private String education;
    private String experience;
    private String description;
    private String requirements;
    /** ONLINE / OFFLINE */
    private String status;
    private Integer viewCount;
    private Integer deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
