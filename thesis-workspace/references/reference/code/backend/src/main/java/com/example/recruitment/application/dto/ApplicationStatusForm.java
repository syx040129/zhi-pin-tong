package com.example.recruitment.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 投递状态更新请求（企业处理）
 */
@Data
public class ApplicationStatusForm {

    @NotBlank(message = "状态不能为空")
    @Pattern(regexp = "^(VIEWED|INTERVIEW|REJECTED|HIRED)$", message = "状态参数不正确")
    private String status;
}
