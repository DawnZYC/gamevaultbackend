package com.sg.nusiss.gamevaultbackend.dto.forum;

import lombok.Getter;
import lombok.Setter;
import com.sg.nusiss.gamevaultbackend.entity.forum.ForumContent;
import com.sg.nusiss.gamevaultbackend.entity.forum.ForumUser;
import java.time.LocalDateTime;

/**
 * 帖子响应数据传输对象
 * 用于向前端返回帖子信息（包含统计数据和作者信息）
 */

public class PostResponseDTO {

    private Long contentId;
    private String title;
    private String body;
    private String bodyPlain;
    private Long authorId;
    private String authorName;
    private String authorNickname;
    private String authorAvatar;
    private Integer viewCount;
    private Integer likeCount;
    private Integer replyCount;
    private Boolean isLiked;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;

    // 默认构造函数
    public PostResponseDTO() {}

    // 从 Content 实体创建 DTO
    public static PostResponseDTO fromContent(ForumContent content) {
        PostResponseDTO dto = new PostResponseDTO();
        dto.contentId = content.getContentId();
        dto.title = content.getTitle();
        dto.body = content.getBody();
        dto.bodyPlain = content.getBodyPlain();
        dto.authorId = content.getAuthorId();
        dto.createdDate = content.getCreatedDate();
        dto.updatedDate = content.getUpdatedDate();
        // 统计数据需要额外设置
        dto.viewCount = content.getViewCount();
        dto.likeCount = content.getLikeCount();
        dto.replyCount = content.getReplyCount();
        return dto;
    }

    // 从 Content 和 User 创建完整的 DTO
    public static PostResponseDTO fromContentAndUser(ForumContent content, ForumUser author) {
        PostResponseDTO dto = fromContent(content);
        if (author != null) {
            dto.authorName = author.getUsername();
            dto.authorNickname = author.getNickname();
            dto.authorAvatar = author.getAvatarUrl();
            dto.isLiked = content.getIsLikedByCurrentUser();
        }
        return dto;
    }

    public Long getContentId() {
        return contentId;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public String getBodyPlain() {
        return bodyPlain;
    }

    public Long getAuthorId() {
        return authorId;
    }

    public String getAuthorName() {
        return authorName;
    }

    public String getAuthorNickname() {
        return authorNickname;
    }

    public String getAuthorAvatar() {
        return authorAvatar;
    }

    public Integer getViewCount() {
        return viewCount;
    }

    public Integer getLikeCount() {
        return likeCount;
    }

    public Integer getReplyCount() {
        return replyCount;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public LocalDateTime getUpdatedDate() {
        return updatedDate;
    }

    public void setContentId(Long contentId) {
        this.contentId = contentId;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public void setBodyPlain(String bodyPlain) {
        this.bodyPlain = bodyPlain;
    }

    public void setAuthorId(Long authorId) {
        this.authorId = authorId;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public void setAuthorNickname(String authorNickname) {
        this.authorNickname = authorNickname;
    }

    public void setAuthorAvatar(String authorAvatar) {
        this.authorAvatar = authorAvatar;
    }

    public void setViewCount(Integer viewCount) {
        this.viewCount = viewCount;
    }

    public void setLikeCount(Integer likeCount) {
        this.likeCount = likeCount;
    }

    public void setReplyCount(Integer replyCount) {
        this.replyCount = replyCount;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public void setUpdatedDate(LocalDateTime updatedDate) {
        this.updatedDate = updatedDate;
    }


    public Boolean getIsLiked() {
        return isLiked;
    }
    public void setIsLiked(Boolean isLiked) {
        this.isLiked = isLiked;
    }
    @Override
    public String toString() {
        return "PostResponseDTO{" +
                "contentId=" + contentId +
                ", title='" + title + '\'' +
                ", authorName='" + authorName + '\'' +
                ", authorAvatar='" + authorAvatar + '\'' +
                ", viewCount=" + viewCount +
                ", likeCount=" + likeCount +
                ", replyCount=" + replyCount +
                ", createdDate=" + createdDate +
                '}';
    }
}
