package com.example.recruitment.common.utils;

import com.example.recruitment.common.exception.BusinessException;
import com.example.recruitment.common.security.AuthUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 安全上下文工具
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /**
     * 获取当前登录用户
     */
    public static AuthUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthUser user) {
            return user;
        }
        throw BusinessException.unauthorized("请先登录");
    }

    /**
     * 当前登录用户，未登录返回 null（供匿名可用的接口使用，例如 AI 智能助手）
     */
    public static AuthUser currentUserOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthUser user) {
            return user;
        }
        return null;
    }

    public static Long currentUserId() {
        return currentUser().getId();
    }

    public static String currentRole() {
        return currentUser().getRole();
    }
}
