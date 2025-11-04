package com.techblog.backend.controller;

import com.techblog.backend.dto.ArticleDto;
import com.techblog.backend.dto.UserDto;
import com.techblog.backend.service.ArticleService;
import com.techblog.backend.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 个人主页控制器
 * 提供用户主页查询和个人简介编辑功能
 */
@RestController
@RequestMapping("/api/profile")
public class ProfileController {
    
    // 用户服务，处理用户信息的业务逻辑
    private final UserService userService;
    // 文章服务，处理文章查询的业务逻辑
    private final ArticleService articleService;
    
    /**
     * 构造函数注入依赖
     */
    public ProfileController(UserService userService, ArticleService articleService) {
        this.userService = userService;
        this.articleService = articleService;
    }
    
    /**
     * 获取用户主页（包含用户信息和文章列表）
     * 
     * @param username 用户名
     * @param page 页码，从 0 开始，默认为 0
     * @param size 每页大小，默认为 10
     * @return 用户信息和文章列表
     */
    @GetMapping("/{username}")
    public ResponseEntity<Map<String, Object>> getUserProfile(
            @PathVariable String username,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        UserDto user = userService.getUserByUsername(username);
        Pageable pageable = PageRequest.of(page, size);
        Page<ArticleDto> articles = articleService.getArticlesByAuthor(username, pageable);
        
        return ResponseEntity.ok(Map.of(
            "user", user,
            "articles", articles
        ));
    }
    
    /**
     * 更新用户个人简介（XSS L2 演示入口点）
     * 
     * @param request 请求体，包含 bio 字段
     * @param authentication Spring Security 认证对象
     * @return 更新后的用户信息
     */
    @PostMapping("/bio")
    public ResponseEntity<Map<String, Object>> updateBio(
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }
        
        String username = authentication.getName();
        String bio = request.get("bio");
        UserDto updated = userService.updateBio(username, bio);
        
        return ResponseEntity.ok(Map.of("ok", true, "user", updated));
    }
}
