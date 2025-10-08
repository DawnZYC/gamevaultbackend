package com.sg.nusiss.gamevaultbackend.service.message;


import com.sg.nusiss.gamevaultbackend.common.ErrorCode;

import com.sg.nusiss.gamevaultbackend.dto.message.request.SendMessageRequest;
import com.sg.nusiss.gamevaultbackend.dto.message.response.MessageResponse;
import com.sg.nusiss.gamevaultbackend.entity.auth.User;
import com.sg.nusiss.gamevaultbackend.entity.conversation.Conversation;
import com.sg.nusiss.gamevaultbackend.entity.message.Message;
import com.sg.nusiss.gamevaultbackend.exception.BusinessException;
import com.sg.nusiss.gamevaultbackend.repository.auth.UserRepository;
import com.sg.nusiss.gamevaultbackend.repository.conversation.ConversationRepository;
import com.sg.nusiss.gamevaultbackend.repository.conversation.MemberRepository;
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

/**
 * @ClassName MessageService
 * @Author HUANG ZHENJIA
 * @Date 2025/9/30
 * @Description
 */

@Service
@Slf4j
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final MemberRepository memberRepository;
    private final UserRepository userRepository;
    private final MessageCacheService messageCacheService;

    /**
     * 发送消息（同步到 MySQL + Redis）
     */
    @Transactional
    public MessageResponse sendMessage(SendMessageRequest request, Long senderId) {
        // 1. 验证群聊存在
        Conversation conversation = conversationRepository.findById(request.getConversationId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ERROR, "群聊不存在"));

        // 2. 检查群聊是否已解散
        if ("dissolved".equals(conversation.getStatus())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "群聊已解散，无法发送消息");
        }

        // 3. 验证发送者是群成员
        memberRepository.findByConversationIdAndUserIdAndIsActive(
                request.getConversationId(), senderId, true
        ).orElseThrow(() -> new BusinessException(ErrorCode.NO_AUTH_ERROR, "您不在该群聊中"));

        // 4. 验证消息内容
        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "消息内容不能为空");
        }

        // 保存到 MySQL
        Message message = Message.builder()
                .conversationId(request.getConversationId())
                .senderId(senderId)
                .content(request.getContent().trim())
                .messageType(request.getMessageType())
                .createdAt(LocalDateTime.now())
                .isDeleted(false)
                .build();

        message = messageRepository.save(message);

        // 转换为响应对象
        MessageResponse response = convertToResponse(message);

        // 缓存到 Redis
        messageCacheService.cacheMessage(response);

        log.info("消息已发送并同步 - 群聊ID: {}, 发送者: {}, 消息ID: {}",
                request.getConversationId(), senderId, message.getId());

        return response;
    }

    /**
     * 获取群聊历史消息（优先从 Redis 读取）
     */
    public List<MessageResponse> getMessages(Long conversationId, Long currentUserId, int page, int size) {
        // 1. 验证群聊存在
        conversationRepository.findById(conversationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ERROR, "群聊不存在"));

        // 2. 验证用户是群成员
        memberRepository.findByConversationIdAndUserIdAndIsActive(conversationId, currentUserId, true)
                .orElseThrow(() -> new BusinessException(ErrorCode.NO_AUTH_ERROR, "您不在该群聊中"));

        // 3. 第一页优先从 Redis 读取
        if (page == 0) {
            List<MessageResponse> cachedMessages = messageCacheService.getCachedMessages(conversationId, size);

            if (!cachedMessages.isEmpty() && cachedMessages.size() >= size) {
                log.info("从 Redis 返回消息 - 群聊ID: {}, 数量: {}", conversationId, cachedMessages.size());
                return cachedMessages;
            }
        }

        // 4. Redis 没有或不够，从 MySQL 查询
        Pageable pageable = PageRequest.of(page, size);
        Page<Message> messagePage = messageRepository.findByConversationId(conversationId, pageable);

        List<MessageResponse> messages = messagePage.getContent().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());

        // 反转顺序（从旧到新）
        java.util.Collections.reverse(messages);

        // 5. 第一页数据缓存到 Redis
        if (page == 0 && !messages.isEmpty()) {
            messageCacheService.batchCacheMessages(conversationId, messages);
        }

        log.info("从 MySQL 返回消息 - 群聊ID: {}, 数量: {}", conversationId, messages.size());
        return messages;
    }

    /**
     * 转换为响应对象
     */
    private MessageResponse convertToResponse(Message message) {
        User sender = userRepository.findById(message.getSenderId())
                .orElse(null);

        return MessageResponse.builder()
                .id(message.getId())
                .conversationId(message.getConversationId())
                .senderId(message.getSenderId())
                .receiverId(message.getReceiverId())      // 添加这行
                .senderUsername(sender != null ? sender.getUsername() : "未知用户")
                .senderEmail(sender != null ? sender.getEmail() : "")
                .content(message.getContent())
                .messageType(message.getMessageType())
                .chatType("group")                         // 添加这行
                .createdAt(message.getCreatedAt())
                .build();
    }
}
