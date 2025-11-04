package com.techblog.backend.controller;

import com.techblog.backend.config.XssProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;

import java.util.List;
import java.util.Map;

/**
 * 搜索接口控制器
 * XSS 演示场景 L0/L1 的入口点：反射型 XSS 攻击
 * 
 * 演示场景：
 * - L0：基础反射型 XSS PoC（如 <script>alert('XSS')</script>）
 * - L1：窃取 JWT 凭证（localStorage.getItem('accessToken')）
 * 
 * 双态实现：
 * - VULN 模式：直接拼接用户输入，不进行 HTML 转义
 * - SECURE 模式：使用 HtmlUtils.htmlEscape() 转义特殊字符
 */
@RestController
@RequestMapping("/api")
public class SearchController {
    
    private final XssProperties xssProperties;
    
    /**
     * 构造函数注入依赖
     */
    public SearchController(XssProperties xssProperties) {
        this.xssProperties = xssProperties;
    }
    
    /**
     * 搜索文章接口（XSS 演示入口点）
     * 
     * @param q 搜索关键词（潜在的 XSS 攻击向量）
     * @return 包含搜索结果提示和文章列表的 JSON 对象
     */
    @GetMapping("/search")
    public Map<String, Object> search(@RequestParam String q) {
        String message;
        
        if (xssProperties.isVuln()) {
            // VULN 模式：直接拼接，不转义（XSS L0/L1 入口点）
            message = "为您找到『" + q + "』的结果…";
        } else {
            // SECURE 模式：HTML 转义用户输入，防止 XSS
            message = "为您找到『" + HtmlUtils.htmlEscape(q) + "』的结果…";
        }
        
        // 演示目的，返回空结果列表
        // 真实应用中应该查询数据库返回匹配的文章
        return Map.of(
            "message", message,
            "items", List.of()
        );
    }
}
