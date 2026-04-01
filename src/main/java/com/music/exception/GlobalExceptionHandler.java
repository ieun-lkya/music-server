package com.music.exception;

import com.music.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器 - "管家"
 * 统一捕获所有未处理的异常，返回友好的 JSON 提示
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理所有 Exception 类型异常
     * @param e 异常对象
     * @return 统一的错误响应结果
     */
    @ExceptionHandler(Exception.class)
    public Result<String> handleException(Exception e) {
        log.error("====== 系统运行时发生未捕获异常 ======", e);
        return Result.error("服务器开小差了，请稍后再试或联系管理员");
    }
}
