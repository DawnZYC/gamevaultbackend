package com.sg.nusiss.gamevaultbackend.dto.conversation.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @ClassName DissolveRequest
 * @Author HUANG ZHENJIA
 * @Date 2025/10/4
 * @Description
 */
@Data
public class DissolveRequest {
    @NotNull(message = "conversationId cannot be blanked")
    private Long conversationId;
}
