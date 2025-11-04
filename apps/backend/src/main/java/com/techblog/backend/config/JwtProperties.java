package com.techblog.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * JWT 配置属性类
 * 从 application.yml 中读取 security.jwt.* 配置
 * 
 * 配置项：
 * - secret: JWT 签名密钥（HS256 算法）
 * - issuer: JWT 签发者标识
 * - accessTtlMinutes: 访问令牌有效期（分钟）
 */
@Configuration
@ConfigurationProperties(prefix = "security.jwt")
public class JwtProperties {
    /** JWT 签名密钥，用于 HS256 算法 */
    private String secret;
    
    /** JWT 签发者标识 */
    private String issuer;
    
    /** 访问令牌有效期（分钟），默认 30 分钟 */
    private Integer accessTtlMinutes = 30;
    
    // Getter 和 Setter 方法
    
    public String getSecret() {
        return secret;
    }
    
    public void setSecret(String secret) {
        this.secret = secret;
    }
    
    public String getIssuer() {
        return issuer;
    }
    
    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }
    
    public Integer getAccessTtlMinutes() {
        return accessTtlMinutes;
    }
    
    public void setAccessTtlMinutes(Integer accessTtlMinutes) {
        this.accessTtlMinutes = accessTtlMinutes;
    }
}
