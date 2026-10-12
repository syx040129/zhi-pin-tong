package com.example.recruitment.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统用户表
 */
@Data
@TableName("sys_user")
public class SysUser {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String password;
    private String nickname;
    private String avatar;
    private String phone;
    private String email;
    /** MALE / FEMALE / UNKNOWN */
    private String gender;
    /** USER / COMPANY / ADMIN */
    private String role;
    /** ENABLED / DISABLED */
    private String status;
    private Integer deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
