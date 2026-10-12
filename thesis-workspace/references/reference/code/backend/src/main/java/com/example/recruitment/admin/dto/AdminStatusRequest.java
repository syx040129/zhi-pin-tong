package com.example.recruitment.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 管理端状态更新
 */
@Data
public class AdminStatusRequest {

    @NotBlank(message = "状态不能为空")
    private String status;
}
