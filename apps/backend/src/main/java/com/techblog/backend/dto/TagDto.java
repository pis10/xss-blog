package com.techblog.backend.dto;

/**
 * 标签数据传输对象
 * 用于前后端数据交换
 */
public class TagDto {
    /**
     * 标签 ID
     */
    private Long id;
    
    /**
     * 标签名称
     */
    private String name;
    
    /**
     * 标签颜色（十六进制颜色值）
     */
    private String color;

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
}
