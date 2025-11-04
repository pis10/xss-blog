package com.techblog.backend.service;

import com.techblog.backend.dto.UserDto;
import com.techblog.backend.entity.User;
import com.techblog.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户服务类
 * 处理用户相关的业务逻辑
 * 
 * 主要功能：
 * - 根据用户名查询用户信息
 * - 更新用户 Bio（个人简介）——XSS L2 攻击入口
 */
@Service
public class UserService {
    
    private final UserRepository userRepository;
    
    /**
     * 构造函数，注入依赖
     * @param userRepository 用户仓库
     */
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    /**
     * 根据用户名查询用户信息
     * 
     * @param username 用户名
     * @return 用户 DTO 对象
     * @throws RuntimeException 用户不存在时抛出
     */
    public UserDto getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found"));
        return mapToDto(user);
    }
    
    /**
     * 更新用户 Bio（XSS L2 入口点）
     * 
     * 安全注意：
     * - VULN 模式：直接存储用户提交的 HTML，可能包含 XSS 攻击代码
     * - SECURE 模式：前端使用 DOMPurify 过滤后再显示
     * 
     * @param username 用户名
     * @param bio 新的 Bio 内容（支持 HTML）
     * @return 更新后的用户 DTO
     * @throws RuntimeException 用户不存在时抛出
     */
    @Transactional
    public UserDto updateBio(String username, String bio) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found"));
        user.setBio(bio);
        userRepository.save(user);
        return mapToDto(user);
    }
    
    /**
     * 将用户实体转换为 DTO 对象
     * 
     * @param user 用户实体
     * @return 用户 DTO
     */
    private UserDto mapToDto(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole().name());
        dto.setAvatarUrl(user.getAvatarUrl());
        dto.setBannerUrl(user.getBannerUrl());
        dto.setBio(user.getBio());
        dto.setCreatedAt(user.getCreatedAt());
        return dto;
    }
}
