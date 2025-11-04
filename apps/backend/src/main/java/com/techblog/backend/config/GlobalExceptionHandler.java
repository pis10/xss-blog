package com.techblog.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理器
 * 统一处理控制器层抛出的各类异常，返回标准化的错误响应
 * 
 * 处理的异常类型：
 * - MethodArgumentNotValidException: 参数校验失败
 * - AuthenticationException: 认证失败（用户名/密码错误）
 * - AccessDeniedException: 权限不足
 * - RuntimeException: 业务逻辑异常
 * - Exception: 未预期的系统异常
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    /**
     * 日志记录器
     */
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    /**
     * 处理参数校验异常（@Valid/@Validated 注解触发）
     * @param ex 校验异常对象
     * @return 包含所有字段错误信息的响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        log.warn("Validation failed: {}", errors);
        return ResponseEntity.badRequest().body(Map.of("errors", errors));
    }
    
    /**
     * 处理认证失败异常（用户名/密码错误、Token 无效等）
     * @param ex 认证异常对象
     * @return 401 Unauthorized 响应
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<?> handleAuthenticationException(AuthenticationException ex) {
        log.warn("Authentication failed: {}", ex.getMessage());
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(Map.of("error", "Invalid credentials"));
    }
    
    /**
     * 处理访问拒绝异常（权限不足）
     * @param ex 访问拒绝异常对象
     * @return 403 Forbidden 响应
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> handleAccessDeniedException(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(Map.of("error", "Access denied"));
    }
    
    /**
     * 处理业务逻辑异常（如用户不存在、数据不合法等）
     * @param ex 运行时异常对象
     * @return 400 Bad Request 响应
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleRuntimeException(RuntimeException ex) {
        log.error("Runtime exception: ", ex);
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(Map.of("error", ex.getMessage()));
    }
    
    /**
     * 处理未预期的系统异常（兜底处理）
     * @param ex 异常对象
     * @return 500 Internal Server Error 响应
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleException(Exception ex) {
        log.error("Unexpected error: ", ex);
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Map.of("error", "An internal error occurred"));
    }
}
