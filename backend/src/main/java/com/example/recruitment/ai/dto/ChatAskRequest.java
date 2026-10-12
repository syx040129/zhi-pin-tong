package com.example.recruitment.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * AI 智能助手提问请求
 */
@Data
public class ChatAskRequest {

    @NotBlank(message = "提问内容不能为空")
    @Size(max = 500, message = "单次提问不能超过 500 字")
    private String content;

    /** 会话 id：为空表示开启新会话（需登录，未登录时不落库） */
    private Long sessionId;

    /** 上下文类型：JOB（岗位详情页提问）/ RESUME（简历页提问），可为空 */
    private String contextType;

    /** 上下文对象 id，与 contextType 搭配使用 */
    private Long contextId;
}
