package com.example.recruitment.admin.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理端简历 VO
 */
@Data
public class ResumeAdminVO {

    private Long id;
    private Long userId;
    private String userNickname;
    private String userName;
    private String phone;
    private String email;
    private String education;
    private String school;
    private String major;
    private String skills;
    private Integer completionScore;
    private String resumeFileUrl;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
