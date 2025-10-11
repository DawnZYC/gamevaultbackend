package com.sg.nusiss.gamevaultbackend.dto.shopping;

import lombok.Data;

@Data
public class UserProfileDTO {
    private Long uid;
    private String username;
    private String email;
    private String avatar;
}
