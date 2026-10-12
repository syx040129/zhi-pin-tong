package com.example.recruitment.ai.chat;

import com.example.recruitment.common.constant.RoleConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AI 智能助手 Mock 规则引擎单元测试：验证无 Key 场景下的关键词命中、数据注入与推荐追问，
 * 纯内存执行，不依赖 Spring 容器与数据库。
 */
class ChatMockEngineTest {

    private final ChatMockEngine engine = new ChatMockEngine();

    private ChatContext userContext() {
        ChatContext context = new ChatContext();
        context.setRole(RoleConstants.USER);
        context.setResumeId(1L);
        context.setResumeName("王小明");
        context.setEducation("本科");
        context.setCompletionScore(100);
        context.setSkills(List.of("Java", "Spring Boot", "MySQL", "Redis"));
        context.setApplicationCount(6);
        context.setTopJobs(new java.util.LinkedHashMap<>(java.util.Map.of("Java开发工程师", 91)));
        return context;
    }

    @Test
    @DisplayName("匹配度提问命中岗位匹配主题并给出四维加权口径")
    void matchTopic() {
        ChatMockEngine.MockAnswer answer = engine.answer("AI 岗位匹配是怎么算分的？", userContext());
        assertTrue(answer.content().contains("语义匹配 50%"), answer.content());
        assertTrue(answer.content().contains("技能匹配 25%"), answer.content());
        assertFalse(answer.suggestions().isEmpty());
    }

    @Test
    @DisplayName("推荐提问会带上用户的真实技能与推荐结果")
    void recommendTopicUsesUserData() {
        ChatMockEngine.MockAnswer answer = engine.answer("帮我推荐几个岗位", userContext());
        assertTrue(answer.content().contains("Java"), answer.content());
        assertTrue(answer.content().contains("Java开发工程师"), answer.content());
    }

    @Test
    @DisplayName("岗位上下文提问会注入命中/缺失技能对比")
    void focusJobTopic() {
        ChatContext context = userContext();
        context.setFocusJobId(1L);
        context.setFocusJobName("Java开发工程师");
        context.setFocusJobExperience("1-3年");
        context.setFocusJobEducation("本科");
        context.setFocusJobSkills(List.of("Java", "Spring Boot", "Kafka"));
        context.setFocusMatchedSkills(List.of("Java", "Spring Boot"));
        context.setFocusMissingSkills(List.of("Kafka"));

        ChatMockEngine.MockAnswer answer = engine.answer("我缺哪些技能？", context);
        assertTrue(answer.content().contains("Kafka"), answer.content());
        assertTrue(answer.content().contains("Java开发工程师"), answer.content());
    }

    @Test
    @DisplayName("企业角色提问命中企业专属主题")
    void companyTopic() {
        ChatContext context = new ChatContext();
        context.setRole(RoleConstants.COMPANY);
        context.setCompanyName("上海星云网络科技有限公司");
        context.setCompanyJobCount(4);
        context.setCompanyOnlineJobCount(4);
        context.setCompanyPendingApplicationCount(2);

        ChatMockEngine.MockAnswer answer = engine.answer("怎么快速筛选合适的候选人？", context);
        assertTrue(answer.content().contains("上海星云网络科技有限公司"), answer.content());
        assertTrue(answer.content().contains("待处理投递 2 条"), answer.content());
    }

    @Test
    @DisplayName("管理员提问命中平台数据主题并回填统计口径")
    void adminTopic() {
        ChatContext context = new ChatContext();
        context.setRole(RoleConstants.ADMIN);
        context.setPlatformUserCount(8);
        context.setPlatformCompanyCount(6);
        context.setPlatformOnlineJobCount(24);
        context.setPlatformPendingReportCount(2);

        ChatMockEngine.MockAnswer answer = engine.answer("平台当前整体数据怎么样？", context);
        assertTrue(answer.content().contains("注册用户 8"), answer.content());
        assertTrue(answer.content().contains("待处理举报 2"), answer.content());
    }

    @Test
    @DisplayName("无法识别的问题走兜底回答且仍给出 3 条推荐追问")
    void fallbackTopic() {
        ChatMockEngine.MockAnswer answer = engine.answer("今天天气如何", userContext());
        assertTrue(answer.content().contains("这个问题我还没完全理解"), answer.content());
        assertEquals(3, answer.suggestions().size());
    }

    @Test
    @DisplayName("completeSuggestions 去重并补齐到 3 条")
    void completeSuggestions() {
        List<String> result = engine.completeSuggestions(List.of("我缺哪些技能？", "我缺哪些技能？"), userContext());
        assertEquals(3, result.size());
        assertEquals(1, result.stream().filter("我缺哪些技能？"::equals).count());
    }

    @Test
    @DisplayName("未登录访客只给通用快捷提问")
    void guestQuickQuestions() {
        assertEquals(3, engine.defaultQuickQuestions(null).size());
        ChatMockEngine.MockAnswer answer = engine.answer("你好", new ChatContext());
        assertFalse(answer.content().isBlank());
    }
}
