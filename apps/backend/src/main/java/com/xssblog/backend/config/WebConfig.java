package com.xssblog.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

/**
 * Web 配置类
 * 配置跨域资源共享（CORS）策略
 * 
 * 允许的源：
 * - localhost:5173 (Vite 开发服务器)
 * - localhost:5174 (Webpack 开发服务器)
 * - 127.0.0.1:5173, 127.0.0.1:5174 (本地回环地址)
 * - localhost:80/localhost (生产环境)
 * 
 * 注意：仅用于开发和教学环境，生产环境需要更严格的 CORS 策略
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    /**
     * CORS 配置源 Bean
     * Spring Security 6 推荐的标准方式
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        
        // 允许的源地址（包含 localhost 和 127.0.0.1）
        config.setAllowedOrigins(Arrays.asList(
            "http://localhost:5173",
            "http://localhost:5174",
            "http://127.0.0.1:5173",
            "http://127.0.0.1:5174",
            "http://localhost:80",
            "http://localhost"
        ));
        
        // 允许的 HTTP 方法
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        
        // 允许的请求头（白名单）
        config.setAllowedHeaders(Arrays.asList(
            "Content-Type",
            "Authorization",
            "X-Requested-With",
            "Accept",
            "Origin"
        ));
        
        // 允许携带凭证（Cookie、Authorization 头等）
        config.setAllowCredentials(true);
        
        // 预检请求缓存时间（秒）
        config.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        
        return source;
    }
}
