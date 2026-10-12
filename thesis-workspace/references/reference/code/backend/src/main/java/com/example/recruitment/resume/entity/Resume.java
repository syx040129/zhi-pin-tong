package com.example.recruitment.resume.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 简历表
 */
@Data
@TableName("resume")
public class Resume {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String name;
    private String phone;
    private String email;
    private String education;
    private String school;
    private String major;
    /** 技能，逗号分隔 */
    private String skills;
    private String summary;
    private String resumeFileUrl;
    private Integer completionScore;
    private Integer deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
