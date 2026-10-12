package com.example.recruitment.report.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 举报表
 */
@Data
@TableName("report")
public class Report {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    /** JOB / COMPANY */
    private String targetType;
    private Long targetId;
    private String reason;
    private String detail;
    /** PENDING / HANDLED / IGNORED */
    private String status;
    private String handleRemark;
    private Integer deleted;
    private LocalDateTime createTime;
}
