package com.sg.nusiss.gamevaultbackend.dto.conversation.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @ClassName ConversationListResponse
 * @Author HUANG ZHENJIA
 * @Date 2025/10/4
 * @Description
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConversationListResponse {
    private Long id;
    private String title;
    private Long ownerId;
    private LocalDateTime createdAt;
    private String status;

    // 可选字段
    private String lastMessage;
    private LocalDateTime lastMessageTime;
    private Integer unreadCount;
}
