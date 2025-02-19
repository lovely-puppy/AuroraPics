package com.wws.imageplatformback.utils;

import com.wws.imageplatformback.exception.BusinessException;
import com.wws.imageplatformback.exception.ErrorCode;

/**
 * @author 汪文松
 * @date 2025-01-17 23:21
 * 异常处理工具类，使用assert断言
 */
public class ThrowUtils {
    /**
     *
     * @param condition
     * @param runtimeException
     */
    public static void throwIf(boolean condition, RuntimeException runtimeException) {
        if (condition) {
            throw runtimeException;
        }
    }
    public static void throwIf(boolean condition, ErrorCode errorCode) {
        throwIf(condition, new BusinessException(errorCode));
    }

    /**
     *
     * @param condition 条件
     * @param errorCode 状态码
     * @param message 错误消息
     */
    public static void throwIf(boolean condition, ErrorCode errorCode, String message) {
        throwIf(condition, new BusinessException(errorCode, message));
    }
}
