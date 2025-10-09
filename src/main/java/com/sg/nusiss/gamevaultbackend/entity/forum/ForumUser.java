package com.sg.nusiss.gamevaultbackend.entity.forum;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 论坛用户实体类
 * 对应数据库 users 表
 */

public class ForumUser {
    // Getters and Setters
    private Long userId;
    private String username;
    private String nickname;        // 显示名称
    private String avatarUrl;       // 头像地址
    private String bio;             // 用户简介
    private String status;          // 用户状态：active, banned, inactive
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;

    // 默认构造函数
    public ForumUser() {}

    // 基础构造函数（用于外部认证系统）
    public ForumUser(Long userId, String username) {
        this.userId = userId;
        this.username = username;
        this.nickname = username;
        this.status = "active";
        this.createdDate = LocalDateTime.now();
        this.updatedDate = LocalDateTime.now();
    }

    // 完整构造函数
    public ForumUser(Long userId, String username, String nickname, String bio) {
        this.userId = userId;
        this.username = username;
        this.nickname = nickname != null ? nickname : username;
        this.bio = bio;
        this.status = "active";
        this.createdDate = LocalDateTime.now();
        this.updatedDate = LocalDateTime.now();
    }

    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getNickname() {
        return nickname;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public String getBio() {
        return bio;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public LocalDateTime getUpdatedDate() {
        return updatedDate;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public void setUpdatedDate(LocalDateTime updatedDate) {
        this.updatedDate = updatedDate;
    }

    // 业务方法
    public boolean isActive() {
        return "active".equals(this.status);
    }

    public void ban() {
        this.status = "banned";
        this.updatedDate = LocalDateTime.now();
    }

    public void activate() {
        this.status = "active";
        this.updatedDate = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "ForumUser{" +
                "userId=" + userId +
                ", username='" + username + '\'' +
                ", nickname='" + nickname + '\'' +
                ", status='" + status + '\'' +
                ", createdDate=" + createdDate +
                '}';
    }
}

