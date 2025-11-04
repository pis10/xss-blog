package com.techblog.backend.controller;

import com.techblog.backend.dto.ArticleDto;
import com.techblog.backend.dto.CommentDto;
import com.techblog.backend.dto.CommentRequest;
import com.techblog.backend.service.ArticleService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 文章控制器
 * 提供文章查询、评论查询和评论提交功能
 * 
 * 权限说明：
 * - 文章查询：公开访问，无需登录
 * - 评论查询：公开访问，无需登录
 * - 评论提交：需要登录（JWT 认证）
 */
@RestController
@RequestMapping("/api/articles")
public class ArticleController {
    
    // 文章服务，处理文章和评论的业务逻辑
    private final ArticleService articleService;
    
    /**
     * 构造函数注入依赖
     */
    public ArticleController(ArticleService articleService) {
        this.articleService = articleService;
    }
    
    /**
     * 分页获取所有文章列表
     * 
     * @param page 页码，从 0 开始，默认为 0
     * @param size 每页大小，默认为 10
     * @return 分页的文章数据
     */
    @GetMapping
    public ResponseEntity<Page<ArticleDto>> getAllArticles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(articleService.getAllArticles(pageable));
    }
    
    /**
     * 根据 ID 获取文章详情
     * 
     * @param id 文章 ID
     * @return 文章详情 DTO
     */
    @GetMapping("/{id}")
    public ResponseEntity<ArticleDto> getArticleById(@PathVariable Long id) {
        return ResponseEntity.ok(articleService.getArticleById(id));
    }
    
    /**
     * 根据 Slug 获取文章详情
     * 
     * @param slug 文章 Slug（URL 友好标识）
     * @return 文章详情 DTO
     */
    @GetMapping("/slug/{slug}")
    public ResponseEntity<ArticleDto> getArticleBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(articleService.getArticleBySlug(slug));
    }
    
    /**
     * 获取文章的评论列表（公开访问）
     * 
     * @param id 文章 ID
     * @return 评论列表
     */
    @GetMapping("/{id}/comments")
    public ResponseEntity<List<CommentDto>> getArticleComments(@PathVariable Long id) {
        return ResponseEntity.ok(articleService.getArticleComments(id));
    }
    
    /**
     * 提交文章评论（需要登录）
     * 
     * XSS 演示说明：
     * - VULN 模式：后端直接存储用户提交的 HTML，存在存储型 XSS 漏洞
     * - SECURE 模式：后端对 HTML 进行转义，前端再用 DOMPurify 二次过滤
     * 
     * @param id 文章 ID
     * @param request 评论请求（content 字段）
     * @param authentication 当前认证用户（由 Spring Security 自动注入）
     * @return 创建的评论 DTO
     */
    @PostMapping("/{id}/comments")
    public ResponseEntity<CommentDto> createComment(
            @PathVariable Long id,
            @Valid @RequestBody CommentRequest request,
            Authentication authentication) {
        String username = authentication.getName();
        CommentDto comment = articleService.createComment(id, username, request);
        return ResponseEntity.ok(comment);
    }
}
