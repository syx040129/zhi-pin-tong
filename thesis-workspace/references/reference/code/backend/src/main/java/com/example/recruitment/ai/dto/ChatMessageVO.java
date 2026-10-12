package com.example.recruitment.ai.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI 智能助手消息视图
 */
@Data
public class ChatMessageVO {

    private Long id;
    private Long sessionId;
    /** USER / ASSISTANT */
    private String role;
    private String content;
    /** 推荐追问（仅助手消息有值） */
    private List<String> suggestions;
    /** LLM / RULE */
    private String source;
    private LocalDateTime createTime;
}
