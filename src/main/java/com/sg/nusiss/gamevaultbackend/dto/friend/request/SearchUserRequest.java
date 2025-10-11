package com.sg.nusiss.gamevaultbackend.dto.friend.request;

import lombok.Data;

/**
 * @ClassName SearchUserRequest
 * @Author HUANG ZHENJIA
 * @Date 2025/10/4
 * @Description
 */

// 搜索用户请求
@Data
public class SearchUserRequest {
    private String keyword; // 邮箱或用户名
}
