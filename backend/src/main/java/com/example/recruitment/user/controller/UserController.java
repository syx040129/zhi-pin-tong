package com.example.recruitment.user.controller;

import com.example.recruitment.common.Result;
import com.example.recruitment.log.annotation.OpLog;
import com.example.recruitment.user.dto.PasswordChangeRequest;
import com.example.recruitment.user.dto.ProfileUpdateRequest;
import com.example.recruitment.user.dto.UserProfileVO;
import com.example.recruitment.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 用户接口
 */
@Tag(name = "用户模块")
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final com.example.recruitment.file.service.FileService fileService;

    @Operation(summary = "获取我的个人信息")
    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public Result<UserProfileVO> profile() {
        return Result.ok(userService.currentProfile());
    }

    @Operation(summary = "修改个人信息")
    @PutMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    @OpLog(module = "用户中心", action = "修改个人信息")
    public Result<UserProfileVO> updateProfile(@Valid @RequestBody ProfileUpdateRequest request) {
        return Result.ok(userService.updateProfile(request));
    }

    @Operation(summary = "修改密码")
    @PutMapping("/password")
    @PreAuthorize("isAuthenticated()")
    @OpLog(module = "用户中心", action = "修改密码")
    public Result<Void> changePassword(@Valid @RequestBody PasswordChangeRequest request) {
        userService.changePassword(request.getOldPassword(), request.getNewPassword());
        return Result.ok();
    }

    @Operation(summary = "上传头像")
    @PostMapping("/avatar")
    @PreAuthorize("isAuthenticated()")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        String url = fileService.upload(file, "avatar");
        userService.updateAvatar(url);
        return Result.ok(url);
    }
}
