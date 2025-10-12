package com.sg.nusiss.gamevaultbackend.dto.friend.response;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * @ClassName UserSearchResponse
 * @Author HUANG ZHENJIA
 * @Date 2025/10/4
 * @Description
 */
// 用户搜索结果
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserSearchResponse {
    private Long userId;
    private String username;
    private String email;
    private Boolean isFriend;      // 是否已是好友
    private Boolean hasPending;    // 是否有待处理的请求
}