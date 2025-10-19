package com.sg.nusiss.gamevaultbackend.dto.message.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @ClassName MessageResponse
 * @Author HUANG ZHENJIA
 * @Date 2025/9/30
 * @Description
 */
/**
 * 消息响应 DTO
 */
// 消息响应
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponse {
    private Long id;
    private Long conversationId;
    private Long senderId;
    private Long receiverId;
    private String senderUsername;
    private String senderEmail;
    private String content;
    private String chatType;
    private String messageType;
    private LocalDateTime createdAt;
    private FileAttachment attachment;

    /**
     * 文件附件内部类
     */
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