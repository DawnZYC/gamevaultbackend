package com.sg.nusiss.gamevaultbackend.dto.message.request;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @ClassName SendPrivateMessageRequest
 * @Author HUANG ZHENJIA
 * @Date 2025/10/6
 * @Description
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendPrivateMessageRequest {
    private Long receiverId;
    private String content;
    private String messageType = "text";

    private String fileId;
    private String fileName;
    private Long fileSize;
    private String fileType;
    private String fileExt;
    private String accessUrl;
    private String thumbnailUrl;
}
