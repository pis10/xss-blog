package com.techblog.backend.dto;

import java.time.LocalDateTime;

/**
 * 用户数据传输对象
 * 用于前后端数据交换，不包含敏感信息（密码哈希）
 */
public class UserDto {
    /**
     * 用户 ID
     */
    private Long id;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 邮箱
     */
    private String email;
    
    /**
     * 角色（ADMIN 或 USER）
     */
    private String role;
    
    /**
     * 头像 URL
     */
    private String avatarUrl;
    
    /**
     * 背景图 URL
     */
    private String bannerUrl;
    
    /**
     * 用户简介（Bio）
     * 注意：此字段在 VULN 模式下存在 XSS 风险
     */
    private String bio;
    
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

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getBannerUrl() {
        return bannerUrl;
    }

    public void setBannerUrl(String bannerUrl) {
        this.bannerUrl = bannerUrl;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
