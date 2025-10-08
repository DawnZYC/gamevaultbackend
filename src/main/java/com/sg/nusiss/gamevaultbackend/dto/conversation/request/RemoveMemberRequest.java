package com.sg.nusiss.gamevaultbackend.dto.conversation.request;

import lombok.Data;

/**
 * @ClassName RemoveMemberRequest
 * @Author HUANG ZHENJIA
 * @Date 2025/9/30
 * @Description
 */

@Data
public class RemoveMemberRequest {
    private Long conversationId;
    private Long ownerId;
    private Long userId;
}
