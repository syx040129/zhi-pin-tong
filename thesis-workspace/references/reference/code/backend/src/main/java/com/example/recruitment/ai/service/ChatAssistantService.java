package com.example.recruitment.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.recruitment.ai.chat.ChatContext;
import com.example.recruitment.ai.chat.ChatContextProvider;
import com.example.recruitment.ai.chat.ChatMockEngine;
import com.example.recruitment.ai.chat.ChatPrompt;
import com.example.recruitment.ai.config.AiProperties;
import com.example.recruitment.ai.dto.ChatAskRequest;
import com.example.recruitment.ai.dto.ChatAskVO;
import com.example.recruitment.ai.dto.ChatGreetingVO;
import com.example.recruitment.ai.dto.ChatHistoryVO;
import com.example.recruitment.ai.dto.ChatMessageVO;
import com.example.recruitment.ai.dto.ChatSessionVO;
import com.example.recruitment.ai.entity.AiChatMessage;
import com.example.recruitment.ai.entity.AiChatSession;
import com.example.recruitment.ai.llm.LlmClient;
import com.example.recruitment.ai.mapper.AiChatMessageMapper;
import com.example.recruitment.ai.mapper.AiChatSessionMapper;
import com.example.recruitment.common.constant.RoleConstants;
import com.example.recruitment.common.utils.JsonUtils;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * AI 智能助手服务（右下角悬浮对话框）。
 * <p>
 * 三种能力：智能问答（{@link #ask}）、推荐追问（随回答返回）、对话记录（{@link #history}/{@link #sessions}）。
 * 真实模式调用大模型并输出 JSON，任何异常都降级到 {@link ChatMockEngine} 规则回答，保证接口恒可用；
 * 未登录访客可直接问答，只是不落库。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatAssistantService {

    /** 送入模型的历史消息条数（滑动窗口，控制 token 与延迟） */
    private static final int HISTORY_WINDOW = 10;
    /** 单会话最多返回的历史消息条数 */
    private static final int HISTORY_LIMIT = 50;
    /** 会话列表返回条数 */
    private static final int SESSION_LIMIT = 20;
    /** 推荐追问条数 */
    private static final int SUGGESTION_LIMIT = 3;

    private final AiChatSessionMapper sessionMapper;
    private final AiChatMessageMapper messageMapper;
    private final ChatContextProvider contextProvider;
    private final ChatMockEngine mockEngine;
    private final ChatPrompt chatPrompt;
    private final LlmClient llmClient;
    private final AiProperties aiProperties;

    /* ==================== 欢迎语与快捷提问 ==================== */

    public ChatGreetingVO greeting(Long userId, String role) {
        ChatContext context = contextProvider.collect(userId, role);
        boolean loggedIn = userId != null;
        ChatGreetingVO vo = new ChatGreetingVO();
        vo.setGreeting(buildGreeting(context, loggedIn));
        vo.setQuickQuestions(mockEngine.defaultQuickQuestions(role));
        vo.setMode(aiProperties.displayMode());
        vo.setLoggedIn(loggedIn);
        return vo;
    }

    private String buildGreeting(ChatContext context, boolean loggedIn) {
        if (!loggedIn) {
            return "你好，我是智聘AI 智能助手 👋\n可以问我招聘平台怎么用、简历怎么写、面试怎么准备，例如“AI 岗位匹配是怎么算分的？”。登录后我还能结合你的简历与推荐记录给出个性化建议。";
        }
        if (RoleConstants.COMPANY.equals(context.getRole())) {
            return "你好" + (StringUtils.hasText(context.getCompanyName())
                    ? "，" + context.getCompanyName() : "") + " 👋\n我是智聘AI 智能助手，可以帮你梳理岗位发布技巧、候选人筛选口径与招聘漏斗数据。";
        }
        if (RoleConstants.ADMIN.equals(context.getRole())) {
            return "你好，管理员 👋\n我可以说明平台运营数据口径、举报处理流程与 AI 模式配置。当前待处理举报 "
                    + context.getPlatformPendingReportCount() + " 条。";
        }
        if (context.hasResume()) {
            String name = StringUtils.hasText(context.getResumeName()) ? "，" + context.getResumeName() : "";
            return "你好" + name + " 👋\n我看过你的简历了：技能栈 " + context.skillText() + "，完整度 "
                    + (context.getCompletionScore() == null ? "待评估" : context.getCompletionScore() + " 分") + "。\n"
                    + "可以问我简历优化、岗位匹配、面试准备或投递跟进，也能直接问某个岗位「我适合吗」。";
        }
        return "你好 👋 我是智聘AI 智能助手。\n你还没有简历，我建议先到「AI简历」创建一份并上传附件，这样岗位匹配与推荐会更准确。\n在此之前，也可以先问我简历怎么写、面试怎么准备。";
    }

    /* ==================== 对话记录 ==================== */

    /** 最近一次会话的历史（未登录或没有记录时返回空列表） */
    public ChatHistoryVO history(Long userId) {
        ChatHistoryVO vo = new ChatHistoryVO();
        if (userId == null) {
            vo.setMessages(Collections.emptyList());
            return vo;
        }
        AiChatSession session = latestSession(userId);
        if (session == null) {
            vo.setMessages(Collections.emptyList());
            return vo;
        }
        vo.setSessionId(session.getId());
        vo.setTitle(session.getTitle());
        vo.setMessageCount(session.getMessageCount());
        vo.setMessages(loadMessages(session.getId(), HISTORY_LIMIT).stream().map(this::toVO).toList());
        return vo;
    }

    /** 我的历史会话列表 */
    public List<ChatSessionVO> sessions(Long userId) {
        List<AiChatSession> sessions = sessionMapper.selectList(new LambdaQueryWrapper<AiChatSession>()
                .eq(AiChatSession::getUserId, userId)
                .orderByDesc(AiChatSession::getId)
                .last("limit " + SESSION_LIMIT));
        List<ChatSessionVO> list = new ArrayList<>();
        for (AiChatSession session : sessions) {
            ChatSessionVO vo = new ChatSessionVO();
            vo.setId(session.getId());
            vo.setTitle(session.getTitle());
            vo.setMessageCount(session.getMessageCount());
            vo.setCreateTime(session.getCreateTime() == null ? null : session.getCreateTime().toString());
            AiChatMessage last = messageMapper.selectOne(new LambdaQueryWrapper<AiChatMessage>()
                    .eq(AiChatMessage::getSessionId, session.getId())
                    .orderByDesc(AiChatMessage::getId)
                    .last("limit 1"));
            if (last != null) {
                vo.setLastMessage(abbreviate(last.getContent(), 60));
            }
            list.add(vo);
        }
        return list;
    }

    /** 清空对话记录（删除该用户全部会话与消息） */
    @Transactional
    public void clear(Long userId) {
        List<AiChatSession> sessions = sessionMapper.selectList(new LambdaQueryWrapper<AiChatSession>()
                .eq(AiChatSession::getUserId, userId));
        for (AiChatSession session : sessions) {
            messageMapper.delete(new LambdaQueryWrapper<AiChatMessage>()
                    .eq(AiChatMessage::getSessionId, session.getId()));
        }
        sessionMapper.delete(new LambdaQueryWrapper<AiChatSession>().eq(AiChatSession::getUserId, userId));
    }

    /* ==================== 智能问答 ==================== */

    @Transactional
    public ChatAskVO ask(ChatAskRequest request, Long userId, String role) {
        String question = request.getContent() == null ? "" : request.getContent().trim();
        if (!StringUtils.hasText(question)) {
            question = "你好";
        }
        boolean persist = userId != null;

        AiChatSession session = null;
        if (persist) {
            session = resolveSession(userId, request.getSessionId());
        }
        Long sessionId = session == null ? null : session.getId();
        List<AiChatMessage> history = sessionId == null
                ? Collections.emptyList() : loadMessages(sessionId, HISTORY_WINDOW);

        ChatContext context = contextProvider.collect(userId, role);
        if ("JOB".equalsIgnoreCase(request.getContextType())) {
            contextProvider.withJob(context, request.getContextId(), userId);
        }

        Reply reply = generate(question, context, history);
        List<String> suggestions = mockEngine.completeSuggestions(reply.suggestions(), context);

        if (persist) {
            if (session == null) {
                session = resolveSession(userId, null);
            }
            if (!StringUtils.hasText(session.getTitle()) || "新对话".equals(session.getTitle())) {
                // 首条提问即作为会话标题，便于“对话记录”识别
                session.setTitle(abbreviate(question, 30));
            }
            saveMessage(session.getId(), userId, "USER", question, null, null);
            saveMessage(session.getId(), userId, "ASSISTANT", reply.content(), suggestions, reply.source());
            session.setMessageCount((session.getMessageCount() == null ? 0 : session.getMessageCount()) + 2);
            session.setUpdateTime(LocalDateTime.now());
            sessionMapper.updateById(session);
        }

        ChatAskVO vo = new ChatAskVO();
        vo.setContent(reply.content());
        vo.setSuggestions(suggestions);
        vo.setSource(reply.source());
        vo.setSessionId(session == null ? null : session.getId());
        vo.setMessageCount(session == null ? 2 : session.getMessageCount());
        vo.setPersisted(persist);
        return vo;
    }

    /** 真实模式调用大模型，失败或非真实模式走规则引擎 */
    private Reply generate(String question, ChatContext context, List<AiChatMessage> history) {
        if (aiProperties.realMode()) {
            try {
                String json = llmClient.chatJson(chatPrompt.systemPrompt(context),
                        chatPrompt.userPrompt(context, history, question));
                JsonNode node = JsonUtils.readTree(json);
                if (node != null) {
                    String reply = node.path("reply").asText("");
                    if (StringUtils.hasText(reply)) {
                        List<String> suggestions = new ArrayList<>();
                        JsonNode suggestionNode = node.get("suggestions");
                        if (suggestionNode != null && suggestionNode.isArray()) {
                            for (JsonNode item : suggestionNode) {
                                String text = item.asText("");
                                if (StringUtils.hasText(text)) {
                                    suggestions.add(abbreviate(text, 30));
                                }
                                if (suggestions.size() >= SUGGESTION_LIMIT) {
                                    break;
                                }
                            }
                        }
                        return new Reply(reply.trim(), suggestions, "LLM");
                    }
                }
                log.warn("LLM 助手回答为空，降级规则引擎");
            } catch (Exception e) {
                log.warn("LLM 助手调用失败，降级规则引擎: {}", e.getMessage());
            }
        }
        ChatMockEngine.MockAnswer answer = mockEngine.answer(question, context);
        String content = StringUtils.hasText(answer.content()) ? answer.content() : "抱歉，我暂时没想到合适的回答，可以换个问法试试。";
        return new Reply(content, answer.suggestions(), "RULE");
    }

    private record Reply(String content, List<String> suggestions, String source) {
    }

    /* ==================== 内部工具 ==================== */

    private AiChatSession resolveSession(Long userId, Long sessionId) {
        if (sessionId != null) {
            AiChatSession session = sessionMapper.selectById(sessionId);
            if (session != null && Objects.equals(session.getUserId(), userId)) {
                return session;
            }
        }
        AiChatSession session = new AiChatSession();
        session.setUserId(userId);
        session.setTitle("新对话");
        session.setMessageCount(0);
        sessionMapper.insert(session);
        return session;
    }

    private AiChatSession latestSession(Long userId) {
        return sessionMapper.selectOne(new LambdaQueryWrapper<AiChatSession>()
                .eq(AiChatSession::getUserId, userId)
                .orderByDesc(AiChatSession::getId)
                .last("limit 1"));
    }

    private void saveMessage(Long sessionId, Long userId, String role, String content,
                             List<String> suggestions, String source) {
        AiChatMessage message = new AiChatMessage();
        message.setSessionId(sessionId);
        message.setUserId(userId);
        message.setRole(role);
        message.setContent(content);
        message.setSuggestions(suggestions == null || suggestions.isEmpty()
                ? null : JsonUtils.fromList(suggestions));
        message.setSource(source);
        messageMapper.insert(message);
    }

    /** 取最近 limit 条消息并按时间正序返回（用于对话展示与模型上下文） */
    private List<AiChatMessage> loadMessages(Long sessionId, int limit) {
        List<AiChatMessage> messages = messageMapper.selectList(new LambdaQueryWrapper<AiChatMessage>()
                .eq(AiChatMessage::getSessionId, sessionId)
                .orderByDesc(AiChatMessage::getId)
                .last("limit " + limit));
        List<AiChatMessage> ordered = new ArrayList<>(messages);
        Collections.reverse(ordered);
        return ordered;
    }

    private ChatMessageVO toVO(AiChatMessage message) {
        ChatMessageVO vo = new ChatMessageVO();
        vo.setId(message.getId());
        vo.setSessionId(message.getSessionId());
        vo.setRole(message.getRole());
        vo.setContent(message.getContent());
        vo.setSource(message.getSource());
        vo.setCreateTime(message.getCreateTime());
        vo.setSuggestions(JsonUtils.toList(message.getSuggestions()));
        return vo;
    }

    private String abbreviate(String text, int max) {
        if (text == null) {
            return "";
        }
        String flat = text.replaceAll("\\s+", " ").trim();
        return flat.length() <= max ? flat : flat.substring(0, max) + "…";
    }
}
