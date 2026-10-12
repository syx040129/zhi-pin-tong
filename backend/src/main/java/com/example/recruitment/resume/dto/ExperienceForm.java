package com.example.recruitment.resume.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 简历经历编辑项
 */
@Data
public class ExperienceForm {

    private Long id;

    @NotBlank(message = "经历类型不能为空")
    private String type;

    @NotBlank(message = "经历标题不能为空")
    @Size(max = 100, message = "标题最长 100 个字符")
    private String title;

    @Size(max = 100, message = "公司/学校最长 100 个字符")
    private String company;

    private String content;

    @Size(max = 20, message = "开始时间格式不正确")
    private String startDate;

    @Size(max = 20, message = "结束时间格式不正确")
    private String endDate;

    private Integer sort;
}
