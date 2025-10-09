package com.sg.nusiss.gamevaultbackend.entity.forum;

import lombok.Getter;
import lombok.Setter;

import java.beans.Transient;
import java.time.LocalDateTime;

/**
 * 通用内容实体类
 * 对应数据库 contents 表
 * 支持多种内容类型：post、reply、comment 等
 */

public class ForumContent {
    private Long contentId;
    private String contentType;  // 'post', 'reply', 'comment', 'review'
    private String title;
    private String body;         // 原始内容（HTML/Markdown）
    private String bodyPlain;    // 纯文本内容（用于搜索）
    private Long authorId;
    private Long parentId;       // 父内容ID，支持层级结构
    private String status;       // 'active', 'deleted', 'hidden', 'pending'
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
    private Integer likeCount;
    private Integer viewCount;
    private Integer replyCount;
    private Boolean isLikedByCurrentUser;
    // 默认构造函数
    public ForumContent() {}

    // 创建帖子的构造函数
    public ForumContent(String contentType, String title, String body, Long authorId) {
        this.contentType = contentType;
        this.title = title;
        this.body = body;
        this.bodyPlain = extractPlainText(body); // 简单提取纯文本
        this.authorId = authorId;
        this.status = "active";
        this.createdDate = LocalDateTime.now();
        this.updatedDate = LocalDateTime.now();
    }

    // 创建回复的构造函数
    public ForumContent(String contentType, String body, Long authorId, Long parentId) {
        this.contentType = contentType;
        this.body = body;
        this.bodyPlain = extractPlainText(body);
        this.authorId = authorId;
        this.parentId = parentId;
        this.status = "active";
        this.createdDate = LocalDateTime.now();
        this.updatedDate = LocalDateTime.now();
    }

    // 重写 setBody 方法，自动更新 bodyPlain
    public void setBody(String body) {
        this.body = body;
        this.bodyPlain = extractPlainText(body);
        this.updatedDate = LocalDateTime.now();
    }


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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public Integer getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(Integer likeCount) {
        this.likeCount = likeCount;
    }

    public Integer getViewCount() {
        return viewCount;
    }

    public void setViewCount(Integer viewCount) {
        this.viewCount = viewCount;
    }

    public Integer getReplyCount() {
        return replyCount;
    }

    public void setReplyCount(Integer replyCount) {
        this.replyCount = replyCount;
    }

    public Boolean getIsLikedByCurrentUser() {
        return isLikedByCurrentUser;
    }

    public void setIsLikedByCurrentUser(Boolean likedByCurrentUser) {
        isLikedByCurrentUser = likedByCurrentUser;
    }

    // 业务方法
    public boolean isPost() {
        return "post".equals(this.contentType);
    }

    public boolean isReply() {
        return "reply".equals(this.contentType);
    }

    public boolean isActive() {
        return "active".equals(this.status);
    }

    public void softDelete() {
        this.status = "deleted";
        this.updatedDate = LocalDateTime.now();
    }

    public void restore() {
        this.status = "active";
        this.updatedDate = LocalDateTime.now();
    }

    // 简单的纯文本提取方法
    private String extractPlainText(String htmlContent) {
        if (htmlContent == null) return "";
        // 简单的HTML标签移除（生产环境建议使用 Jsoup）
        return htmlContent.replaceAll("<[^>]+>", "").trim();
    }

    @Override
    public String toString() {
        return "ForumContent{" +
                "contentId=" + contentId +
                ", contentType='" + contentType + '\'' +
                ", title='" + title + '\'' +
                ", authorId=" + authorId +
                ", status='" + status + '\'' +
                ", createdDate=" + createdDate +
                '}';
    }
}

