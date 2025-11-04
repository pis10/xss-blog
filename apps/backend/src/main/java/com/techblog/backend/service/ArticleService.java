package com.techblog.backend.service;

import com.techblog.backend.common.exception.ResourceNotFoundException;
import com.techblog.backend.config.XssProperties;
import com.techblog.backend.dto.*;
import com.techblog.backend.entity.Article;
import com.techblog.backend.entity.Comment;
import com.techblog.backend.entity.User;
import com.techblog.backend.mapper.ArticleMapper;
import com.techblog.backend.mapper.CommentMapper;
import com.techblog.backend.repository.ArticleRepository;
import com.techblog.backend.repository.CommentRepository;
import com.techblog.backend.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 文章服务
 * 负责文章和评论的查询业务逻辑
 */
@Service
@Transactional(readOnly = true)
public class ArticleService {
    
    private final ArticleRepository articleRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final ArticleMapper articleMapper;
    private final CommentMapper commentMapper;
    private final XssProperties xssProperties;
    
    /**
     * 构造函数，注入依赖
     * @param articleRepository 文章仓库
     * @param commentRepository 评论仓库
     * @param userRepository 用户仓库
     * @param articleMapper 文章对象映射器
     * @param commentMapper 评论对象映射器
     * @param xssProperties XSS 模式配置
     */
    public ArticleService(ArticleRepository articleRepository,
                         CommentRepository commentRepository,
                         UserRepository userRepository,
                         ArticleMapper articleMapper,
                         CommentMapper commentMapper,
                         XssProperties xssProperties) {
        this.articleRepository = articleRepository;
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
        this.articleMapper = articleMapper;
        this.commentMapper = commentMapper;
        this.xssProperties = xssProperties;
    }
    
    public Page<ArticleDto> getAllArticles(Pageable pageable) {
        return articleRepository.findAllByOrderByPublishedAtDesc(pageable)
            .map(articleMapper::toDto);
    }
    
    public ArticleDto getArticleById(Long id) {
        Article article = articleRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Article", id));
        return articleMapper.toDto(article);
    }
    
    public ArticleDto getArticleBySlug(String slug) {
        Article article = articleRepository.findBySlug(slug)
            .orElseThrow(() -> new ResourceNotFoundException("Article with slug: " + slug));
        return articleMapper.toDto(article);
    }
    
    public List<CommentDto> getArticleComments(Long articleId) {
        return commentRepository.findByArticleIdOrderByCreatedAtDesc(articleId)
            .stream()
            .map(commentMapper::toDto)
            .collect(Collectors.toList());
    }
    
    public Page<ArticleDto> getArticlesByAuthor(String username, Pageable pageable) {
        return articleRepository.findByAuthorUsernameOrderByPublishedAtDesc(username, pageable)
            .map(articleMapper::toDto);
    }
    
    /**
     * 提交文章评论（需要登录）
     * 
     * XSS 双模式处理：
     * - VULN 模式：直接存储用户提交的 HTML 内容（存储型 XSS 漏洞）
     * - SECURE 模式：对 HTML 内容进行转义后存储，防止 XSS 攻击
     * 
     * @param articleId 文章 ID
     * @param username 评论用户名（从 JWT 获取）
     * @param request 评论请求
     * @return 评论 DTO
     */
    @Transactional
    public CommentDto createComment(Long articleId, String username, CommentRequest request) {
        // 验证文章是否存在
        Article article = articleRepository.findById(articleId)
            .orElseThrow(() -> new ResourceNotFoundException("Article", articleId));
        
        // 查找用户
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User", username));
        
        // 创建评论
        Comment comment = new Comment();
        comment.setArticle(article);
        comment.setUser(user);
        
        // XSS 双模式处理
        String content = request.getContent();
        if (xssProperties.isSecure()) {
            // SECURE 模式：HTML 转义，防止 XSS
            comment.setContentHtml(HtmlUtils.htmlEscape(content));
        } else {
            // VULN 模式：直接存储（存在 XSS 漏洞）
            comment.setContentHtml(content);
        }
        
        commentRepository.save(comment);
        return commentMapper.toDto(comment);
    }
}
