package com.example.recruitment.resume.dto;

import com.example.recruitment.resume.entity.ResumeExperience;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 简历 VO
 */
@Data
public class ResumeVO {

    private Long id;
    private Long userId;
    private String name;
    private String phone;
    private String email;
    private String education;
    private String school;
    private String major;
    private List<String> skills;
    private String summary;
    private String resumeFileUrl;
    private Integer completionScore;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private List<ResumeExperience> experiences;
}
