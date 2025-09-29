package com.sg.nusiss.gamevaultbackend.dto.friend.request;

import lombok.Data;

import java.io.Serializable;

/**
 * @ClassName SendFriendRequest
 * @Author HUANG ZHENJIA
 * @Date 2025/9/29
 * @Description
 */

@Data
public class SendFriendRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long userId;    // 发起人
    private Long friendId;  // 接收人
}
