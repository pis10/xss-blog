package com.techblog.backend.dto;

import java.time.LocalDateTime;

/**
 * 反馈数据传输对象
 * 用于前后端数据交换
 */
public class FeedbackDto {
    /**
     * 反馈 ID
     */
    private Long id;
    
    /**
     * 用户邮箱
     */
    private String email;
    
    /**
     * 反馈内容（HTML 格式）
     */
    private String contentHtml;
    
    /**
     * 反馈状态（pending/reviewed）
     */
    private String status;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    // Getter 和 Setter 方法
    
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getContentHtml() {
        return contentHtml;
    }

    public void setContentHtml(String contentHtml) {
        this.contentHtml = contentHtml;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
