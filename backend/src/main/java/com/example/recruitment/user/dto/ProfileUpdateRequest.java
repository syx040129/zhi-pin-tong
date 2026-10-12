package com.example.recruitment.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改个人信息请求
 */
@Data
public class ProfileUpdateRequest {

    @Size(max = 30, message = "昵称最长 30 个字符")
    private String nickname;

    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Email(message = "邮箱格式不正确")
    private String email;

    @Pattern(regexp = "^(MALE|FEMALE|UNKNOWN)?$", message = "性别参数不正确")
    private String gender;
}
