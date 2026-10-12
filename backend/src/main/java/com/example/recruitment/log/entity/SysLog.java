package com.example.recruitment.log.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统日志表（登录 / 操作 / AI 调用 / 异常）
 */
@Data
@TableName("sys_log")
public class SysLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String username;
    private String type;
    private String module;
    private String action;
    private String detail;
    private String ip;
    private Long costMs;
    private LocalDateTime createTime;
}
