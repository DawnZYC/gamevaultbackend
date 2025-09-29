package com.sg.nusiss.gamevaultbackend.dto.settings;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ChangePasswordReq {
    @NotBlank(message = "Current password cannot be empty")
    private String oldPassword;
    
    @NotBlank(message = "New password cannot be empty")
    @Size(min = 6, message = "New password must be at least 6 characters")
    private String newPassword;

    public String getOldPassword() {
        return oldPassword;
    }

    public void setOldPassword(String oldPassword) {
        this.oldPassword = oldPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
