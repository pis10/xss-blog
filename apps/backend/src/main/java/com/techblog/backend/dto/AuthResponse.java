package com.techblog.backend.dto;

/**
 * 认证响应数据传输对象
 * 用于返回 JWT 访问令牌
 * 
 * 注意：
 * - VULN 模式：会返回该对象，前端存入 localStorage
 * - SECURE 模式：不返回该对象，JWT 通过 HttpOnly Cookie 返回
 */
public class AuthResponse {
    /**
     * JWT 访问令牌
     */
    private String accessToken;

    /**
     * 无参构造函数
     */
    public AuthResponse() {
    }

    /**
     * 全参构造函数
     * @param accessToken JWT 访问令牌
     */
    public AuthResponse(String accessToken) {
        this.accessToken = accessToken;
    }

    // Getter 和 Setter 方法
    
    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
}
