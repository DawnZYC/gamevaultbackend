package com.sg.nusiss.gamevaultbackend.entity.forum;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 论坛内容实体类
 * 对应数据库 contents 表（支持帖子、回复、评论等）
 */
@Entity
@Table(name = "contents")
public class Content {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "content_id")
    private Long contentId;
    
    @Column(name = "content_type", nullable = false, length = 20)
    private String contentType = "post";
    
    @Column(name = "title", length = 200)
    private String title;
    
    @Column(name = "body", columnDefinition = "TEXT", nullable = false)
    private String body;
    
    @Column(name = "body_plain", columnDefinition = "TEXT", nullable = false)
    private String bodyPlain;
    
    @Column(name = "author_id", nullable = false)
    private Long authorId;
    
    @Column(name = "parent_id")
    private Long parentId;
    
    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;
    
    @Column(name = "updated_date")
    private LocalDateTime updatedDate;
    
    @Column(name = "status", length = 20)
    private String status = "active";
    
    // 默认构造函数
    public Content() {}
    
    // 构造函数
    public Content(String title, String body, String bodyPlain, Long authorId) {
        this.contentType = "post";
        this.title = title;
        this.body = body;
        this.bodyPlain = bodyPlain;
        this.authorId = authorId;
        this.createdDate = LocalDateTime.now();
        this.updatedDate = LocalDateTime.now();
        this.status = "active";
    }
    
    // Getters and Setters
    public Long getContentId() {
        return contentId;
    }
    
    public void setContentId(Long contentId) {
        this.contentId = contentId;
    }
    
    public String getContentType() {
        return contentType;
    }
    
    public void setContentType(String contentType) {
        this.contentType = contentType;
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
    
    public String getBodyPlain() {
        return bodyPlain;
    }
    
    public void setBodyPlain(String bodyPlain) {
        this.bodyPlain = bodyPlain;
    }
    
    public Long getAuthorId() {
        return authorId;
    }
    
    public void setAuthorId(Long authorId) {
        this.authorId = authorId;
    }
    
    public Long getParentId() {
        return parentId;
    }
    
    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }
    
    public LocalDateTime getCreatedDate() {
        return createdDate;
    }
    
    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }
    
    public LocalDateTime getUpdatedDate() {
        return updatedDate;
    }
    
    public void setUpdatedDate(LocalDateTime updatedDate) {
        this.updatedDate = updatedDate;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    // 业务方法
    public boolean isActive() {
        return "active".equals(this.status);
    }
    
    public void delete() {
        this.status = "deleted";
        this.updatedDate = LocalDateTime.now();
    }
    
    public boolean isPost() {
        return "post".equals(this.contentType);
    }
    
    public boolean isReply() {
        return "reply".equals(this.contentType);
    }
    
    @Override
    public String toString() {
        return "Content{" +
                "contentId=" + contentId +
                ", contentType='" + contentType + '\'' +
                ", title='" + title + '\'' +
                ", authorId=" + authorId +
                ", createdDate=" + createdDate +
                ", status='" + status + '\'' +
                '}';
    }
}
