package com.example.recruitment.auth.dto;

import com.example.recruitment.user.dto.UserProfileVO;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 登录返回
 */
@Data
@AllArgsConstructor
public class LoginResponse {

    private String token;
    private UserProfileVO user;
}
