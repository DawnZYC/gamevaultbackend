package com.sg.nusiss.gamevaultbackend.dto.message.request;

import lombok.Data;

/**
 * @ClassName SendMessageRequest
 * @Author HUANG ZHENJIA
 * @Date 2025/10/5
 * @Description
 */
// 发送消息请求
@Data
public class SendMessageRequest {
    private Long conversationId;
    private String content;
    private String messageType = "text";  // text, image, file, system
}
