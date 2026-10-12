package com.example.recruitment.application.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 投递请求
 */
@Data
public class ApplicationForm {

    @NotNull(message = "请选择要投递的岗位")
    private Long jobId;

    private Long resumeId;

    @Size(max = 1000, message = "求职信最长 1000 字")
    private String coverLetter;
}
