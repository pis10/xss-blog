package com.techblog.backend.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 反馈实体类
 * 用于存储用户提交的反馈信息
 * 
 * 安全注意：
 * - contentHtml 字段在 VULN 模式下存在盲 XSS 风险（L3 场景）
 */
@Entity
@Table(name = "feedbacks")
@EntityListeners(AuditingEntityListener.class)
public class Feedback {
    
    /**
     * 反馈 ID（数据库自增主键）
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 用户邮箱
     */
    @Column(nullable = false, length = 128)
    private String email;
    
    /**
     * 反馈内容（HTML 格式）
     * 注意：此字段是盲 XSS 攻击的触发点
     */
    @Column(name = "content_html", columnDefinition = "TEXT")
    private String contentHtml;
    
    /**
     * 反馈状态（NEW 或 READ）
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(20)")
    private FeedbackStatus status = FeedbackStatus.NEW;
    
    /**
     * 创建时间（自动填充）
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 反馈状态枚举
     */
    public enum FeedbackStatus {
        /** 新反馈（未读） */
        NEW,
        /** 已读 */
        READ
    }

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

    public FeedbackStatus getStatus() {
        return status;
    }

    public void setStatus(FeedbackStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
