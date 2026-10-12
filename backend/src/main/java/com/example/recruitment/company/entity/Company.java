package com.example.recruitment.company.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 企业表
 */
@Data
@TableName("company")
public class Company {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String companyName;
    private String logo;
    private String industry;
    private String scale;
    private String address;
    private String description;
    private String website;
    /** ACTIVE / DISABLED */
    private String status;
    private Integer deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
