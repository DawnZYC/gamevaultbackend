package com.sg.nusiss.gamevaultbackend.service.message;

import com.sg.nusiss.gamevaultbackend.common.ErrorCode;
import com.sg.nusiss.gamevaultbackend.dto.message.request.SendPrivateMessageRequest;
import com.sg.nusiss.gamevaultbackend.dto.message.response.MessageResponse;
import com.sg.nusiss.gamevaultbackend.entity.auth.User;
import com.sg.nusiss.gamevaultbackend.entity.message.Message;
import com.sg.nusiss.gamevaultbackend.exception.BusinessException;
import com.sg.nusiss.gamevaultbackend.repository.auth.UserRepository;
import com.sg.nusiss.gamevaultbackend.repository.friend.FriendshipRepository;
import com.sg.nusiss.gamevaultbackend.repository.message.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class PrivateMessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final FriendshipRepository friendshipRepository;

    /**
     * 发送私聊消息
     */
    @Transactional
    public MessageResponse sendPrivateMessage(SendPrivateMessageRequest request, Long senderId) {
        // 1. 验证接收者存在
        User receiver = userRepository.findById(request.getReceiverId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ERROR, "接收者不存在"));

        // 2. 验证是否是好友关系
        friendshipRepository.findByUserIdAndFriendIdAndIsActive(senderId, request.getReceiverId(), true)
                .orElseThrow(() -> new BusinessException(ErrorCode.NO_AUTH_ERROR, "只能给好友发送消息"));

        // 3. 验证消息内容（文件消息可以没有文本内容）
        if ("text".equals(request.getMessageType()) &&
                (request.getContent() == null || request.getContent().trim().isEmpty())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "消息内容不能为空");
        }

        // 构建消息实体
        Message.MessageBuilder messageBuilder = Message.builder()
                .senderId(senderId)
                .receiverId(request.getReceiverId())
                .content(request.getContent() != null ? request.getContent().trim() : "")
                .messageType(request.getMessageType() != null ? request.getMessageType() : "text")
                .chatType("private")
                .createdAt(LocalDateTime.now())
                .isDeleted(false);

        // 如果是文件消息，添加文件字段
        if ("file".equals(request.getMessageType()) && request.getFileId() != null) {
            messageBuilder
                    .fileId(request.getFileId())
                    .fileName(request.getFileName())
                    .fileSize(request.getFileSize())
                    .fileType(request.getFileType())
                    .fileExt(request.getFileExt())
                    .accessUrl(request.getAccessUrl())
                    .thumbnailUrl(request.getThumbnailUrl());

            log.info("保存私聊文件消息 - fileId: {}, fileName: {}", request.getFileId(), request.getFileName());
        }

        Message message = messageRepository.save(messageBuilder.build());

        log.info("私聊消息发送成功 - 发送者: {}, 接收者: {}, 消息ID: {}, 类型: {}",
                senderId, request.getReceiverId(), message.getId(), message.getMessageType());

        // 5. 转换为响应对象
        return convertToResponse(message);
    }

    /**
     * 获取私聊历史消息
     */
    public List<MessageResponse> getPrivateMessages(Long userId, Long friendId, int page, int size) {
        // 验证是好友关系
        friendshipRepository.findByUserIdAndFriendIdAndIsActive(userId, friendId, true)
                .orElseThrow(() -> new BusinessException(ErrorCode.NO_AUTH_ERROR, "只能查看好友的聊天记录"));

        Pageable pageable = PageRequest.of(page, size);
        Page<Message> messagePage = messageRepository.findPrivateMessages(userId, friendId, pageable);

        List<MessageResponse> messages = messagePage.getContent().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());

        // 反转顺序（从旧到新）
        java.util.Collections.reverse(messages);

        log.info("获取私聊历史 - 用户: {}, 好友: {}, 数量: {}", userId, friendId, messages.size());
        return messages;
    }

    /**
     * 转换为响应对象（包含文件附件）
     */
    private MessageResponse convertToResponse(Message message) {
        User sender = userRepository.findById(message.getSenderId())
                .orElse(null);

        MessageResponse.MessageResponseBuilder responseBuilder = MessageResponse.builder()
                .id(message.getId())
                .conversationId(message.getConversationId())
                .senderId(message.getSenderId())
                .receiverId(message.getReceiverId())
                .senderUsername(sender != null ? sender.getUsername() : "未知用户")
                .senderEmail(sender != null ? sender.getEmail() : "")
                .content(message.getContent())
                .messageType(message.getMessageType())
                .chatType("private")
                .createdAt(message.getCreatedAt());

        // 如果是文件消息，添加附件信息
        if ("file".equals(message.getMessageType()) && message.getFileId() != null) {
            MessageResponse.FileAttachment attachment = MessageResponse.FileAttachment.builder()
                    .fileId(message.getFileId())
                    .fileName(message.getFileName())
                    .fileSize(message.getFileSize())
                    .fileType(message.getFileType())
                    .fileExt(message.getFileExt())
                    .accessUrl(message.getAccessUrl())
                    .thumbnailUrl(message.getThumbnailUrl())
                    .build();

            responseBuilder.attachment(attachment);
        }

        return responseBuilder.build();
    }
}