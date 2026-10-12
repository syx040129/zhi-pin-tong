package com.example.recruitment.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 智能助手会话表
 */
@Data
@TableName("ai_chat_session")
public class AiChatSession {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    /** 会话标题（取首条提问前 30 字） */
    private String title;
    /** 消息条数（含提问与回答） */
    private Integer messageCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
