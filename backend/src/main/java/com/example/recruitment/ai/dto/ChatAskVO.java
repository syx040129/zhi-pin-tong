package com.example.recruitment.ai.dto;

import lombok.Data;

import java.util.List;

/**
 * AI 智能助手提问响应
 */
@Data
public class ChatAskVO {

    /** 本次回答（等价于 messages 中最后一条） */
    private String content;
    /** 会话 id（未登录时为 null） */
    private Long sessionId;
    /** 推荐追问 */
    private List<String> suggestions;
    /** LLM / RULE */
    private String source;
    /** 该会话消息条数 */
    private Integer messageCount;
    /** 对话记录是否已落库（未登录为 false） */
    private Boolean persisted;
}
