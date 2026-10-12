package com.example.recruitment.resume.dto;

import lombok.Data;

import java.util.List;

/**
 * 简历解析结果 VO
 */
@Data
public class ResumeParseVO {

    private Long resumeId;
    private String fileName;
    private String fileUrl;
    /** 提取到的原文（前 3000 字） */
    private String text;
    /** 解析预览不落库，固定 false；由用户确认后通过简历更新接口保存。 */
    private boolean filled;
    private String name;
    private String phone;
    private String email;
    private String education;
    private String school;
    private String major;
    private String summary;
    private List<String> skills;
    private List<ExperienceForm> experiences;
}
