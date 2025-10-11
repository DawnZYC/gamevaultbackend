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

/**
 * @ClassName ChatWebSocketController
 * @Author HUANG ZHENJIA
 * @Date 2025/10/5
 * @Description
 */

@Controller
@Slf4j
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final MessageService messageService;
    private final PrivateMessageService privateMessageService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 处理客户端发送的群聊消息
     * 客户端发送到：/app/chat.sendMessage
     */
    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload SendMessageRequest request, Principal principal) {
        try {
            // 从 Principal 中获取用户 ID
            Long senderId = extractUserIdFromPrincipal(principal);

            if (senderId == null) {
                log.error("无法获取用户 ID");
                return;
            }

            log.info("收到 WebSocket 消息 - 群聊ID: {}, 发送者: {}, 内容: {}",
                    request.getConversationId(), senderId, request.getContent());

            // 保存消息到数据库和 Redis
            MessageResponse response = messageService.sendMessage(request, senderId);

            // 转换为 WebSocket DTO
            ChatMessageDto chatMessage = ChatMessageDto.builder()
                    .id(response.getId())
                    .conversationId(response.getConversationId())
                    .senderId(response.getSenderId())
                    .senderUsername(response.getSenderUsername())
                    .senderEmail(response.getSenderEmail())
                    .content(response.getContent())
                    .messageType(response.getMessageType())
                    .timestamp(response.getCreatedAt())
                    .build();

            // 广播消息
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

    /**
     * 处理私聊消息
     */
    @MessageMapping("/chat.sendPrivateMessage")
    public void sendPrivateMessage(@Payload SendPrivateMessageRequest request, Principal principal) {
        try {
            Long senderId = extractUserIdFromPrincipal(principal);

            if (senderId == null) {
                log.error("无法获取用户 ID");
                return;
            }

            log.info("收到私聊消息 - 发送者: {}, 接收者: {}", senderId, request.getReceiverId());

            MessageResponse response = privateMessageService.sendPrivateMessage(request, senderId);

            ChatMessageDto chatMessage = ChatMessageDto.builder()
                    .id(response.getId())
                    .senderId(response.getSenderId())
                    .receiverId(response.getReceiverId())
                    .senderUsername(response.getSenderUsername())
                    .senderEmail(response.getSenderEmail())
                    .content(response.getContent())
                    .messageType(response.getMessageType())
                    .timestamp(response.getCreatedAt())
                    .build();

            // 改用 topic 方式，直接发送到特定路径
            log.info("发送私聊消息到接收者 topic");
            messagingTemplate.convertAndSend(
                    "/topic/private/" + request.getReceiverId(),
                    chatMessage
            );

            log.info("发送私聊消息到发送者 topic");
            messagingTemplate.convertAndSend(
                    "/topic/private/" + senderId,
                    chatMessage
            );

            log.info("私聊消息已发送");

        } catch (Exception e) {
            log.error("处理私聊消息失败", e);
        }
    }

    /**
     * 从 Principal 中提取用户 ID
     */
    private Long extractUserIdFromPrincipal(Principal principal) {
        if (principal instanceof JwtAuthenticationToken) {
            JwtAuthenticationToken jwtAuth = (JwtAuthenticationToken) principal;
            Jwt jwt = jwtAuth.getToken();
            return jwt.getClaim("uid");  // 从 JWT 的 uid claim 获取
        }
        return null;
    }
}
