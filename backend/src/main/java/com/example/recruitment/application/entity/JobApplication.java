package com.example.recruitment.application.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 职位投递表
 */
@Data
@TableName("job_application")
public class JobApplication {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long jobId;
    private Long userId;
    private Long resumeId;
    /** PENDING / VIEWED / INTERVIEW / REJECTED / HIRED */
    private String status;
    private String coverLetter;
    private Integer matchScore;
    private Integer deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
