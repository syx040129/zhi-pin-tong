package com.example.recruitment.user.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户信息 VO
 */
@Data
public class UserProfileVO {

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
    /** COMPANY 角色对应的企业信息 */
    private Long companyId;
    private String companyName;
}
