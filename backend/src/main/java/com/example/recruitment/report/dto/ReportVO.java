package com.example.recruitment.report.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 举报记录 VO
 */
@Data
public class ReportVO {

    private Long id;
    private Long userId;
    private String reporterName;
    private String targetType;
    private Long targetId;
    private String targetName;
    private String reason;
    private String detail;
    private String status;
    private String handleRemark;
    private LocalDateTime createTime;
}
