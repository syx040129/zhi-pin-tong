package com.example.recruitment.admin.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理端重置密码
 */
@Data
public class PasswordResetRequest {

    @Size(min = 6, max = 30, message = "密码长度需在 6-30 位之间")
    private String password;
}
