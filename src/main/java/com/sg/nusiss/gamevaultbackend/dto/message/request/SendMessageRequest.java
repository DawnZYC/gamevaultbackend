package com.sg.nusiss.gamevaultbackend.dto.message.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import lombok.Data;

/**
 * @ClassName SendMessageRequest
 * @Author HUANG ZHENJIA
 * @Date 2025/10/5
 * @Description
 */
// 发送消息请求
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendMessageRequest {
    private Long conversationId;
    private String content;
    private String messageType; // "text" | "file"

    private String fileId;
    private String fileName;
    private Long fileSize;
    private String fileType;
    private String fileExt;
    private String accessUrl;
    private String thumbnailUrl;
}
