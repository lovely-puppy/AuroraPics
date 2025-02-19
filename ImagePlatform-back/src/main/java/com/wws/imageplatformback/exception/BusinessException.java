package com.wws.imageplatformback.exception;

import lombok.Getter;

/**
 * @author 汪文松
 * @date 2025-01-17 23:10
 * 自定义异常
 */
@Getter
public class BusinessException extends RuntimeException{
    /**
     *状态码
     */
    private final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }
}
