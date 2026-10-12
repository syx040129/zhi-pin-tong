package com.example.recruitment.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.recruitment.ai.entity.AiChatSession;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 智能助手会话 Mapper
 */
@Mapper
public interface AiChatSessionMapper extends BaseMapper<AiChatSession> {
}
