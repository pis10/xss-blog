package com.techblog.backend.service;

import com.techblog.backend.dto.FeedbackDto;
import com.techblog.backend.dto.FeedbackRequest;
import com.techblog.backend.entity.Feedback;
import com.techblog.backend.repository.FeedbackRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 反馈服务
 * 负责用户反馈的提交和管理逻辑
 * 
 * 安全注意：
 * - 此服务涉及 XSS L3 盲 XSS 攻击场景
 */
@Service
public class FeedbackService {
    
    private final FeedbackRepository feedbackRepository;
    
    /**
     * 构造函数，注入依赖
     * @param feedbackRepository 反馈仓库
     */
    public FeedbackService(FeedbackRepository feedbackRepository) {
        this.feedbackRepository = feedbackRepository;
    }
    
    @Transactional
    public void submitFeedback(FeedbackRequest request) {
        Feedback feedback = new Feedback();
        feedback.setEmail(request.getEmail());
        feedback.setContentHtml(request.getContent());
        feedback.setStatus(Feedback.FeedbackStatus.NEW);
        feedbackRepository.save(feedback);
    }
    
    public Page<FeedbackDto> getAllFeedbacks(Pageable pageable) {
        return feedbackRepository.findAllByOrderByCreatedAtDesc(pageable)
            .map(this::mapToDto);
    }
    
    public FeedbackDto getFeedbackById(Long id) {
        Feedback feedback = feedbackRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Feedback not found"));
        return mapToDto(feedback);
    }
    
    @Transactional
    public void markAsRead(Long id) {
        Feedback feedback = feedbackRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Feedback not found"));
        feedback.setStatus(Feedback.FeedbackStatus.READ);
        feedbackRepository.save(feedback);
    }
    
    private FeedbackDto mapToDto(Feedback feedback) {
        FeedbackDto dto = new FeedbackDto();
        dto.setId(feedback.getId());
        dto.setEmail(feedback.getEmail());
        dto.setContentHtml(feedback.getContentHtml());
        dto.setStatus(feedback.getStatus().name());
        dto.setCreatedAt(feedback.getCreatedAt());
        return dto;
    }
}
