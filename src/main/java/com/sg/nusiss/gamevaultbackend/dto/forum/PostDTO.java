package com.sg.nusiss.gamevaultbackend.dto.forum;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;


/**
 * 帖子数据传输对象
 * 用于接收前端创建/更新帖子的请求
 */

public class PostDTO {

    // Getters and Setters
    @NotBlank(message = "帖子标题不能为空")
    @Size(min = 1, max = 200, message = "帖子标题长度必须在1-200字符之间")
    private String title;

    @NotBlank(message = "帖子内容不能为空")
    @Size(min = 1, max = 10000, message = "帖子内容长度必须在1-10000字符之间")
    private String body;

    // 默认构造函数
    public PostDTO() {}

    // 构造函数
    public PostDTO(String title, String body) {
        this.title = title;
        this.body = body;
    }

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
