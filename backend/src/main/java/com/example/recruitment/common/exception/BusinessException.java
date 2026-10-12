package com.example.recruitment.common.exception;

import com.example.recruitment.common.constant.CommonConstants;
import lombok.Getter;

/**
 * 业务异常
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(String message) {
        super(message);
        this.code = CommonConstants.CODE_BAD_REQUEST;
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public static BusinessException badRequest(String message) {
        return new BusinessException(CommonConstants.CODE_BAD_REQUEST, message);
    }

    public static BusinessException notFound(String message) {
        return new BusinessException(CommonConstants.CODE_NOT_FOUND, message);
    }

    public static BusinessException unauthorized(String message) {
        return new BusinessException(CommonConstants.CODE_UNAUTHORIZED, message);
    }

    public static BusinessException forbidden(String message) {
        return new BusinessException(CommonConstants.CODE_FORBIDDEN, message);
    }

    public static BusinessException conflict(String message) {
        return new BusinessException(CommonConstants.CODE_CONFLICT, message);
    }

    public static BusinessException aiError(String message) {
        return new BusinessException(CommonConstants.CODE_AI_ERROR, message);
    }
}
