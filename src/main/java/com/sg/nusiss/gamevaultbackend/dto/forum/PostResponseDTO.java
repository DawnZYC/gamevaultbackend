package com.sg.nusiss.gamevaultbackend.dto.forum;

import com.sg.nusiss.gamevaultbackend.entity.forum.Content;
import com.sg.nusiss.gamevaultbackend.entity.auth.User;

import java.time.LocalDateTime;

/**
 * 帖子响应DTO
 */
public class PostResponseDTO {
    
    private Long contentId;
    private String title;
    private String body;
    private Long authorId;
    private String authorUsername;
    private String authorEmail;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
    private Integer viewCount;
    private Integer likeCount;
    private String status;
    
    // 默认构造函数
    public PostResponseDTO() {}
    
    // 构造函数
    public PostResponseDTO(Long contentId, String title, String body, Long authorId, 
                          String authorUsername, String authorEmail, LocalDateTime createdDate, 
                          LocalDateTime updatedDate, Integer viewCount, Integer likeCount, String status) {
        this.contentId = contentId;
        this.title = title;
        this.body = body;
        this.authorId = authorId;
        this.authorUsername = authorUsername;
        this.authorEmail = authorEmail;
        this.createdDate = createdDate;
        this.updatedDate = updatedDate;
        this.viewCount = viewCount;
        this.likeCount = likeCount;
        this.status = status;
    }
    
    /**
     * 从Content和User实体创建PostResponseDTO
     */
    public static PostResponseDTO fromContentAndUser(Content content, User user) {
        PostResponseDTO dto = new PostResponseDTO();
        dto.setContentId(content.getContentId());
        dto.setTitle(content.getTitle());
        dto.setBody(content.getBody());
        dto.setAuthorId(content.getAuthorId());
        dto.setCreatedDate(content.getCreatedDate());
        dto.setUpdatedDate(content.getUpdatedDate());
        dto.setViewCount(0); // 默认值，实际值需要通过统计表查询
        dto.setLikeCount(0); // 默认值，实际值需要通过统计表查询
        dto.setStatus(content.getStatus());
        
        if (user != null) {
            dto.setAuthorUsername(user.getUsername());
            dto.setAuthorEmail(user.getEmail());
        } else {
            dto.setAuthorUsername("未知用户");
            dto.setAuthorEmail("");
        }
        
        return dto;
    }
    
    // Getters and Setters
    public Long getContentId() {
        return contentId;
    }
    
    public void setContentId(Long contentId) {
        this.contentId = contentId;
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
    
    public Long getAuthorId() {
        return authorId;
    }
    
    public void setAuthorId(Long authorId) {
        this.authorId = authorId;
    }
    
    public String getAuthorUsername() {
        return authorUsername;
    }
    
    public void setAuthorUsername(String authorUsername) {
        this.authorUsername = authorUsername;
    }
    
    public String getAuthorEmail() {
        return authorEmail;
    }
    
    public void setAuthorEmail(String authorEmail) {
        this.authorEmail = authorEmail;
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
    
    public Integer getViewCount() {
        return viewCount;
    }
    
    public void setViewCount(Integer viewCount) {
        this.viewCount = viewCount;
    }
    
    public Integer getLikeCount() {
        return likeCount;
    }
    
    public void setLikeCount(Integer likeCount) {
        this.likeCount = likeCount;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    @Override
    public String toString() {
        return "PostResponseDTO{" +
                "contentId=" + contentId +
                ", title='" + title + '\'' +
                ", authorId=" + authorId +
                ", authorUsername='" + authorUsername + '\'' +
                ", createdDate=" + createdDate +
                ", viewCount=" + viewCount +
                ", status='" + status + '\'' +
                '}';
    }
}
