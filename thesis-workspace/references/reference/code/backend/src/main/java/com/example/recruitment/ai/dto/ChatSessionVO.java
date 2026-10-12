package com.example.recruitment.ai.dto;

import lombok.Data;

import java.util.List;

/**
 * AI 智能助手会话概览
 */
@Data
public class ChatSessionVO {

    private Long id;
    private String title;
    private Integer messageCount;
    private String lastMessage;
    private String createTime;
}
