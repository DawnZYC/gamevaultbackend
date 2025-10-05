package com.sg.nusiss.gamevaultbackend.dto.friend.request;

import lombok.Data;

/**
 * @ClassName HandleFriendRequestRequest
 * @Author HUANG ZHENJIA
 * @Date 2025/10/4
 * @Description
 */
// 处理好友请求
@Data
public class HandleFriendRequestRequest {
    private Long requestId;
    private Boolean accept; // true=接受, false=拒绝
}