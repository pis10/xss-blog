package com.techblog.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * XSS 模式配置类
 * 支持运行时动态切换 VULN（漏洞）和 SECURE（安全）两种模式
 * 
 * - VULN 模式：不过滤用户输入，用于演示 XSS 攻击
 * - SECURE 模式：启用 HTML 转义和 DOMPurify 过滤，防御 XSS 攻击
 * 
 * 配置来源：application.yml 中的 xss.mode
 * 切换接口：POST /api/config/mode
 */
@Configuration
@ConfigurationProperties(prefix = "xss")
public class XssProperties {
    // 使用 volatile 保证多线程环境下的可见性，支持运行时切换
    private volatile String mode = "vuln";
    
    /**
     * 获取当前 XSS 模式
     * @return 模式名称（vuln 或 secure）
     */
    public String getMode() {
        return mode;
    }
    
    /**
     * 判断当前是否为 VULN 模式
     * @return true 表示漏洞模式，false 表示非漏洞模式
     */
    public boolean isVuln() {
        return "vuln".equalsIgnoreCase(mode);
    }
    
    /**
     * 判断当前是否为 SECURE 模式
     * @return true 表示安全模式，false 表示非安全模式
     */
    public boolean isSecure() {
        return "secure".equalsIgnoreCase(mode);
    }
    
    /**
     * 设置 XSS 模式（仅接受 vuln 或 secure）
     * @param mode 模式名称（vuln/secure，不区分大小写）
     */
    public void setMode(String mode) {
        if ("vuln".equalsIgnoreCase(mode) || "secure".equalsIgnoreCase(mode)) {
            this.mode = mode.toLowerCase();
        }
    }
}
