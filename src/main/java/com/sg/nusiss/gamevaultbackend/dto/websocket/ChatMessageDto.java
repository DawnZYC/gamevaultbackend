package com.sg.nusiss.gamevaultbackend.dto.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @ClassName ChatMessageDto
 * @Author HUANG ZHENJIA
 * @Date 2025/10/5
 * @Description
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDto {
    private Long id;
    private Long conversationId;
    private Long senderId;
    private Long receiverId;
    private String senderUsername;
    private String senderEmail;
    private String content;
    private String messageType;
    private LocalDateTime timestamp;
}
