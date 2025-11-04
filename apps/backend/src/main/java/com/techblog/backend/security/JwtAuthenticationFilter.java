package com.techblog.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * JWT 认证过滤器
 * 拦截 HTTP 请求，从请求中提取 JWT 并验证身份
 * 
 * 双态实现：
 * - VULN 模式：从 Authorization 请求头读取 JWT
 * - SECURE 模式：从 HttpOnly Cookie 读取 JWT
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    /**
     * 日志记录器
     */
    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    
    private final JwtTokenProvider jwtTokenProvider;
    
    /**
     * 构造函数注入依赖
     */
    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }
    
    /**
     * 过滤器核心逻辑：提取并验证 JWT
     */
    /**
     * 过滤器核心逻辑：提取并验证 JWT
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String jwt = extractJwtFromRequest(request);
            
            if (jwt != null) {
                String username = jwtTokenProvider.getUsernameFromToken(jwt);
                String role = jwtTokenProvider.getRoleFromToken(jwt);
                
                UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(
                        username, 
                        null, 
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))
                    );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("Authenticated user: {} with role: {}", username, role);
            }
        } catch (Exception e) {
            log.error("Cannot set user authentication: {}", e.getMessage());
        }
        
        filterChain.doFilter(request, response);
    }
    
    /**
     * 从 HTTP 请求中提取 JWT Token
     * 优先级：
     * 1. Authorization 请求头（VULN 模式）
     * 2. HttpOnly Cookie（SECURE 模式）
     */
    /**
     * 从 HTTP 请求中提取 JWT Token
     * 优先级：
     * 1. Authorization 请求头（VULN 模式）
     * 2. HttpOnly Cookie（SECURE 模式）
     */
    private String extractJwtFromRequest(HttpServletRequest request) {
        // Priority 1: Authorization header (VULN mode)
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        
        // Priority 2: HttpOnly Cookie (SECURE mode)
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("access".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        
        return null;
    }
}
