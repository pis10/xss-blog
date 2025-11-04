package com.techblog.backend.controller;

import com.techblog.backend.config.XssProperties;
import com.techblog.backend.dto.*;
import com.techblog.backend.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 * 提供用户注册、登录、登出功能
 * 
 * 双态实现（根据 XSS 模式切换 JWT 存储方式）：
 * - VULN 模式：JWT 存储在 localStorage（易受 XSS 攻击窃取）
 * - SECURE 模式：JWT 存储在 HttpOnly Cookie（防止 XSS 窃取）
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    // 认证服务，处理注册、登录、用户查询等业务逻辑
    private final AuthService authService;
    // XSS 属性配置，用于判断当前模式（VULN/SECURE）
    private final XssProperties xssProperties;
    
    /**
     * 构造函数注入依赖
     */
    public AuthController(AuthService authService, XssProperties xssProperties) {
        this.authService = authService;
        this.xssProperties = xssProperties;
    }
    
    /**
     * 用户注册接口
     * 
     * @param request 注册请求（包含 username, password）
     * @param response HTTP 响应对象（用于设置 Cookie）
     * @return VULN 模式返回 JWT，SECURE 模式返回 204 No Content
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request, 
                                      HttpServletResponse response) {
        AuthResponse authResponse = authService.register(request);
        
        if (xssProperties.isSecure()) {
            // SECURE mode: Set HttpOnly Cookie
            setSecureCookie(response, authResponse.getAccessToken());
            return ResponseEntity.noContent().build();
        } else {
            // VULN mode: Return JWT in response body
            return ResponseEntity.ok(authResponse);
        }
    }
    
    /**
     * 用户登录接口
     * 
     * @param request 登录请求（包含 username, password）
     * @param response HTTP 响应对象（用于设置 Cookie）
     * @return VULN 模式返回 JWT，SECURE 模式返回 204 No Content
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request, 
                                   HttpServletResponse response) {
        AuthResponse authResponse = authService.login(request);
        
        if (xssProperties.isSecure()) {
            // SECURE mode: Set HttpOnly Cookie
            setSecureCookie(response, authResponse.getAccessToken());
            return ResponseEntity.noContent().build();
        } else {
            // VULN mode: Return JWT in response body
            return ResponseEntity.ok(authResponse);
        }
    }
    
    /**
     * 用户登出接口
     * 
     * @param response HTTP 响应对象（用于清除 Cookie）
     * @return 204 No Content
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        if (xssProperties.isSecure()) {
            // Clear cookie
            Cookie cookie = new Cookie("access", "");
            cookie.setHttpOnly(true);
            cookie.setSecure(true);
            cookie.setPath("/");
            cookie.setMaxAge(0);
            response.addCookie(cookie);
        }
        return ResponseEntity.noContent().build();
    }
    
    /**
     * 获取当前登录用户信息
     * 
     * @param authentication Spring Security 认证对象
     * @return 用户信息 DTO
     */
    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }
        String username = authentication.getName();
        UserDto user = authService.getCurrentUser(username);
        return ResponseEntity.ok(user);
    }
    
    /**
     * 设置安全的 HttpOnly Cookie（SECURE 模式专用）
     * 
     * @param response HTTP 响应对象
     * @param token JWT Token
     */
    private void setSecureCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie("access", token);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // Set to true in production with HTTPS
        cookie.setPath("/");
        cookie.setMaxAge(30 * 60); // 30 minutes
        cookie.setAttribute("SameSite", "Strict");
        response.addCookie(cookie);
    }
}
