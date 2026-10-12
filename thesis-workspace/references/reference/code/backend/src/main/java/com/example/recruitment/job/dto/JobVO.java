package com.example.recruitment.job.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 岗位信息 VO
 */
@Data
public class JobVO {

    private Long id;
    private Long companyId;
    private String companyName;
    private String companyLogo;
    private String companyIndustry;
    private String jobName;
    private String jobType;
    private Integer salaryMin;
    private Integer salaryMax;
    private String city;
    private String education;
    private String experience;
    private String description;
    private String requirements;
    private String status;
    private Integer viewCount;
    private LocalDateTime createTime;
    private List<JobSkillDTO> skills;
    /** 当前用户是否已收藏 */
    private Boolean favorite;
    /** 当前用户是否已投递及状态 */
    private String applicationStatus;
    /** 缓存的历史 AI 匹配度 */
    private Integer matchScore;
    /** 企业端：投递数量统计 */
    private Long totalApplications;
    private Long pendingApplications;
}
