package com.sg.nusiss.gamevaultbackend.dto.shopping;

import lombok.Data;
import java.time.Instant;

@Data
public class UserDTO {
    private Long userId;
    private String username;
    private String email;
    private String profile;
    private Instant createdAt;
    private Instant updatedAt;
}
