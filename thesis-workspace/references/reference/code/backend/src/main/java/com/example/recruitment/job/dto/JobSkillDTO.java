package com.example.recruitment.job.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 岗位技能 DTO
 */
@Data
public class JobSkillDTO {

    private Long id;

    @NotBlank(message = "技能名称不能为空")
    private String skillName;

    private Integer requiredLevel;

    private Integer weight;
}
