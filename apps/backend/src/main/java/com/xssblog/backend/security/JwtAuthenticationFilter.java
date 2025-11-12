package com.xssblog.backend.security;

import com.xssblog.backend.config.XssProperties;
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
 * 拦截 HTTP 请求,从请求中提取 JWT 并验证身份
 * 
 * 双态实现:
 * - VULN 模式:从 Authorization 请求头读取 JWT
 * - SECURE 模式:仅从 HttpOnly Cookie 读取 JWT(拒绝 Header)
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    /**
     * 日志记录器
     */
    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    
    private final JwtTokenProvider jwtTokenProvider;
    private final XssProperties xssProperties;
    
    /**
     * 构造函数注入依赖
     */
    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider,
                                   XssProperties xssProperties) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.xssProperties = xssProperties;
    }
    
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
     * 根据 XSS 模式严格区分来源：
     * - VULN 模式：优先 Authorization 头，降级至 Cookie
     * - SECURE 模式：仅接受 HttpOnly Cookie，拒绝 Header
     */
    private String extractJwtFromRequest(HttpServletRequest request) {
        if (xssProperties.isSecure()) {
            // SECURE 模式：仅从 Cookie 读取，强化教学对比
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if ("access".equals(cookie.getName())) {
                        log.debug("Token extracted from HttpOnly Cookie (SECURE mode)");
                        return cookie.getValue();
                    }
                }
            }
        } else {
            // VULN 模式：优先 Authorization 头，兼容 Cookie
            String bearerToken = request.getHeader("Authorization");
            if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
                log.debug("Token extracted from Authorization header (VULN mode)");
                return bearerToken.substring(7);
            }
            
            // 降级：从 Cookie 读取（兼容混合场景）
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if ("access".equals(cookie.getName())) {
                        log.debug("Token extracted from Cookie fallback (VULN mode)");
                        return cookie.getValue();
                    }
                }
            }
        }
        
        return null;
    }
}
