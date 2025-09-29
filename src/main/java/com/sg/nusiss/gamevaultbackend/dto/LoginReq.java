package com.sg.nusiss.gamevaultbackend.dto;

import jakarta.validation.constraints.NotBlank;

public class LoginReq {
    @NotBlank public String username;
    @NotBlank public String password;
}
