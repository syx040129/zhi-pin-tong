package com.example.recruitment.application.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 投递记录 VO
 */
@Data
public class ApplicationVO {

    private Long id;
    private Long jobId;
    private String jobName;
    private String jobType;
    private String city;
    private Integer salaryMin;
    private Integer salaryMax;
    private Long companyId;
    private String companyName;
    private String companyLogo;
    private Long userId;
    /** 候选人（企业视角） */
    private String candidateName;
    private String candidateAvatar;
    private String candidatePhone;
    private String candidateEmail;
    private Long resumeId;
    private String resumeName;
    private String status;
    private String coverLetter;
    private Integer matchScore;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
