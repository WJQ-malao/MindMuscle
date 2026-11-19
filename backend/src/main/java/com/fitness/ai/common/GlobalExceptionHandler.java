package com.fitness.ai.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public Result<String> handleBadRequest(RuntimeException e) {
        return Result.fail(e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<String> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getDefaultMessage())
                .findFirst()
                .orElse("参数校验失败");
        return Result.fail(msg);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<String> handleNotReadable(HttpMessageNotReadableException e) {
        log.error("请求体解析失败", e);
        return Result.fail("请求体格式错误：" + e.getMostSpecificCause().getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<String> handleUploadSize(MaxUploadSizeExceededException e) {
        return Result.fail("文件超过 20MB 限制");
    }

    @ExceptionHandler(Exception.class)
    public Result<String> handleOther(Exception e) {
        log.error("未处理异常", e);
        return Result.fail("服务器内部错误：" + e.getMessage());
    }
}
