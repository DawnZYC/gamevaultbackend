package com.sg.nusiss.gamevaultbackend.dto.auth;

import jakarta.validation.constraints.*;

public class RegisterReq {
    @Email @NotBlank public String email;
    @NotBlank public String username;
    @Size(min = 6) public String password;
}
