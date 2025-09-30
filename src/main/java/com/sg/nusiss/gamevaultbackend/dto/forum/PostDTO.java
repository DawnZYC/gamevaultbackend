package com.sg.nusiss.gamevaultbackend.dto.forum;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 创建帖子的请求DTO
 */
public class PostDTO {
    
    @NotBlank(message = "标题不能为空")
    @Size(max = 255, message = "标题长度不能超过255个字符")
    private String title;
    
    @NotBlank(message = "内容不能为空")
    private String body;
    
    // 默认构造函数
    public PostDTO() {}
    
    // 构造函数
    public PostDTO(String title, String body) {
        this.title = title;
        this.body = body;
    }
    
    // Getters and Setters
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getBody() {
        return body;
    }
    
    public void setBody(String body) {
        this.body = body;
    }
    
    @Override
    public String toString() {
        return "PostDTO{" +
                "title='" + title + '\'' +
                ", body='" + body + '\'' +
                '}';
    }
}
