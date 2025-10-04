package com.sg.nusiss.gamevaultbackend.dto.conversation.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @ClassName CreateConversationRequest
 * @Author HUANG ZHENJIA
 * @Date 2025/9/30
 * @Description
 */
@Data
public class CreateConversationRequest {
    @NotBlank(message = "Conversation title cannot be blanked")
    private String title;
}
