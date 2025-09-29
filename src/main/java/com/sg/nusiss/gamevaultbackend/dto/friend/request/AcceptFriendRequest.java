package com.sg.nusiss.gamevaultbackend.dto.friend.request;

import lombok.Data;

import java.io.Serializable;

/**
 * @ClassName AcceptFriendRequest
 * @Author HUANG ZHENJIA
 * @Date 2025/9/29
 * @Description
 */
@Data
public class AcceptFriendRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long userId;    // 接收人
    private Long friendId;  // 发起人
}
