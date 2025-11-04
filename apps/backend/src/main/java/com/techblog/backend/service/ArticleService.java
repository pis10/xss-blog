package com.techblog.backend.service;

import com.techblog.backend.dto.*;
import com.techblog.backend.entity.Article;
import com.techblog.backend.entity.Comment;
import com.techblog.backend.entity.Tag;
import com.techblog.backend.entity.User;
import com.techblog.backend.repository.ArticleRepository;
import com.techblog.backend.repository.CommentRepository;
import com.techblog.backend.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    
    /**
     * 构造函数，注入依赖
     * @param articleRepository 文章仓库
     * @param commentRepository 评论仓库
     * @param userRepository 用户仓库
     */
    public ArticleService(ArticleRepository articleRepository,
                         CommentRepository commentRepository,
                         UserRepository userRepository) {
        this.articleRepository = articleRepository;
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
    }
    
    public Page<ArticleDto> getAllArticles(Pageable pageable) {
        return articleRepository.findAllByOrderByPublishedAtDesc(pageable)
            .map(this::mapToDto);
    }
    
    public ArticleDto getArticleById(Long id) {
        Article article = articleRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Article not found"));
        return mapToDto(article);
    }
    
    public ArticleDto getArticleBySlug(String slug) {
        Article article = articleRepository.findBySlug(slug)
            .orElseThrow(() -> new RuntimeException("Article not found"));
        return mapToDto(article);
    }
    
    public List<CommentDto> getArticleComments(Long articleId) {
        return commentRepository.findByArticleIdOrderByCreatedAtDesc(articleId)
            .stream()
            .map(this::mapCommentToDto)
            .collect(Collectors.toList());
    }
    
    public Page<ArticleDto> getArticlesByAuthor(String username, Pageable pageable) {
        return articleRepository.findByAuthorUsernameOrderByPublishedAtDesc(username, pageable)
            .map(this::mapToDto);
    }
    
    private ArticleDto mapToDto(Article article) {
        ArticleDto dto = new ArticleDto();
        dto.setId(article.getId());
        dto.setTitle(article.getTitle());
        dto.setSlug(article.getSlug());
        dto.setExcerpt(article.getExcerpt());
        dto.setContentHtml(article.getContentHtml());
        dto.setLikesCount(article.getLikesCount());
        dto.setPublishedAt(article.getPublishedAt());
        dto.setCreatedAt(article.getCreatedAt());
        dto.setAuthor(mapUserToDto(article.getAuthor()));
        dto.setTags(article.getTags().stream().map(this::mapTagToDto).collect(Collectors.toList()));
        return dto;
    }
    
    private UserDto mapUserToDto(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setAvatarUrl(user.getAvatarUrl());
        dto.setBannerUrl(user.getBannerUrl());
        dto.setBio(user.getBio());
        return dto;
    }
    
    private TagDto mapTagToDto(Tag tag) {
        TagDto dto = new TagDto();
        dto.setId(tag.getId());
        dto.setName(tag.getName());
        dto.setColor(tag.getColor());
        return dto;
    }
    
    private CommentDto mapCommentToDto(Comment comment) {
        CommentDto dto = new CommentDto();
        dto.setId(comment.getId());
        dto.setContentHtml(comment.getContentHtml());
        dto.setCreatedAt(comment.getCreatedAt());
        dto.setUser(mapUserToDto(comment.getUser()));
        return dto;
    }
}
