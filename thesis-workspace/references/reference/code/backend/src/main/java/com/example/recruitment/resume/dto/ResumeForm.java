package com.example.recruitment.resume.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 简历保存请求
 */
@Data
public class ResumeForm {

    @Size(max = 50, message = "姓名最长 50 个字符")
    private String name;

    @Size(max = 20, message = "手机号格式不正确")
    private String phone;

    @Size(max = 100, message = "邮箱最长 100 个字符")
    private String email;

    private String education;
    private String school;
    private String major;
    private List<String> skills;
    private String summary;

    @Valid
    private List<ExperienceForm> experiences;
}
