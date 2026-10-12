package com.example.recruitment.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.recruitment.ai.entity.AiChatMessage;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 智能助手消息 Mapper
 */
@Mapper
public interface AiChatMessageMapper extends BaseMapper<AiChatMessage> {
}
