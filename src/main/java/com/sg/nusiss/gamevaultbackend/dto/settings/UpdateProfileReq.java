package com.sg.nusiss.gamevaultbackend.dto.settings;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public class UpdateProfileReq {
    
    @Size(max = 100, message = "昵称长度不能超过100个字符")
    private String nickname;
    
    @Email(message = "邮箱格式不正确")
    @Size(max = 255, message = "邮箱长度不能超过255个字符")
    private String email;
    
    @Size(max = 1000, message = "个人简介长度不能超过1000个字符")
    private String bio;
    
    private String avatarUrl;

    // 构造函数
    public UpdateProfileReq() {}

    // Getters and Setters
    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }
}
