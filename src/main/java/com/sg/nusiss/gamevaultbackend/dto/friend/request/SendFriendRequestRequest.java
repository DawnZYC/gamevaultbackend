package com.sg.nusiss.gamevaultbackend.dto.friend.request;

import lombok.Data;

/**
 * @ClassName SendFriendRequestRequest
 * @Author HUANG ZHENJIA
 * @Date 2025/10/4
 * @Description
 */
// 发送好友请求
@Data
public class SendFriendRequestRequest {
    private Long toUserId;
    private String message;
}
