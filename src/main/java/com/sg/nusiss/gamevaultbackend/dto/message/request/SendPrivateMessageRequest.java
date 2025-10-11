package com.sg.nusiss.gamevaultbackend.dto.message.request;

import lombok.Data;

/**
 * @ClassName SendPrivateMessageRequest
 * @Author HUANG ZHENJIA
 * @Date 2025/10/6
 * @Description
 */
@Data
public class SendPrivateMessageRequest {
    private Long receiverId;
    private String content;
    private String messageType = "text";
}
