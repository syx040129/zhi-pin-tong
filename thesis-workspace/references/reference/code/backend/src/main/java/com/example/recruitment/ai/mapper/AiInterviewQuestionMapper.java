package com.example.recruitment.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.recruitment.ai.entity.AiInterviewQuestion;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 面试题目 Mapper
 */
@Mapper
public interface AiInterviewQuestionMapper extends BaseMapper<AiInterviewQuestion> {
}
