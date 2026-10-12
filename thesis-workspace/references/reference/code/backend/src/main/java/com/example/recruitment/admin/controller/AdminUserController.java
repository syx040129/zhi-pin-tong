package com.example.recruitment.admin.controller;

import com.example.recruitment.admin.dto.AdminStatusRequest;
import com.example.recruitment.admin.dto.PasswordResetRequest;
import com.example.recruitment.admin.dto.SysUserAdminVO;
import com.example.recruitment.admin.service.AdminUserService;
import com.example.recruitment.common.PageResult;
import com.example.recruitment.common.Result;
import com.example.recruitment.log.annotation.OpLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端：用户管理
 */
@Tag(name = "管理端-用户")
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @Operation(summary = "用户分页查询")
    @GetMapping
    public Result<PageResult<SysUserAdminVO>> page(@RequestParam(required = false) String keyword,
                                                   @RequestParam(required = false) String role,
                                                   @RequestParam(required = false) String status,
                                                   @RequestParam(defaultValue = "1") long page,
                                                   @RequestParam(defaultValue = "10") long size) {
        return Result.ok(adminUserService.page(keyword, role, status, page, size));
    }

    @Operation(summary = "启用/禁用用户")
    @PutMapping("/{id}/status")
    @OpLog(module = "用户管理", action = "启用/禁用用户")
    public Result<Void> setStatus(@PathVariable Long id,
                                  @Valid @RequestBody AdminStatusRequest request) {
        adminUserService.setStatus(id, request.getStatus());
        return Result.ok();
    }

    @Operation(summary = "重置用户密码（默认 123456）")
    @PutMapping("/{id}/password")
    @OpLog(module = "用户管理", action = "重置密码")
    public Result<Void> resetPassword(@PathVariable Long id,
                                      @Valid @RequestBody(required = false) PasswordResetRequest request) {
        adminUserService.resetPassword(id, request == null ? null : request.getPassword());
        return Result.ok();
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/{id}")
    @OpLog(module = "用户管理", action = "删除用户")
    public Result<Void> delete(@PathVariable Long id) {
        adminUserService.deleteUser(id);
        return Result.ok();
    }
}
