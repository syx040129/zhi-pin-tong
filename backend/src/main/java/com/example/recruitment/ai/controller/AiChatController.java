package com.example.recruitment.ai.controller;

import com.example.recruitment.ai.dto.ChatAskRequest;
import com.example.recruitment.ai.dto.ChatAskVO;
import com.example.recruitment.ai.dto.ChatGreetingVO;
import com.example.recruitment.ai.dto.ChatHistoryVO;
import com.example.recruitment.ai.dto.ChatSessionVO;
import com.example.recruitment.ai.service.ChatAssistantService;
import com.example.recruitment.common.Result;
import com.example.recruitment.common.security.AuthUser;
import com.example.recruitment.common.utils.SecurityUtils;
import com.example.recruitment.log.annotation.OpLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * AI 智能助手接口（页面右下角悬浮对话框）。
 * 未登录访客也可问答（不落库），登录后自动保存对话记录。
 * 对外放行见 {@code SecurityConfig.PUBLIC_PATHS}，因此这里不做角色限制。
 */
@Tag(name = "AI 智能助手")
@RestController
@RequestMapping("/api/ai/chat")
@RequiredArgsConstructor
public class AiChatController {

    private final ChatAssistantService chatAssistantService;

    @Operation(summary = "欢迎语与快捷提问")
    @GetMapping("/greeting")
    public Result<ChatGreetingVO> greeting() {
        AuthUser user = SecurityUtils.currentUserOrNull();
        return Result.ok(chatAssistantService.greeting(
                user == null ? null : user.getId(),
                user == null ? null : user.getRole()));
    }

    @Operation(summary = "智能问答（提问并获取回答与推荐追问）")
    @PostMapping("/ask")
    @OpLog(module = "AI 智能助手", action = "智能问答")
    public Result<ChatAskVO> ask(@Valid @RequestBody ChatAskRequest request) {
        AuthUser user = SecurityUtils.currentUserOrNull();
        return Result.ok(chatAssistantService.ask(request,
                user == null ? null : user.getId(),
                user == null ? null : user.getRole()));
    }

    @Operation(summary = "最近一次对话记录")
    @GetMapping("/history")
    public Result<ChatHistoryVO> history() {
        AuthUser user = SecurityUtils.currentUserOrNull();
        return Result.ok(chatAssistantService.history(user == null ? null : user.getId()));
    }

    @Operation(summary = "我的历史会话列表")
    @GetMapping("/sessions")
    @PreAuthorize("isAuthenticated()")
    public Result<List<ChatSessionVO>> sessions() {
        AuthUser user = SecurityUtils.currentUser();
        return Result.ok(chatAssistantService.sessions(user.getId()));
    }

    @Operation(summary = "清空对话记录")
    @DeleteMapping("/history")
    @PreAuthorize("isAuthenticated()")
    @OpLog(module = "AI 智能助手", action = "清空对话")
    public Result<Void> clear() {
        AuthUser user = SecurityUtils.currentUser();
        chatAssistantService.clear(user.getId());
        return Result.ok();
    }
}
