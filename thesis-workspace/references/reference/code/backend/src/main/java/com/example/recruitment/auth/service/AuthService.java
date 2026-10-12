package com.example.recruitment.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.recruitment.auth.dto.LoginRequest;
import com.example.recruitment.auth.dto.LoginResponse;
import com.example.recruitment.auth.dto.RegisterRequest;
import com.example.recruitment.common.constant.CommonConstants;
import com.example.recruitment.common.constant.RoleConstants;
import com.example.recruitment.common.exception.BusinessException;
import com.example.recruitment.common.security.AuthUser;
import com.example.recruitment.common.utils.JwtUtils;
import com.example.recruitment.company.service.CompanyService;
import com.example.recruitment.log.service.LogService;
import com.example.recruitment.user.dto.UserProfileVO;
import com.example.recruitment.user.entity.SysUser;
import com.example.recruitment.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 认证服务：注册、登录、当前用户
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;
    private final CompanyService companyService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final LogService logService;

    public UserProfileVO register(RegisterRequest request, HttpServletRequest httpRequest) {
        if (userService.getByUsername(request.getUsername()) != null) {
            throw BusinessException.conflict("用户名已存在");
        }
        SysUser user = userService.createUser(request.getUsername(), request.getPassword(),
                request.getNickname(), request.getPhone(), request.getEmail(), request.getRole());
        if (RoleConstants.COMPANY.equals(request.getRole())) {
            companyService.createCompanyForUser(user.getId(), request.getNickname());
        }
        logService.saveOperateLog("认证", "注册账号", "username=" + request.getUsername()
                + ", role=" + request.getRole());
        return userService.toProfileVO(user);
    }

    public LoginResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        SysUser user = userService.getByUsername(request.getUsername());
        String ip = httpRequest.getRemoteAddr();
        if (user == null || !passwordEncoder.matches(request.getPassword(),
                user.getPassword() == null ? "" : user.getPassword())) {
            logService.saveLoginLog(request.getUsername(), false, ip, "用户名或密码错误");
            throw BusinessException.badRequest("用户名或密码错误");
        }
        if (!CommonConstants.USER_ENABLED.equals(user.getStatus())) {
            logService.saveLoginLog(request.getUsername(), false, ip, "账号已被禁用");
            throw BusinessException.forbidden("账号已被禁用，请联系管理员");
        }
        AuthUser authUser = new AuthUser(user.getId(), user.getUsername(),
                user.getNickname(), user.getRole());
        String token = jwtUtils.generateToken(authUser);
        logService.saveLoginLog(request.getUsername(), true, ip, "登录成功");
        return new LoginResponse(token, userService.toProfileVO(user));
    }

    public UserProfileVO me() {
        return userService.currentProfile();
    }
}
