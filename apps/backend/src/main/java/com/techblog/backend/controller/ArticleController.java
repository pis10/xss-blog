package com.techblog.backend.controller;

import com.techblog.backend.dto.ArticleDto;
import com.techblog.backend.dto.CommentDto;
import com.techblog.backend.service.ArticleService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 文章控制器
 * 提供文章查询和评论查询功能（公开访问，无需登录）
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
     * 获取文章的评论列表
     * 
     * @param id 文章 ID
     * @return 评论列表
     */
    @GetMapping("/{id}/comments")
    public ResponseEntity<List<CommentDto>> getArticleComments(@PathVariable Long id) {
        return ResponseEntity.ok(articleService.getArticleComments(id));
    }
}
