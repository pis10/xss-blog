package com.techblog.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文章数据传输对象
 * 用于前后端数据交换
 */
public class ArticleDto {
    /**
     * 文章 ID
     */
    private Long id;
    
    /**
     * 文章标题
     */
    private String title;
    
    /**
     * 文章 slug（URL 友好的标识符）
     */
    private String slug;
    
    /**
     * 文章摘要
     */
    private String excerpt;
    
    /**
     * 文章内容（HTML 格式）
     */
    private String contentHtml;
    
    /**
     * 点赞数
     */
    private Integer likesCount;
    
    /**
     * 发布时间
     */
    private LocalDateTime publishedAt;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 文章作者
     */
    private UserDto author;
    
    /**
     * 文章标签列表
     */
    private List<TagDto> tags;

    // Getter 和 Setter 方法
    
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getExcerpt() {
        return excerpt;
    }

    public void setExcerpt(String excerpt) {
        this.excerpt = excerpt;
    }

    public String getContentHtml() {
        return contentHtml;
    }

    public void setContentHtml(String contentHtml) {
        this.contentHtml = contentHtml;
    }

    public Integer getLikesCount() {
        return likesCount;
    }

    public void setLikesCount(Integer likesCount) {
        this.likesCount = likesCount;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public UserDto getAuthor() {
        return author;
    }

    public void setAuthor(UserDto author) {
        this.author = author;
    }

    public List<TagDto> getTags() {
        return tags;
    }

    public void setTags(List<TagDto> tags) {
        this.tags = tags;
    }
}
