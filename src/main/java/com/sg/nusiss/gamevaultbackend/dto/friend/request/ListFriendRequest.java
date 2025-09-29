package com.sg.nusiss.gamevaultbackend.dto.friend.request;

import lombok.Data;

import java.io.Serializable;

/**
 * @ClassName ListFriendRequest
 * @Author HUANG ZHENJIA
 * @Date 2025/9/29
 * @Description
 */

@Data
public class ListFriendRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long userId;
}