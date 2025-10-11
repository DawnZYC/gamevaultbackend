package com.sg.nusiss.gamevaultbackend.dto.conversation.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @ClassName MemberResponse
 * @Author HUANG ZHENJIA
 * @Date 2025/10/4
 * @Description
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MemberResponse {
    private Long userId;
    private String username;
    private String email;
    private String role;  // owner | member
    private LocalDateTime joinedAt;
}
