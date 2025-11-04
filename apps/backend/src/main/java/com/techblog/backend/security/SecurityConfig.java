package com.techblog.backend.security;

import com.techblog.backend.config.XssProperties;
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
    private final XssProperties xssProperties;
    
    /**
     * 构造函数注入依赖
     */
    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                         XssProperties xssProperties) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.xssProperties = xssProperties;
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
     * 
     * 根据 XSS 模式动态配置安全响应头：
     * - VULN 模式：不启用安全响应头，允许 XSS 攻击成功执行（教学演示）
     * - SECURE 模式：启用完整的安全响应头（CSP、X-Frame-Options、X-XSS-Protection）
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
                // 公开访问的接口（登录、注册、配置、文章查询、评论查询、个人主页、搜索、反馈）
                .requestMatchers("/api/auth/**", "/api/config/**", "/api/search", "/api/feedback").permitAll()
                // 文章相关：查询公开，评论提交需要认证
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/articles/**").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/profile/**").permitAll()
                // 管理员接口（仅 ADMIN 角色可访问）
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                // 其他所有请求均需认证
                .anyRequest().authenticated()
            )
            // 在 UsernamePasswordAuthenticationFilter 之前添加 JWT 过滤器
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        // 根据 XSS 模式动态配置安全响应头
        // SECURE 模式：启用完整的安全防护
        // VULN 模式：不启用安全响应头，保证 XSS 攻击能够成功执行（教学演示目的）
        if (xssProperties.isSecure()) {
            http.headers(headers -> headers
                // Content Security Policy（内容安全策略）
                // 限制资源加载来源，防止恶意脚本注入
                .contentSecurityPolicy(csp -> csp.policyDirectives(
                    "default-src 'self'; " +                    // 默认只允许同源资源
                    "script-src 'self'; " +                      // 只允许同源脚本
                    "style-src 'self' 'unsafe-inline'; " +      // 允许同源样式和内联样式（Element Plus需要）
                    "img-src 'self' data: https:; " +           // 允许同源、Data URI、HTTPS图片
                    "font-src 'self'; " +                       // 只允许同源字体
                    "connect-src 'self'; " +                    // 只允许同源AJAX请求
                    "frame-ancestors 'none';"                   // 禁止被iframe嵌入（防点击劫持）
                ))
                // X-Frame-Options: 禁止页面被iframe嵌入（防点击劫持）
                .frameOptions(frame -> frame.deny())
            );
        }
        // VULN 模式下不配置安全响应头，保留默认行为，允许XSS攻击成功
        
        return http.build();
    }
}
