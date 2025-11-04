package com.techblog.backend.controller;

import com.techblog.backend.dto.FeedbackDto;
import com.techblog.backend.service.FeedbackService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 管理员控制器
 * 提供管理员专用的API接口，包括反馈管理和仪表板数据展示
 * 所有接口都需要ADMIN角色才能访问
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    
    // 通过构造函数注入FeedbackService服务
    private final FeedbackService feedbackService;
    
    /**
     * 构造函数注入依赖
     */
    public AdminController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }
    
    /**
     * 获取所有用户反馈列表（分页）
     * 
     * @param page 页码，从0开始，默认为0
     * @param size 每页大小，默认为20
     * @return 分页的反馈数据
     */
    @GetMapping("/feedbacks")
    public ResponseEntity<Page<FeedbackDto>> getAllFeedbacks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(feedbackService.getAllFeedbacks(pageable));
    }
    
    /**
     * 根据ID获取特定反馈详情，并标记为已读
     * 
     * @param id 反馈ID
     * @return 反馈详情
     */
    @GetMapping("/feedbacks/{id}")
    public ResponseEntity<FeedbackDto> getFeedbackById(@PathVariable Long id) {
        FeedbackDto feedback = feedbackService.getFeedbackById(id);
        // 获取反馈后自动标记为已读
        feedbackService.markAsRead(id);
        return ResponseEntity.ok(feedback);
    }
    
    /**
     * 获取管理仪表板数据
     * 
     * @return 包含各种统计数据的Map对象
     */
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboard() {
        // 简单的演示数据
        return ResponseEntity.ok(Map.of(
            "todayVisits", 1247,      // 今日访问量
            "newUsers", 23,           // 新增用户数
            "totalArticles", 3,       // 总文章数
            "pendingFeedbacks", 2     // 待处理反馈数
        ));
    }
}