package com.example.recruitment.resume.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 简历经历表（教育/工作/项目/实习）
 */
@Data
@TableName("resume_experience")
public class ResumeExperience {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long resumeId;
    /** EDUCATION / WORK / PROJECT / INTERNSHIP */
    private String type;
    private String title;
    private String company;
    private String content;
    private String startDate;
    private String endDate;
    private Integer sort;
}
