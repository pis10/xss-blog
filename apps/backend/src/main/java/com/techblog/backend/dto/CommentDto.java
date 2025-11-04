package com.techblog.backend.dto;

import java.time.LocalDateTime;

/**
 * 评论数据传输对象
 * 用于前后端数据交换
 */
public class CommentDto {
    /**
     * 评论 ID
     */
    private Long id;
    
    /**
     * 评论内容（HTML 格式）
     */
    private String contentHtml;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 评论用户
     */
    private UserDto user;

    // Getter 和 Setter 方法
    
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getContentHtml() {
        return contentHtml;
    }

    public void setContentHtml(String contentHtml) {
        this.contentHtml = contentHtml;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public UserDto getUser() {
        return user;
    }

    public void setUser(UserDto user) {
        this.user = user;
    }
}
