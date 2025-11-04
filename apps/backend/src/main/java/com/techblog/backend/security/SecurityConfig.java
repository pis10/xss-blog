package com.techblog.backend.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 安全配置类
 * 配置基于 JWT 的无状态认证机制
 * 
 * 关键配置：
 * 1. 禁用 CSRF（因为使用 JWT，不依赖 Cookie 会话）
 * 2. 启用 CORS（允许前端跨域访问）
 * 3. 无状态会话（STATELESS）
 * 4. 基于角色的访问控制（ADMIN/USER）
 * 5. JWT 过滤器集成
 * 
 * 权限访问规则：
 * - 公开访问：/api/auth/**, /api/config/**, /api/articles/**, /api/profile/**, /api/search, /api/feedback
 * - 管理员专用：/api/admin/** (ADMIN 角色)
 * - 其他接口：需要认证
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    
    /**
     * 构造函数注入依赖
     */
    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }
    
    /**
     * 密码加密器 Bean
     * 使用 BCrypt 算法加密密码（带盐值，防彩虹表攻击）
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    /**
     * 安全过滤器链配置
     * 定义 HTTP 安全策略和访问控制规则
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 禁用 CSRF 保护（JWT 无状态认证不需要 CSRF 保护）
            .csrf(AbstractHttpConfigurer::disable)
            // 启用 CORS 支持（使用 WebConfig 中的配置）
            .cors(cors -> cors.configure(http))
            // 无状态会话管理（不创建 HttpSession）
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // 请求授权配置
            .authorizeHttpRequests(auth -> auth
                // 公开访问的接口（登录、注册、配置、文章、个人主页、搜索、反馈）
                .requestMatchers("/api/auth/**", "/api/config/**", "/api/articles/**", 
                                "/api/profile/**", "/api/search", "/api/feedback").permitAll()
                // 管理员接口（仅 ADMIN 角色可访问）
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                // 其他所有请求均需认证
                .anyRequest().authenticated()
            )
            // 在 UsernamePasswordAuthenticationFilter 之前添加 JWT 过滤器
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
}
