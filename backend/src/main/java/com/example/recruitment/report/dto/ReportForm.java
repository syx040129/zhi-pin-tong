package com.example.recruitment.report.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 举报请求
 */
@Data
public class ReportForm {

    @NotBlank(message = "举报对象类型不能为空")
    @Pattern(regexp = "^(JOB|COMPANY)$", message = "举报对象类型只能是 JOB 或 COMPANY")
    private String targetType;

    @NotNull(message = "举报对象不能为空")
    private Long targetId;

    @NotBlank(message = "请选择举报原因")
    @Size(max = 100, message = "举报原因最长 100 字")
    private String reason;

    @Size(max = 1000, message = "补充说明最长 1000 字")
    private String detail;
}
