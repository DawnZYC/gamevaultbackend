package com.sg.nusiss.gamevaultbackend.controller.websocket;

import com.sg.nusiss.gamevaultbackend.dto.message.request.SendMessageRequest;
import com.sg.nusiss.gamevaultbackend.dto.message.request.SendPrivateMessageRequest;
import com.sg.nusiss.gamevaultbackend.dto.message.response.MessageResponse;
import com.sg.nusiss.gamevaultbackend.dto.websocket.ChatMessageDto;
import com.sg.nusiss.gamevaultbackend.service.message.MessageService;
import com.sg.nusiss.gamevaultbackend.service.message.PrivateMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.security.Principal;

@Controller
@Slf4j
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final MessageService messageService;
    private final PrivateMessageService privateMessageService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload SendMessageRequest request, Principal principal) {
        try {
            Long senderId = extractUserIdFromPrincipal(principal);

            if (senderId == null) {
                log.error("无法获取用户 ID");
                return;
            }

            log.info("收到 WebSocket 消息 - 群聊ID: {}, 发送者: {}, 类型: {}",
                    request.getConversationId(), senderId, request.getMessageType());

            if ("file".equals(request.getMessageType())) {
                log.info("文件消息 - fileId: {}, fileName: {}", request.getFileId(), request.getFileName());
            }

            MessageResponse response = messageService.sendMessage(request, senderId);

            // 直接构建，类型匹配
            ChatMessageDto.ChatMessageDtoBuilder builder = ChatMessageDto.builder()
                    .id(response.getId())
                    .conversationId(response.getConversationId())
                    .senderId(response.getSenderId())
                    .senderUsername(response.getSenderUsername())
                    .senderEmail(response.getSenderEmail())
                    .content(response.getContent())
                    .messageType(response.getMessageType())
                    .timestamp(response.getCreatedAt()); // LocalDateTime -> LocalDateTime

            if (response.getAttachment() != null) {
                builder.attachment(
                        ChatMessageDto.FileAttachment.builder()
                                .fileId(response.getAttachment().getFileId())
                                .fileName(response.getAttachment().getFileName())
                                .fileSize(response.getAttachment().getFileSize())
                                .fileType(response.getAttachment().getFileType())
                                .fileExt(response.getAttachment().getFileExt())
                                .accessUrl(response.getAttachment().getAccessUrl())
                                .thumbnailUrl(response.getAttachment().getThumbnailUrl())
                                .build()
                );
            }

            ChatMessageDto chatMessage = builder.build();

            messagingTemplate.convertAndSend(
                    "/topic/chat/" + request.getConversationId(),
                    chatMessage
            );

            log.info("消息已广播 - 群聊ID: {}, 消息ID: {}",
                    request.getConversationId(), response.getId());

        } catch (Exception e) {
            log.error("处理 WebSocket 消息失败", e);
        }
    }

    @MessageMapping("/chat.sendPrivateMessage")
    public void sendPrivateMessage(@Payload SendPrivateMessageRequest request, Principal principal) {
        try {
            Long senderId = extractUserIdFromPrincipal(principal);

            if (senderId == null) {
                log.error("无法获取用户 ID");
                return;
            }

            log.info("收到私聊消息 - 发送者: {}, 接收者: {}, 类型: {}",
                    senderId, request.getReceiverId(), request.getMessageType());

            if ("file".equals(request.getMessageType())) {
                log.info("私聊文件消息 - fileId: {}, fileName: {}", request.getFileId(), request.getFileName());
            }

            MessageResponse response = privateMessageService.sendPrivateMessage(request, senderId);

            // 直接构建，类型匹配
            ChatMessageDto.ChatMessageDtoBuilder builder = ChatMessageDto.builder()
                    .id(response.getId())
                    .senderId(response.getSenderId())
                    .receiverId(response.getReceiverId())
                    .senderUsername(response.getSenderUsername())
                    .senderEmail(response.getSenderEmail())
                    .content(response.getContent())
                    .messageType(response.getMessageType())
                    .timestamp(response.getCreatedAt()); // LocalDateTime -> LocalDateTime

            if (response.getAttachment() != null) {
                builder.attachment(
                        ChatMessageDto.FileAttachment.builder()
                                .fileId(response.getAttachment().getFileId())
                                .fileName(response.getAttachment().getFileName())
                                .fileSize(response.getAttachment().getFileSize())
                                .fileType(response.getAttachment().getFileType())
                                .fileExt(response.getAttachment().getFileExt())
                                .accessUrl(response.getAttachment().getAccessUrl())
                                .thumbnailUrl(response.getAttachment().getThumbnailUrl())
                                .build()
                );
            }

            ChatMessageDto chatMessage = builder.build();

            messagingTemplate.convertAndSend(
                    "/topic/private/" + request.getReceiverId(),
                    chatMessage
            );

            messagingTemplate.convertAndSend(
                    "/topic/private/" + senderId,
                    chatMessage
            );

            log.info("私聊消息已发送");

        } catch (Exception e) {
            log.error("处理私聊消息失败", e);
        }
    }

    private Long extractUserIdFromPrincipal(Principal principal) {
        if (principal instanceof JwtAuthenticationToken) {
            JwtAuthenticationToken jwtAuth = (JwtAuthenticationToken) principal;
            Jwt jwt = jwtAuth.getToken();
            return jwt.getClaim("uid");
        }
        return null;
    }
}