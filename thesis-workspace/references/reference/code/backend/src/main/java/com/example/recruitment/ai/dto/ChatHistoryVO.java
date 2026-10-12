package com.example.recruitment.ai.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 智能助手历史消息响应（用于打开面板时恢复“对话记录”）
 */
@Data
public class ChatHistoryVO {

    private Long sessionId;
    private String title;
    private Integer messageCount;
    private List<ChatMessageVO> messages = new ArrayList<>();
}
