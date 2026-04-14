package com.xu.home.config.timesheet.exception;

import com.xu.home.param.common.response.Response;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public Response<?> handleIllegalArgument(IllegalArgumentException ex) {
        return Response.error(ex.getMessage());
    }

    /**
     * 处理运行时业务异常
     */
    @ExceptionHandler(RuntimeException.class)
    public Response<?> handleRuntime(RuntimeException ex) {
        return Response.error(ex.getMessage());
    }

    /**
     * 处理未捕获异常
     */
    @ExceptionHandler(Exception.class)
    public Response<?> handleException(Exception ex) {
        return Response.error("系统异常，请稍后重试");
    }
}
