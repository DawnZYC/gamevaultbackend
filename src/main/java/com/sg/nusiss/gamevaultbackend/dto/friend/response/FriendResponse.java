package com.sg.nusiss.gamevaultbackend.dto.friend.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @ClassName FriendResponse
 * @Author HUANG ZHENJIA
 * @Date 2025/10/4
 * @Description
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class FriendResponse {
    private Long uid;
    private String username;
    private String email;
    private String remark;
    private LocalDateTime friendSince; // 成为好友的时间
}
