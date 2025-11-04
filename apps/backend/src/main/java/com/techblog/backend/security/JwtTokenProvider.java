package com.techblog.backend.security;

import com.techblog.backend.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

/**
 * JWT 令牌提供者
 * 负责 JWT 令牌的生成、验证和解析
 * 
 * 使用 HS256 算法（HMAC + SHA256）签名 JWT
 * 令牌包含信息：
 * - subject: 用户名
 * - role: 用户角色（ADMIN/USER）
 * - issuer: 签发者
 * - issuedAt: 签发时间
 * - expiration: 过期时间
 * - jti: 令牌唯一 ID
 */
@Component
public class JwtTokenProvider {
    
    private final JwtProperties jwtProperties;
    private SecretKey secretKey;
    
    /**
     * 构造函数，注入 JWT 配置
     * @param jwtProperties JWT 配置属性
     */
    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }
    
    /**
     * 初始化方法，构建后自动执行
     * 将配置的密钥字符串转换为 SecretKey 对象
     */
    @PostConstruct
    public void init() {
        this.secretKey = Keys.hmacShaKeyFor(
            jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8)
        );
    }
    
    /**
     * 生成 JWT 令牌
     * 
     * @param username 用户名
     * @param role 用户角色（ADMIN/USER）
     * @return JWT 令牌字符串
     */
    public String generateToken(String username, String role) {
        Instant now = Instant.now();
        Instant expiration = now.plus(jwtProperties.getAccessTtlMinutes(), ChronoUnit.MINUTES);
        
        return Jwts.builder()
                .subject(username)  // 设置主题（用户名）
                .claim("role", role)  // 自定义声明：用户角色
                .issuer(jwtProperties.getIssuer())  // 签发者
                .issuedAt(Date.from(now))  // 签发时间
                .expiration(Date.from(expiration))  // 过期时间
                .id(UUID.randomUUID().toString())  // 令牌唯一 ID
                .signWith(secretKey)  // 使用密钥签名
                .compact();
    }
    
    /**
     * 验证并解析 JWT 令牌
     * 
     * @param token JWT 令牌字符串
     * @return 令牌负载（Claims）
     * @throws io.jsonwebtoken.JwtException 令牌无效或过期时抛出
     */
    public Claims validateToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)  // 验证签名
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
    
    /**
     * 从令牌中提取用户名
     * 
     * @param token JWT 令牌
     * @return 用户名
     */
    public String getUsernameFromToken(String token) {
        return validateToken(token).getSubject();
    }
    
    /**
     * 从令牌中提取用户角色
     * 
     * @param token JWT 令牌
     * @return 用户角色（ADMIN/USER）
     */
    public String getRoleFromToken(String token) {
        return validateToken(token).get("role", String.class);
    }
}
