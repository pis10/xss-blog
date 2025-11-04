package com.techblog.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 评论请求数据传输对象
 * 用于用户提交文章评论
 */
public class CommentRequest {
    /**
     * 评论内容（必填，最长 2000 个字符）
     */
    @NotBlank(message = "评论内容不能为空")
    @Size(max = 2000, message = "评论内容不能超过 2000 个字符")
    private String content;

    // Getter 和 Setter 方法
    
    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
