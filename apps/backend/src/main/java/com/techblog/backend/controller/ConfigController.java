package com.techblog.backend.controller;

import com.techblog.backend.config.XssProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 配置接口控制器
 * 提供系统配置查询和运行时模式切换功能
 * 
 * 主要接口：
 * - GET /api/config - 获取当前 XSS 模式
 * - POST /api/config/mode - 切换 XSS 模式（VULN/SECURE）
 * 
 * 注意：所有接口均为公开访问，无需登录
 */
@RestController
@RequestMapping("/api")
public class ConfigController {
    
    private final XssProperties xssProperties;
    
    /**
     * 构造函数注入依赖
     */
    public ConfigController(XssProperties xssProperties) {
        this.xssProperties = xssProperties;
    }
    
    /**
     * 获取当前 XSS 模式
     * 前端启动时调用此接口，同步后端模式状态
     * 
     * @return 包含 xssMode 字段的 JSON 对象
     */
    @GetMapping("/config")
    public Map<String, Object> getConfig() {
        return Map.of("xssMode", xssProperties.getMode());
    }
    
    /**
     * 切换 XSS 模式（运行时动态切换，无需重启）
     * 
     * 请求体示例：{"mode": "vuln"} 或 {"mode": "secure"}
     * 
     * @param request 包含 mode 字段的请求体
     * @return 成功时返回新模式和提示信息，失败时返回错误信息
     */
    @PostMapping("/config/mode")
    public ResponseEntity<Map<String, Object>> switchMode(@RequestBody Map<String, String> request) {
        String newMode = request.get("mode");
        // 验证模式参数（仅接受 vuln 或 secure）
        if ("vuln".equalsIgnoreCase(newMode) || "secure".equalsIgnoreCase(newMode)) {
            xssProperties.setMode(newMode);
            return ResponseEntity.ok(Map.of(
                "xssMode", xssProperties.getMode(),
                "message", "Mode switched to " + xssProperties.getMode()
            ));
        }
        // 参数非法时返回 400 错误
        return ResponseEntity.badRequest().body(Map.of(
            "error", "Invalid mode. Use 'vuln' or 'secure'"
        ));
    }
}
