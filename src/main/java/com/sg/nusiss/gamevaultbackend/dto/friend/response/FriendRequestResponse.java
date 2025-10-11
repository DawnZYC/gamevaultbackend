package com.sg.nusiss.gamevaultbackend.dto.friend.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @ClassName FriendRequestResponse
 * @Author HUANG ZHENJIA
 * @Date 2025/10/4
 * @Description
 */
// 好友请求响应
@Data
@AllArgsConstructor
@NoArgsConstructor
public class FriendRequestResponse {
    private Long id;
    private Long fromUserId;
    private String fromUsername;
    private String fromEmail;
    private Long toUserId;
    private String toUsername;
    private String toEmail;
    private String message;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime handledAt;
}
