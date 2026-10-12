package com.example.recruitment.ai.dto;

import lombok.Data;

import java.util.List;

/**
 * AI 智能助手欢迎语与快捷问题
 */
@Data
public class ChatGreetingVO {

    /** 欢迎语（按登录角色与简历完善度生成） */
    private String greeting;
    /** 快捷提问 */
    private List<String> quickQuestions;
    /** REAL / MOCK / DISABLED */
    private String mode;
    private Boolean loggedIn;
}
