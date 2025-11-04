package com.techblog.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 反馈请求数据传输对象
 * 用于用户提交反馈
 */
public class FeedbackRequest {
    /**
     * 邮箱（必填，合法的邮箱格式）
     */
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;
    
    /**
     * 反馈内容（必填）
     */
    @NotBlank(message = "Content is required")
    private String content;

    // Getter 和 Setter 方法
    
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
