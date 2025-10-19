package com.sg.nusiss.gamevaultbackend.dto.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonInclude;

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
@JsonInclude(JsonInclude.Include.NON_NULL)
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

    private FileAttachment attachment;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FileAttachment {
        private String fileId;
        private String fileName;
        private Long fileSize;
        private String fileType;
        private String fileExt;
        private String accessUrl;
        private String thumbnailUrl;
    }
}
