package com.example.recruitment.job.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 岗位发布/编辑请求
 */
@Data
public class JobForm {

    @NotBlank(message = "岗位名称不能为空")
    @Size(max = 100, message = "岗位名称最长 100 个字符")
    private String jobName;

    @NotBlank(message = "岗位类型不能为空")
    private String jobType;

    @NotNull(message = "最低薪资不能为空")
    @Min(value = 0, message = "最低薪资不能小于 0")
    private Integer salaryMin;

    @NotNull(message = "最高薪资不能为空")
    @Max(value = 500, message = "最高薪资不能超过 500K")
    private Integer salaryMax;

    @NotBlank(message = "工作城市不能为空")
    private String city;

    @NotBlank(message = "学历要求不能为空")
    private String education;

    @NotBlank(message = "经验要求不能为空")
    private String experience;

    @NotBlank(message = "岗位描述不能为空")
    private String description;

    @NotBlank(message = "岗位要求不能为空")
    private String requirements;

    @Valid
    private List<JobSkillDTO> skills;
}
