package com.example.recruitment.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 智能助手消息表（提问与回答统一存放）
 */
@Data
@TableName("ai_chat_message")
public class AiChatMessage {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sessionId;
    private Long userId;
    /** 角色：USER / ASSISTANT */
    private String role;
    private String content;
    /** 回答的推荐追问（JSON 数组，仅 ASSISTANT 有值） */
    private String suggestions;
    /** 回答来源：LLM / RULE */
    private String source;
    private LocalDateTime createTime;
}
