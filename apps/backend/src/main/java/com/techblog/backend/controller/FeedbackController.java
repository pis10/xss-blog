package com.techblog.backend.controller;

import com.techblog.backend.dto.FeedbackRequest;
import com.techblog.backend.service.FeedbackService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 反馈控制器
 * 提供用户反馈提交功能（公开访问，无需登录）
 */
@RestController
@RequestMapping("/api")
public class FeedbackController {
    
    // 反馈服务，处理反馈的业务逻辑
    private final FeedbackService feedbackService;
    
    /**
     * 构造函数注入依赖
     */
    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }
    
    /**
     * 提交反馈接口
     * 
     * @param request 反馈请求（包含 email, content）
     * @return 提交成功消息
     */
    @PostMapping("/feedback")
    public ResponseEntity<Map<String, Object>> submitFeedback(@Valid @RequestBody FeedbackRequest request) {
        feedbackService.submitFeedback(request);
        return ResponseEntity.ok(Map.of("ok", true, "message", "感谢您的反馈！"));
    }
}
