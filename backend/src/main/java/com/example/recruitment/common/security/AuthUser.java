package com.example.recruitment.common.security;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 登录用户主体信息（来自 JWT）
 */
@Data
@AllArgsConstructor
public class AuthUser {

    private Long id;
    private String username;
    private String nickname;
    private String role;
}
