package com.sg.nusiss.gamevaultbackend.dto.settings;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class ChangeEmailReq {
    @NotBlank(message = "Password cannot be empty")
    private String password;
    
    @NotBlank(message = "New email cannot be empty")
    @Email(message = "Invalid email format")
    private String newEmail;

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNewEmail() {
        return newEmail;
    }

    public void setNewEmail(String newEmail) {
        this.newEmail = newEmail;
    }
}
