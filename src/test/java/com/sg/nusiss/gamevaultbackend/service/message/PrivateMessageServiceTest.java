package com.sg.nusiss.gamevaultbackend.service.message;

import com.sg.nusiss.gamevaultbackend.common.ErrorCode;
import com.sg.nusiss.gamevaultbackend.dto.message.request.SendPrivateMessageRequest;
import com.sg.nusiss.gamevaultbackend.dto.message.response.MessageResponse;
import com.sg.nusiss.gamevaultbackend.entity.auth.User;
import com.sg.nusiss.gamevaultbackend.entity.friend.Friendship;
import com.sg.nusiss.gamevaultbackend.entity.message.Message;
import com.sg.nusiss.gamevaultbackend.exception.BusinessException;
import com.sg.nusiss.gamevaultbackend.repository.auth.UserRepository;
import com.sg.nusiss.gamevaultbackend.repository.friend.FriendshipRepository;
import com.sg.nusiss.gamevaultbackend.repository.message.MessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * @ClassName PrivateMessageServiceTest
 * @Author HUANG ZHENJIA
 * @Date 2025/10/12
 * @Description
 */

@ExtendWith(MockitoExtension.class)
class PrivateMessageServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FriendshipRepository friendshipRepository;

    @InjectMocks
    private PrivateMessageService privateMessageService;

    private User sender;
    private User receiver;
    private Friendship friendship;
    private SendPrivateMessageRequest testRequest;

    @BeforeEach
    void setUp() {
        sender = new User();
        sender.setUserId(1L);
        sender.setUsername("sender");
        sender.setEmail("sender@example.com");

        receiver = new User();
        receiver.setUserId(2L);
        receiver.setUsername("receiver");
        receiver.setEmail("receiver@example.com");

        friendship = new Friendship();
        friendship.setId(1L);
        friendship.setUserId(1L);
        friendship.setFriendId(2L);
        friendship.setIsActive(true);
        friendship.setCreatedAt(LocalDateTime.now());

        testRequest = new SendPrivateMessageRequest();
        testRequest.setReceiverId(2L);
        testRequest.setContent("私聊消息");
        testRequest.setMessageType("text");
    }

    // ==================== sendPrivateMessage 方法测试 ====================

    @Test
    void testSendPrivateMessage_Success() {
        // Given
        Long senderId = 1L;

        when(userRepository.findById(2L)).thenReturn(Optional.of(receiver));
        when(friendshipRepository.findByUserIdAndFriendIdAndIsActive(senderId, 2L, true))
                .thenReturn(Optional.of(friendship));
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> {
            Message msg = invocation.getArgument(0);
            msg.setId(100L);
            return msg;
        });
        when(userRepository.findById(senderId)).thenReturn(Optional.of(sender));

        // When
        MessageResponse result = privateMessageService.sendPrivateMessage(testRequest, senderId);

        // Then
        assertNotNull(result, "结果不应为null");
        assertEquals(100L, result.getId(), "消息ID应该匹配");
        assertEquals(senderId, result.getSenderId(), "发送者ID应该匹配");
        assertEquals(2L, result.getReceiverId(), "接收者ID应该匹配");
        assertEquals("sender", result.getSenderUsername(), "发送者用户名应该匹配");
        assertEquals("私聊消息", result.getContent(), "消息内容应该匹配");
        assertEquals("text", result.getMessageType(), "消息类型应该匹配");
        assertEquals("private", result.getChatType(), "聊天类型应该是private");
        assertNotNull(result.getCreatedAt(), "创建时间不应为null");

        // 验证方法调用
        verify(userRepository, times(1)).findById(2L);
        verify(friendshipRepository, times(1))
                .findByUserIdAndFriendIdAndIsActive(senderId, 2L, true);
        verify(messageRepository, times(1)).save(any(Message.class));

        // 验证保存的Message
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository).save(messageCaptor.capture());
        Message savedMessage = messageCaptor.getValue();

        assertEquals(senderId, savedMessage.getSenderId(), "senderId应该匹配");
        assertEquals(2L, savedMessage.getReceiverId(), "receiverId应该匹配");
        assertEquals("私聊消息", savedMessage.getContent(), "内容应该匹配");
        assertEquals("text", savedMessage.getMessageType(), "类型应该匹配");
        assertEquals("private", savedMessage.getChatType(), "chatType应该是private");
        assertFalse(savedMessage.getIsDeleted(), "应该标记为未删除");
        assertNotNull(savedMessage.getCreatedAt(), "创建时间不应为null");
        assertNull(savedMessage.getConversationId(), "私聊消息不应有conversationId");
    }

    @Test
    void testSendPrivateMessage_ReceiverNotFound_ThrowsException() {
        // Given
        Long senderId = 1L;

        when(userRepository.findById(2L)).thenReturn(Optional.empty());

        // When & Then
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> privateMessageService.sendPrivateMessage(testRequest, senderId),
                "接收者不存在应该抛出异常"
        );

        assertEquals(ErrorCode.NOT_FOUND_ERROR.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("接收者不存在"));
        verify(friendshipRepository, never())
                .findByUserIdAndFriendIdAndIsActive(anyLong(), anyLong(), anyBoolean());
        verify(messageRepository, never()).save(any());
    }

    @Test
    void testSendPrivateMessage_NotFriends_ThrowsException() {
        // Given
        Long senderId = 1L;

        when(userRepository.findById(2L)).thenReturn(Optional.of(receiver));
        when(friendshipRepository.findByUserIdAndFriendIdAndIsActive(senderId, 2L, true))
                .thenReturn(Optional.empty()); // 不是好友

        // When & Then
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> privateMessageService.sendPrivateMessage(testRequest, senderId),
                "非好友不能发送消息"
        );

        assertEquals(ErrorCode.NO_AUTH_ERROR.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("只能给好友发送消息"));
        verify(messageRepository, never()).save(any());
    }

    @Test
    void testSendPrivateMessage_ContentIsNull_ThrowsException() {
        // Given
        Long senderId = 1L;
        testRequest.setContent(null);

        when(userRepository.findById(2L)).thenReturn(Optional.of(receiver));
        when(friendshipRepository.findByUserIdAndFriendIdAndIsActive(senderId, 2L, true))
                .thenReturn(Optional.of(friendship));

        // When & Then
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> privateMessageService.sendPrivateMessage(testRequest, senderId),
                "消息内容为null应该抛出异常"
        );

        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("消息内容不能为空"));
        verify(messageRepository, never()).save(any());
    }

    @Test
    void testSendPrivateMessage_ContentIsEmpty_ThrowsException() {
        // Given
        Long senderId = 1L;
        testRequest.setContent("");

        when(userRepository.findById(2L)).thenReturn(Optional.of(receiver));
        when(friendshipRepository.findByUserIdAndFriendIdAndIsActive(senderId, 2L, true))
                .thenReturn(Optional.of(friendship));

        // When & Then
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> privateMessageService.sendPrivateMessage(testRequest, senderId)
        );

        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("消息内容不能为空"));
    }

    @Test
    void testSendPrivateMessage_ContentIsBlank_ThrowsException() {
        // Given
        Long senderId = 1L;
        testRequest.setContent("   ");

        when(userRepository.findById(2L)).thenReturn(Optional.of(receiver));
        when(friendshipRepository.findByUserIdAndFriendIdAndIsActive(senderId, 2L, true))
                .thenReturn(Optional.of(friendship));

        // When & Then
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> privateMessageService.sendPrivateMessage(testRequest, senderId)
        );

        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("消息内容不能为空"));
    }

    @Test
    void testSendPrivateMessage_ContentWithWhitespace_TrimmedSuccessfully() {
        // Given - 消息内容有前后空格，应该被trim
        Long senderId = 1L;
        testRequest.setContent("  私聊消息  ");

        when(userRepository.findById(2L)).thenReturn(Optional.of(receiver));
        when(friendshipRepository.findByUserIdAndFriendIdAndIsActive(senderId, 2L, true))
                .thenReturn(Optional.of(friendship));
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> {
            Message msg = invocation.getArgument(0);
            msg.setId(100L);
            return msg;
        });
        when(userRepository.findById(senderId)).thenReturn(Optional.of(sender));

        // When
        MessageResponse result = privateMessageService.sendPrivateMessage(testRequest, senderId);

        // Then
        assertEquals("私聊消息", result.getContent(), "内容应该被trim");

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository).save(captor.capture());
        Message savedMessage = captor.getValue();

        assertEquals("私聊消息", savedMessage.getContent(), "保存的内容应该被trim");
    }

    @Test
    void testSendPrivateMessage_WithDifferentMessageType_Success() {
        // Given - 测试不同的消息类型
        Long senderId = 1L;
        testRequest.setMessageType("image");

        when(userRepository.findById(2L)).thenReturn(Optional.of(receiver));
        when(friendshipRepository.findByUserIdAndFriendIdAndIsActive(senderId, 2L, true))
                .thenReturn(Optional.of(friendship));
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> {
            Message msg = invocation.getArgument(0);
            msg.setId(100L);
            return msg;
        });
        when(userRepository.findById(senderId)).thenReturn(Optional.of(sender));

        // When
        MessageResponse result = privateMessageService.sendPrivateMessage(testRequest, senderId);

        // Then
        assertEquals("image", result.getMessageType(), "消息类型应该是image");
    }

    @Test
    void testSendPrivateMessage_SenderNotFound_UsesDefaultUsername() {
        // Given - 发送者用户不存在，应该使用默认用户名
        Long senderId = 1L;

        when(userRepository.findById(2L)).thenReturn(Optional.of(receiver));
        when(friendshipRepository.findByUserIdAndFriendIdAndIsActive(senderId, 2L, true))
                .thenReturn(Optional.of(friendship));
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> {
            Message msg = invocation.getArgument(0);
            msg.setId(100L);
            return msg;
        });
        when(userRepository.findById(senderId)).thenReturn(Optional.empty()); // 用户不存在

        // When
        MessageResponse result = privateMessageService.sendPrivateMessage(testRequest, senderId);

        // Then
        assertEquals("未知用户", result.getSenderUsername(), "应该使用默认用户名");
        assertEquals("", result.getSenderEmail(), "邮箱应该为空字符串");
    }

    @Test
    void testSendPrivateMessage_WithLongContent_Success() {
        // Given - 测试长消息
        Long senderId = 1L;
        String longContent = "这是一条很长的私聊消息".repeat(50);
        testRequest.setContent(longContent);

        when(userRepository.findById(2L)).thenReturn(Optional.of(receiver));
        when(friendshipRepository.findByUserIdAndFriendIdAndIsActive(senderId, 2L, true))
                .thenReturn(Optional.of(friendship));
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> {
            Message msg = invocation.getArgument(0);
            msg.setId(100L);
            return msg;
        });
        when(userRepository.findById(senderId)).thenReturn(Optional.of(sender));

        // When
        MessageResponse result = privateMessageService.sendPrivateMessage(testRequest, senderId);

        // Then
        assertEquals(longContent, result.getContent(), "长消息应该完整保存");
    }

    @Test
    void testSendPrivateMessage_VerifyTimestamp() {
        // Given
        Long senderId = 1L;
        LocalDateTime beforeCall = LocalDateTime.now();

        when(userRepository.findById(2L)).thenReturn(Optional.of(receiver));
        when(friendshipRepository.findByUserIdAndFriendIdAndIsActive(senderId, 2L, true))
                .thenReturn(Optional.of(friendship));
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> {
            Message msg = invocation.getArgument(0);
            msg.setId(100L);
            return msg;
        });
        when(userRepository.findById(senderId)).thenReturn(Optional.of(sender));

        // When
        privateMessageService.sendPrivateMessage(testRequest, senderId);
        LocalDateTime afterCall = LocalDateTime.now();

        // Then
        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository).save(captor.capture());
        Message savedMessage = captor.getValue();

        assertNotNull(savedMessage.getCreatedAt());
        assertTrue(
                !savedMessage.getCreatedAt().isBefore(beforeCall) &&
                        !savedMessage.getCreatedAt().isAfter(afterCall),
                "创建时间应该在方法调用期间"
        );
    }

    @Test
    void testSendPrivateMessage_VerifyChatTypeIsPrivate() {
        // Given
        Long senderId = 1L;

        when(userRepository.findById(2L)).thenReturn(Optional.of(receiver));
        when(friendshipRepository.findByUserIdAndFriendIdAndIsActive(senderId, 2L, true))
                .thenReturn(Optional.of(friendship));
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> {
            Message msg = invocation.getArgument(0);
            msg.setId(100L);
            return msg;
        });
        when(userRepository.findById(senderId)).thenReturn(Optional.of(sender));

        // When
        MessageResponse result = privateMessageService.sendPrivateMessage(testRequest, senderId);

        // Then
        assertEquals("private", result.getChatType(), "chatType应该是private");

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository).save(captor.capture());
        Message savedMessage = captor.getValue();

        assertEquals("private", savedMessage.getChatType(), "保存的chatType应该是private");
    }

    // ==================== getPrivateMessages 方法测试 ====================

    @Test
    void testGetPrivateMessages_Success() {
        // Given
        Long userId = 1L;
        Long friendId = 2L;
        int page = 0;
        int size = 20;

        Message msg1 = Message.builder()
                .id(1L)
                .senderId(userId)
                .receiverId(friendId)
                .content("消息1")
                .messageType("text")
                .chatType("private")
                .createdAt(LocalDateTime.now().minusMinutes(2))
                .isDeleted(false)
                .build();

        Message msg2 = Message.builder()
                .id(2L)
                .senderId(friendId)
                .receiverId(userId)
                .content("消息2")
                .messageType("text")
                .chatType("private")
                .createdAt(LocalDateTime.now().minusMinutes(1))
                .isDeleted(false)
                .build();

        Page<Message> messagePage = mock(Page.class);
        when(messagePage.getContent()).thenReturn(Arrays.asList(msg2, msg1)); // 新到旧

        when(friendshipRepository.findByUserIdAndFriendIdAndIsActive(userId, friendId, true))
                .thenReturn(Optional.of(friendship));
        when(messageRepository.findPrivateMessages(eq(userId), eq(friendId), any(Pageable.class)))
                .thenReturn(messagePage);
        when(userRepository.findById(userId)).thenReturn(Optional.of(sender));
        when(userRepository.findById(friendId)).thenReturn(Optional.of(receiver));

        // When
        List<MessageResponse> result = privateMessageService.getPrivateMessages(userId, friendId, page, size);

        // Then
        assertNotNull(result, "结果不应为null");
        assertEquals(2, result.size(), "应该返回2条消息");

        // 验证顺序被反转（从旧到新）
        assertEquals("消息1", result.get(0).getContent(), "应该反转顺序");
        assertEquals("消息2", result.get(1).getContent(), "应该反转顺序");
        assertEquals(1L, result.get(0).getId());
        assertEquals(2L, result.get(1).getId());

        verify(friendshipRepository, times(1))
                .findByUserIdAndFriendIdAndIsActive(userId, friendId, true);
        verify(messageRepository, times(1))
                .findPrivateMessages(eq(userId), eq(friendId), any(Pageable.class));
    }

    @Test
    void testGetPrivateMessages_NotFriends_ThrowsException() {
        // Given
        Long userId = 1L;
        Long friendId = 2L;
        int page = 0;
        int size = 20;

        when(friendshipRepository.findByUserIdAndFriendIdAndIsActive(userId, friendId, true))
                .thenReturn(Optional.empty()); // 不是好友

        // When & Then
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> privateMessageService.getPrivateMessages(userId, friendId, page, size),
                "非好友不能查看聊天记录"
        );

        assertEquals(ErrorCode.NO_AUTH_ERROR.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("只能查看好友的聊天记录"));
        verify(messageRepository, never()).findPrivateMessages(anyLong(), anyLong(), any());
    }

    @Test
    void testGetPrivateMessages_EmptyResult_ReturnsEmptyList() {
        // Given
        Long userId = 1L;
        Long friendId = 2L;
        int page = 0;
        int size = 20;

        Page<Message> emptyPage = mock(Page.class);
        when(emptyPage.getContent()).thenReturn(Collections.emptyList());

        when(friendshipRepository.findByUserIdAndFriendIdAndIsActive(userId, friendId, true))
                .thenReturn(Optional.of(friendship));
        when(messageRepository.findPrivateMessages(eq(userId), eq(friendId), any(Pageable.class)))
                .thenReturn(emptyPage);

        // When
        List<MessageResponse> result = privateMessageService.getPrivateMessages(userId, friendId, page, size);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        assertEquals(0, result.size());
    }

    @Test
    void testGetPrivateMessages_WithSingleMessage_Success() {
        // Given
        Long userId = 1L;
        Long friendId = 2L;
        int page = 0;
        int size = 20;

        Message msg = Message.builder()
                .id(1L)
                .senderId(userId)
                .receiverId(friendId)
                .content("单条消息")
                .messageType("text")
                .chatType("private")
                .createdAt(LocalDateTime.now())
                .isDeleted(false)
                .build();

        Page<Message> messagePage = mock(Page.class);
        when(messagePage.getContent()).thenReturn(Collections.singletonList(msg));

        when(friendshipRepository.findByUserIdAndFriendIdAndIsActive(userId, friendId, true))
                .thenReturn(Optional.of(friendship));
        when(messageRepository.findPrivateMessages(eq(userId), eq(friendId), any(Pageable.class)))
                .thenReturn(messagePage);
        when(userRepository.findById(userId)).thenReturn(Optional.of(sender));

        // When
        List<MessageResponse> result = privateMessageService.getPrivateMessages(userId, friendId, page, size);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("单条消息", result.get(0).getContent());
    }

    @Test
    void testGetPrivateMessages_VerifyPagination() {
        // Given - 测试分页
        Long userId = 1L;
        Long friendId = 2L;
        int page = 2;
        int size = 50;

        Page<Message> emptyPage = mock(Page.class);
        when(emptyPage.getContent()).thenReturn(Collections.emptyList());

        when(friendshipRepository.findByUserIdAndFriendIdAndIsActive(userId, friendId, true))
                .thenReturn(Optional.of(friendship));
        when(messageRepository.findPrivateMessages(eq(userId), eq(friendId), any(Pageable.class)))
                .thenReturn(emptyPage);

        // When
        privateMessageService.getPrivateMessages(userId, friendId, page, size);

        // Then - 验证分页参数
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(messageRepository).findPrivateMessages(eq(userId), eq(friendId), pageableCaptor.capture());

        Pageable capturedPageable = pageableCaptor.getValue();
        assertEquals(page, capturedPageable.getPageNumber(), "页码应该匹配");
        assertEquals(size, capturedPageable.getPageSize(), "页面大小应该匹配");
    }

    @Test
    void testGetPrivateMessages_SenderNotFound_UsesDefaultUsername() {
        // Given - convertToResponse 中用户不存在
        Long userId = 1L;
        Long friendId = 2L;
        int page = 0;
        int size = 20;

        Message msg = Message.builder()
                .id(1L)
                .senderId(999L) // 不存在的用户
                .receiverId(friendId)
                .content("消息")
                .messageType("text")
                .chatType("private")
                .createdAt(LocalDateTime.now())
                .isDeleted(false)
                .build();

        Page<Message> messagePage = mock(Page.class);
        when(messagePage.getContent()).thenReturn(Collections.singletonList(msg));

        when(friendshipRepository.findByUserIdAndFriendIdAndIsActive(userId, friendId, true))
                .thenReturn(Optional.of(friendship));
        when(messageRepository.findPrivateMessages(eq(userId), eq(friendId), any(Pageable.class)))
                .thenReturn(messagePage);
        when(userRepository.findById(999L)).thenReturn(Optional.empty()); // 用户不存在

        // When
        List<MessageResponse> result = privateMessageService.getPrivateMessages(userId, friendId, page, size);

        // Then
        assertEquals(1, result.size());
        assertEquals("未知用户", result.get(0).getSenderUsername());
        assertEquals("", result.get(0).getSenderEmail());
    }

    @Test
    void testGetPrivateMessages_VerifyResponseFields() {
        // Given - 验证响应对象的所有字段
        Long userId = 1L;
        Long friendId = 2L;
        int page = 0;
        int size = 20;
        LocalDateTime createdTime = LocalDateTime.of(2024, 1, 15, 10, 30);

        Message msg = Message.builder()
                .id(100L)
                .senderId(userId)
                .receiverId(friendId)
                .content("测试消息")
                .messageType("text")
                .chatType("private")
                .createdAt(createdTime)
                .isDeleted(false)
                .build();

        Page<Message> messagePage = mock(Page.class);
        when(messagePage.getContent()).thenReturn(Collections.singletonList(msg));

        when(friendshipRepository.findByUserIdAndFriendIdAndIsActive(userId, friendId, true))
                .thenReturn(Optional.of(friendship));
        when(messageRepository.findPrivateMessages(eq(userId), eq(friendId), any(Pageable.class)))
                .thenReturn(messagePage);
        when(userRepository.findById(userId)).thenReturn(Optional.of(sender));

        // When
        List<MessageResponse> result = privateMessageService.getPrivateMessages(userId, friendId, page, size);

        // Then
        assertEquals(1, result.size());
        MessageResponse response = result.get(0);

        // 详细验证所有字段
        assertEquals(100L, response.getId(), "消息ID应该匹配");
        assertNull(response.getConversationId(), "私聊消息不应有conversationId");
        assertEquals(userId, response.getSenderId(), "发送者ID应该匹配");
        assertEquals(friendId, response.getReceiverId(), "接收者ID应该匹配");
        assertEquals("sender", response.getSenderUsername(), "发送者用户名应该匹配");
        assertEquals("sender@example.com", response.getSenderEmail(), "发送者邮箱应该匹配");
        assertEquals("测试消息", response.getContent(), "消息内容应该匹配");
        assertEquals("text", response.getMessageType(), "消息类型应该匹配");
        assertEquals("private", response.getChatType(), "聊天类型应该是private");
        assertEquals(createdTime, response.getCreatedAt(), "创建时间应该匹配");
    }

    @Test
    void testGetPrivateMessages_VerifyOrderReversed() {
        // Given - 验证消息顺序被反转
        Long userId = 1L;
        Long friendId = 2L;
        int page = 0;
        int size = 20;

        Message msg1 = Message.builder()
                .id(1L)
                .senderId(userId)
                .receiverId(friendId)
                .content("最旧消息")
                .messageType("text")
                .chatType("private")
                .createdAt(LocalDateTime.now().minusMinutes(5))
                .build();

        Message msg2 = Message.builder()
                .id(2L)
                .senderId(friendId)
                .receiverId(userId)
                .content("中间消息")
                .messageType("text")
                .chatType("private")
                .createdAt(LocalDateTime.now().minusMinutes(3))
                .build();

        Message msg3 = Message.builder()
                .id(3L)
                .senderId(userId)
                .receiverId(friendId)
                .content("最新消息")
                .messageType("text")
                .chatType("private")
                .createdAt(LocalDateTime.now().minusMinutes(1))
                .build();

        // 数据库返回从新到旧
        Page<Message> messagePage = mock(Page.class);
        when(messagePage.getContent()).thenReturn(Arrays.asList(msg3, msg2, msg1));

        when(friendshipRepository.findByUserIdAndFriendIdAndIsActive(userId, friendId, true))
                .thenReturn(Optional.of(friendship));
        when(messageRepository.findPrivateMessages(eq(userId), eq(friendId), any(Pageable.class)))
                .thenReturn(messagePage);
        when(userRepository.findById(userId)).thenReturn(Optional.of(sender));
        when(userRepository.findById(friendId)).thenReturn(Optional.of(receiver));

        // When
        List<MessageResponse> result = privateMessageService.getPrivateMessages(userId, friendId, page, size);

        // Then
        assertEquals(3, result.size());
        // 应该反转为从旧到新
        assertEquals("最旧消息", result.get(0).getContent());
        assertEquals("中间消息", result.get(1).getContent());
        assertEquals("最新消息", result.get(2).getContent());
        assertEquals(1L, result.get(0).getId());
        assertEquals(2L, result.get(1).getId());
        assertEquals(3L, result.get(2).getId());
    }
}