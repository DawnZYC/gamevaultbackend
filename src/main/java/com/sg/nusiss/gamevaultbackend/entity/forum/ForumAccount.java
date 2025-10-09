package com.sg.nusiss.gamevaultbackend.entity.forum;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 论坛账户实体类 - 专门用于认证
 * 对应数据库 accounts 表
 */

public class ForumAccount {
    private Long accountId;
    private String username;
    private String password;  // 明文密码
    private Long userId;      // 关联到users表的user_id
    private LocalDateTime createdDate;

    // 默认构造函数
    public ForumAccount() {}

    // 构造函数
    public ForumAccount(String username, String password) {
        this.username = username;
        this.password = password;
        this.createdDate = LocalDateTime.now();
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }
}

