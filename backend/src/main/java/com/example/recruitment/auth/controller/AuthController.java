package com.example.recruitment.auth.controller;

import com.example.recruitment.auth.dto.LoginRequest;
import com.example.recruitment.auth.dto.LoginResponse;
import com.example.recruitment.auth.dto.RegisterRequest;
import com.example.recruitment.auth.service.AuthService;
import com.example.recruitment.common.Result;
import com.example.recruitment.user.dto.UserProfileVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口：注册 / 登录 / 当前用户
 */
@Tag(name = "认证模块")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "注册（USER 求职者 / COMPANY 企业）")
    @PostMapping("/register")
    public Result<UserProfileVO> register(@Valid @RequestBody RegisterRequest request,
                                          HttpServletRequest httpRequest) {
        return Result.ok(authService.register(request, httpRequest));
    }

    @Operation(summary = "登录")
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                       HttpServletRequest httpRequest) {
        return Result.ok(authService.login(request, httpRequest));
    }

    @Operation(summary = "获取当前登录用户")
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public Result<UserProfileVO> me() {
        return Result.ok(authService.me());
    }
}
