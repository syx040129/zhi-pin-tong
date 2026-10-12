package com.example.recruitment.log.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.recruitment.common.constant.CommonConstants;
import com.example.recruitment.common.security.AuthUser;
import com.example.recruitment.common.utils.JsonUtils;
import com.example.recruitment.log.entity.SysLog;
import com.example.recruitment.log.mapper.SysLogMapper;
import com.example.recruitment.user.entity.SysUser;
import com.example.recruitment.user.mapper.SysUserMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 系统日志服务：登录 / 操作 / AI 调用 / 异常日志统一落库
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LogService {

    private final SysLogMapper sysLogMapper;
    private final SysUserMapper sysUserMapper;

    public void saveLoginLog(String username, boolean success, String ip, String detail) {
        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username).last("limit 1"));
        SysLog sysLog = new SysLog();
        sysLog.setType(CommonConstants.LOG_LOGIN);
        sysLog.setModule("认证");
        sysLog.setAction(success ? "登录成功" : "登录失败");
        sysLog.setDetail(detail);
        sysLog.setIp(ip);
        if (user != null) {
            sysLog.setUserId(user.getId());
            sysLog.setUsername(user.getUsername());
        } else {
            sysLog.setUsername(username);
        }
        doSave(sysLog);
    }

    public void saveAiLog(String module, String action, String detail, long costMs) {
        SysLog sysLog = baseLog();
        sysLog.setType(CommonConstants.LOG_AI);
        sysLog.setModule(module);
        sysLog.setAction(action);
        sysLog.setDetail(detail);
        sysLog.setCostMs(costMs);
        doSave(sysLog);
    }

    public void saveOperateLog(String module, String action, String detail) {
        SysLog sysLog = baseLog();
        sysLog.setType(CommonConstants.LOG_OPERATE);
        sysLog.setModule(module);
        sysLog.setAction(action);
        sysLog.setDetail(detail);
        doSave(sysLog);
    }

    public void saveErrorLogAsync(String detail) {
        try {
            SysLog sysLog = baseLog();
            sysLog.setType(CommonConstants.LOG_ERROR);
            sysLog.setModule("系统异常");
            sysLog.setAction("异常记录");
            sysLog.setDetail(detail == null ? "" : (detail.length() > 2000 ? detail.substring(0, 2000) : detail));
            doSave(sysLog);
        } catch (Exception ignored) {
        }
    }

    public List<SysLog> page(int page, int size, String type) {
        LambdaQueryWrapper<SysLog> qw = new LambdaQueryWrapper<>();
        if (type != null && !type.isBlank()) {
            qw.eq(SysLog::getType, type);
        }
        qw.orderByDesc(SysLog::getId);
        return sysLogMapper.selectList(qw);
    }

    private SysLog baseLog() {
        SysLog sysLog = new SysLog();
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof AuthUser user) {
                sysLog.setUserId(user.getId());
                sysLog.setUsername(user.getUsername());
            }
        } catch (Exception ignored) {
        }
        sysLog.setIp(currentIp());
        sysLog.setCreateTime(LocalDateTime.now());
        return sysLog;
    }

    private void doSave(SysLog sysLog) {
        try {
            if (sysLog.getDetail() != null && sysLog.getDetail().length() > 2000) {
                sysLog.setDetail(sysLog.getDetail().substring(0, 2000));
            }
            sysLogMapper.insert(sysLog);
        } catch (Exception e) {
            log.warn("日志写入失败: {}", e.getMessage());
        }
    }

    private String currentIp() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                return "";
            }
            HttpServletRequest request = attrs.getRequest();
            String ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isBlank()) {
                ip = request.getRemoteAddr();
            }
            return ip.contains(",") ? ip.split(",")[0].trim() : ip;
        } catch (Exception e) {
            return "";
        }
    }
}
