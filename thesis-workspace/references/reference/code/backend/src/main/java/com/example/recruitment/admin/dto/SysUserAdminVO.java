package com.example.recruitment.admin.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理端用户 VO
 */
@Data
public class SysUserAdminVO {

    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private String phone;
    private String email;
    private String gender;
    private String role;
    private String status;
    private LocalDateTime createTime;
}
