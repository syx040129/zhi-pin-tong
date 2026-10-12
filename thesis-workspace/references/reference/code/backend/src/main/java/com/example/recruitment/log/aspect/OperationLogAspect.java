package com.example.recruitment.log.aspect;

import com.example.recruitment.log.annotation.OpLog;
import com.example.recruitment.log.service.LogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 操作日志切面
 */
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final LogService logService;

    @AfterReturning("@annotation(opLog)")
    public void afterReturning(JoinPoint joinPoint, OpLog opLog) {
        try {
            String detail = buildDetail(joinPoint);
            logService.saveOperateLog(opLog.module(), opLog.action(), detail);
        } catch (Exception ignored) {
        }
    }

    private String buildDetail(JoinPoint joinPoint) {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                String query = request.getQueryString();
                return request.getRequestURI() + (query == null ? "" : "?" + query);
            }
        } catch (Exception ignored) {
        }
        return joinPoint.getSignature().toShortString();
    }
}
