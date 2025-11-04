package com.techblog.backend.entity;

import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;

/**
 * 标签实体类
 * 用于文章分类和筛选
 */
@Entity
@Table(name = "tags")
public class Tag {
    
    /**
     * 标签 ID（数据库自增主键）
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 标签名称（全局唯一）
     */
    @Column(nullable = false, unique = true, length = 50)
    private String name;
    
    /**
     * 标签颜色（十六进制颜色值）
     */
    @Column(length = 20)
    private String color;
    
    /**
     * 使用此标签的文章列表
     */
    @ManyToMany(mappedBy = "tags")
    private Set<Article> articles = new HashSet<>();

    // Getter 和 Setter 方法
    
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public Set<Article> getArticles() {
        return articles;
    }

    public void setArticles(Set<Article> articles) {
        this.articles = articles;
    }
}
