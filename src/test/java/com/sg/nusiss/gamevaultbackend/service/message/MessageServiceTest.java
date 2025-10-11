package com.sg.nusiss.gamevaultbackend.service.message;

import com.sg.nusiss.gamevaultbackend.common.ErrorCode;
import com.sg.nusiss.gamevaultbackend.dto.message.request.SendMessageRequest;
import com.sg.nusiss.gamevaultbackend.dto.message.response.MessageResponse;
import com.sg.nusiss.gamevaultbackend.entity.auth.User;
import com.sg.nusiss.gamevaultbackend.entity.conversation.Conversation;
import com.sg.nusiss.gamevaultbackend.entity.conversation.Member;
import com.sg.nusiss.gamevaultbackend.entity.message.Message;
import com.sg.nusiss.gamevaultbackend.exception.BusinessException;
import com.sg.nusiss.gamevaultbackend.repository.auth.UserRepository;
import com.sg.nusiss.gamevaultbackend.repository.conversation.ConversationRepository;
import com.sg.nusiss.gamevaultbackend.repository.conversation.MemberRepository;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * @ClassName MessageServiceTest
 * @Author HUANG ZHENJIA
 * @Date 2025/10/12
 * @Description
 */

@ExtendWith(MockitoExtension.class)
public class MessageServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MessageCacheService messageCacheService;

    @InjectMocks
    private MessageService messageService;

    private User testUser;
    private Conversation testConversation;
    private Member testMember;
    private SendMessageRequest testRequest;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUserId(1L);
        testUser.setUsername("testUser");
        testUser.setEmail("test@example.com");

        testConversation = Conversation.builder()
                .id(1L)
                .uuid(UUID.randomUUID().toString())
                .title("测试群聊")
                .ownerId(1L)
                .status("active")
                .build();

        testMember = Member.builder()
                .id(1L)
                .conversation(testConversation)
                .user(testUser)
                .role("member")
                .isActive(true)
                .build();

        testRequest = new SendMessageRequest();
        testRequest.setConversationId(1L);
        testRequest.setContent("测试消息");
        testRequest.setMessageType("text");
    }

    // ==================== sendMessage 方法测试 ====================

    @Test
    void testSendMessage_Success() {
        // Given
        Long senderId = 1L;

        when(conversationRepository.findById(1L)).thenReturn(Optional.of(testConversation));
        when(memberRepository.findByConversationIdAndUserIdAndIsActive(1L, senderId, true))
                .thenReturn(Optional.of(testMember));
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> {
            Message msg = invocation.getArgument(0);
            msg.setId(100L);
            return msg;
        });
        when(userRepository.findById(senderId)).thenReturn(Optional.of(testUser));
        doNothing().when(messageCacheService).cacheMessage(any(MessageResponse.class));

        // When
        MessageResponse result = messageService.sendMessage(testRequest, senderId);

        // Then
        assertNotNull(result, "结果不应为null");
        assertEquals(100L, result.getId(), "消息ID应该匹配");
        assertEquals(1L, result.getConversationId(), "群聊ID应该匹配");
        assertEquals(senderId, result.getSenderId(), "发送者ID应该匹配");
        assertEquals("testUser", result.getSenderUsername(), "发送者用户名应该匹配");
        assertEquals("测试消息", result.getContent(), "消息内容应该匹配");
        assertEquals("text", result.getMessageType(), "消息类型应该匹配");
        assertNotNull(result.getCreatedAt(), "创建时间不应为null");

        // 验证方法调用
        verify(conversationRepository, times(1)).findById(1L);
        verify(memberRepository, times(1))
                .findByConversationIdAndUserIdAndIsActive(1L, senderId, true);
        verify(messageRepository, times(1)).save(any(Message.class));
        verify(messageCacheService, times(1)).cacheMessage(any(MessageResponse.class));

        // 验证保存的Message
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository).save(messageCaptor.capture());
        Message savedMessage = messageCaptor.getValue();

        assertEquals(1L, savedMessage.getConversationId(), "conversationId应该匹配");
        assertEquals(senderId, savedMessage.getSenderId(), "senderId应该匹配");
        assertEquals("测试消息", savedMessage.getContent(), "内容应该匹配");
        assertEquals("text", savedMessage.getMessageType(), "类型应该匹配");
        assertFalse(savedMessage.getIsDeleted(), "应该标记为未删除");
        assertNotNull(savedMessage.getCreatedAt(), "创建时间不应为null");
    }

    @Test
    void testSendMessage_ConversationNotFound_ThrowsException() {
        // Given
        Long senderId = 1L;

        when(conversationRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> messageService.sendMessage(testRequest, senderId),
                "群聊不存在应该抛出异常"
        );

        assertEquals(ErrorCode.NOT_FOUND_ERROR.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("群聊不存在"));
        verify(memberRepository, never())
                .findByConversationIdAndUserIdAndIsActive(anyLong(), anyLong(), anyBoolean());
        verify(messageRepository, never()).save(any());
    }

    @Test
    void testSendMessage_ConversationDissolved_ThrowsException() {
        // Given
        Long senderId = 1L;
        testConversation.setStatus("dissolved");

        when(conversationRepository.findById(1L)).thenReturn(Optional.of(testConversation));

        // When & Then
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> messageService.sendMessage(testRequest, senderId),
                "群聊已解散应该抛出异常"
        );

        assertEquals(ErrorCode.OPERATION_ERROR.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("群聊已解散"));
        verify(memberRepository, never())
                .findByConversationIdAndUserIdAndIsActive(anyLong(), anyLong(), anyBoolean());
        verify(messageRepository, never()).save(any());
    }

    @Test
    void testSendMessage_SenderNotMember_ThrowsException() {
        // Given
        Long senderId = 999L; // 不是群成员

        when(conversationRepository.findById(1L)).thenReturn(Optional.of(testConversation));
        when(memberRepository.findByConversationIdAndUserIdAndIsActive(1L, senderId, true))
                .thenReturn(Optional.empty());

        // When & Then
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> messageService.sendMessage(testRequest, senderId),
                "非群成员不能发送消息"
        );

        assertEquals(ErrorCode.NO_AUTH_ERROR.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("您不在该群聊中"));
        verify(messageRepository, never()).save(any());
    }

    @Test
    void testSendMessage_ContentIsNull_ThrowsException() {
        // Given
        Long senderId = 1L;
        testRequest.setContent(null);

        when(conversationRepository.findById(1L)).thenReturn(Optional.of(testConversation));
        when(memberRepository.findByConversationIdAndUserIdAndIsActive(1L, senderId, true))
                .thenReturn(Optional.of(testMember));

        // When & Then
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> messageService.sendMessage(testRequest, senderId),
                "消息内容为null应该抛出异常"
        );

        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("消息内容不能为空"));
        verify(messageRepository, never()).save(any());
    }

    @Test
    void testSendMessage_ContentIsEmpty_ThrowsException() {
        // Given
        Long senderId = 1L;
        testRequest.setContent("");

        when(conversationRepository.findById(1L)).thenReturn(Optional.of(testConversation));
        when(memberRepository.findByConversationIdAndUserIdAndIsActive(1L, senderId, true))
                .thenReturn(Optional.of(testMember));

        // When & Then
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> messageService.sendMessage(testRequest, senderId)
        );

        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("消息内容不能为空"));
    }

    @Test
    void testSendMessage_ContentIsBlank_ThrowsException() {
        // Given
        Long senderId = 1L;
        testRequest.setContent("   ");

        when(conversationRepository.findById(1L)).thenReturn(Optional.of(testConversation));
        when(memberRepository.findByConversationIdAndUserIdAndIsActive(1L, senderId, true))
                .thenReturn(Optional.of(testMember));

        // When & Then
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> messageService.sendMessage(testRequest, senderId)
        );

        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("消息内容不能为空"));
    }

    @Test
    void testSendMessage_ContentWithWhitespace_TrimmedSuccessfully() {
        // Given - 消息内容有前后空格，应该被trim
        Long senderId = 1L;
        testRequest.setContent("  测试消息  ");

        when(conversationRepository.findById(1L)).thenReturn(Optional.of(testConversation));
        when(memberRepository.findByConversationIdAndUserIdAndIsActive(1L, senderId, true))
                .thenReturn(Optional.of(testMember));
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> {
            Message msg = invocation.getArgument(0);
            msg.setId(100L);
            return msg;
        });
        when(userRepository.findById(senderId)).thenReturn(Optional.of(testUser));
        doNothing().when(messageCacheService).cacheMessage(any(MessageResponse.class));

        // When
        MessageResponse result = messageService.sendMessage(testRequest, senderId);

        // Then
        assertEquals("测试消息", result.getContent(), "内容应该被trim");

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository).save(captor.capture());
        Message savedMessage = captor.getValue();

        assertEquals("测试消息", savedMessage.getContent(), "保存的内容应该被trim");
    }

    @Test
    void testSendMessage_WithDifferentMessageType_Success() {
        // Given - 测试不同的消息类型
        Long senderId = 1L;
        testRequest.setMessageType("image");

        when(conversationRepository.findById(1L)).thenReturn(Optional.of(testConversation));
        when(memberRepository.findByConversationIdAndUserIdAndIsActive(1L, senderId, true))
                .thenReturn(Optional.of(testMember));
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> {
            Message msg = invocation.getArgument(0);
            msg.setId(100L);
            return msg;
        });
        when(userRepository.findById(senderId)).thenReturn(Optional.of(testUser));
        doNothing().when(messageCacheService).cacheMessage(any(MessageResponse.class));

        // When
        MessageResponse result = messageService.sendMessage(testRequest, senderId);

        // Then
        assertEquals("image", result.getMessageType(), "消息类型应该是image");
    }

    @Test
    void testSendMessage_VerifyCacheServiceCalled() {
        // Given
        Long senderId = 1L;

        when(conversationRepository.findById(1L)).thenReturn(Optional.of(testConversation));
        when(memberRepository.findByConversationIdAndUserIdAndIsActive(1L, senderId, true))
                .thenReturn(Optional.of(testMember));
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> {
            Message msg = invocation.getArgument(0);
            msg.setId(100L);
            return msg;
        });
        when(userRepository.findById(senderId)).thenReturn(Optional.of(testUser));
        doNothing().when(messageCacheService).cacheMessage(any(MessageResponse.class));

        // When
        messageService.sendMessage(testRequest, senderId);

        // Then - 验证缓存服务被调用
        ArgumentCaptor<MessageResponse> cacheCaptor = ArgumentCaptor.forClass(MessageResponse.class);
        verify(messageCacheService, times(1)).cacheMessage(cacheCaptor.capture());

        MessageResponse cachedMessage = cacheCaptor.getValue();
        assertEquals(100L, cachedMessage.getId());
        assertEquals("测试消息", cachedMessage.getContent());
    }

    @Test
    void testSendMessage_SenderUserNotFound_UsesDefaultUsername() {
        // Given - 发送者用户不存在，应该使用默认用户名
        Long senderId = 1L;

        when(conversationRepository.findById(1L)).thenReturn(Optional.of(testConversation));
        when(memberRepository.findByConversationIdAndUserIdAndIsActive(1L, senderId, true))
                .thenReturn(Optional.of(testMember));
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> {
            Message msg = invocation.getArgument(0);
            msg.setId(100L);
            return msg;
        });
        when(userRepository.findById(senderId)).thenReturn(Optional.empty()); // 用户不存在
        doNothing().when(messageCacheService).cacheMessage(any(MessageResponse.class));

        // When
        MessageResponse result = messageService.sendMessage(testRequest, senderId);

        // Then
        assertEquals("未知用户", result.getSenderUsername(), "应该使用默认用户名");
        assertEquals("", result.getSenderEmail(), "邮箱应该为空字符串");
    }

    @Test
    void testSendMessage_WithLongContent_Success() {
        // Given - 测试长消息
        Long senderId = 1L;
        String longContent = "这是一条很长的消息内容".repeat(50);
        testRequest.setContent(longContent);

        when(conversationRepository.findById(1L)).thenReturn(Optional.of(testConversation));
        when(memberRepository.findByConversationIdAndUserIdAndIsActive(1L, senderId, true))
                .thenReturn(Optional.of(testMember));
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> {
            Message msg = invocation.getArgument(0);
            msg.setId(100L);
            return msg;
        });
        when(userRepository.findById(senderId)).thenReturn(Optional.of(testUser));
        doNothing().when(messageCacheService).cacheMessage(any(MessageResponse.class));

        // When
        MessageResponse result = messageService.sendMessage(testRequest, senderId);

        // Then
        assertEquals(longContent, result.getContent(), "长消息应该完整保存");
    }

    @Test
    void testSendMessage_VerifyTimestamp() {
        // Given
        Long senderId = 1L;
        LocalDateTime beforeCall = LocalDateTime.now();

        when(conversationRepository.findById(1L)).thenReturn(Optional.of(testConversation));
        when(memberRepository.findByConversationIdAndUserIdAndIsActive(1L, senderId, true))
                .thenReturn(Optional.of(testMember));
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> {
            Message msg = invocation.getArgument(0);
            msg.setId(100L);
            return msg;
        });
        when(userRepository.findById(senderId)).thenReturn(Optional.of(testUser));
        doNothing().when(messageCacheService).cacheMessage(any(MessageResponse.class));

        // When
        messageService.sendMessage(testRequest, senderId);
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

    // ==================== getMessages 方法测试 ====================

    @Test
    void testGetMessages_FirstPageFromCache_Success() {
        // Given - 第一页从 Redis 缓存读取，缓存足够多
        Long conversationId = 1L;
        Long currentUserId = 1L;
        int page = 0;
        int size = 20;

        MessageResponse cachedMsg1 = MessageResponse.builder()
                .id(1L)
                .conversationId(conversationId)
                .senderId(1L)
                .senderUsername("user1")
                .content("缓存消息1")
                .messageType("text")
                .createdAt(LocalDateTime.now())
                .build();

        MessageResponse cachedMsg2 = MessageResponse.builder()
                .id(2L)
                .conversationId(conversationId)
                .senderId(2L)
                .senderUsername("user2")
                .content("缓存消息2")
                .messageType("text")
                .createdAt(LocalDateTime.now())
                .build();

        // 创建一个包含20条消息的列表（足够size）
        List<MessageResponse> cachedMessages = new java.util.ArrayList<>();
        cachedMessages.add(cachedMsg1);
        cachedMessages.add(cachedMsg2);
        // 填充到20条
        for (int i = 3; i <= 20; i++) {
            MessageResponse msg = MessageResponse.builder()
                    .id((long) i)
                    .conversationId(conversationId)
                    .senderId(1L)
                    .senderUsername("user")
                    .content("缓存消息" + i)
                    .messageType("text")
                    .createdAt(LocalDateTime.now())
                    .build();
            cachedMessages.add(msg);
        }

        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(testConversation));
        when(memberRepository.findByConversationIdAndUserIdAndIsActive(conversationId, currentUserId, true))
                .thenReturn(Optional.of(testMember));
        when(messageCacheService.getCachedMessages(conversationId, size))
                .thenReturn(cachedMessages); // 返回足够数量的缓存

        // When
        List<MessageResponse> result = messageService.getMessages(conversationId, currentUserId, page, size);

        // Then
        assertNotNull(result, "结果不应为null");
        assertEquals(20, result.size(), "应该返回20条缓存消息");
        assertEquals("缓存消息1", result.get(0).getContent());
        assertEquals("缓存消息2", result.get(1).getContent());

        verify(conversationRepository, times(1)).findById(conversationId);
        verify(memberRepository, times(1))
                .findByConversationIdAndUserIdAndIsActive(conversationId, currentUserId, true);
        verify(messageCacheService, times(1)).getCachedMessages(conversationId, size);
        verify(messageRepository, never()).findByConversationId(any(), any()); // 不应查询数据库
    }

    @Test
    void testGetMessages_FirstPageFromDatabase_Success() {
        // Given - 缓存为空，从数据库读取
        Long conversationId = 1L;
        Long currentUserId = 1L;
        int page = 0;
        int size = 20;

        Message msg1 = Message.builder()
                .id(1L)
                .conversationId(conversationId)
                .senderId(1L)
                .content("数据库消息1")
                .messageType("text")
                .createdAt(LocalDateTime.now())
                .isDeleted(false)
                .build();

        Message msg2 = Message.builder()
                .id(2L)
                .conversationId(conversationId)
                .senderId(1L)
                .content("数据库消息2")
                .messageType("text")
                .createdAt(LocalDateTime.now())
                .isDeleted(false)
                .build();

        Page<Message> messagePage = mock(Page.class);
        when(messagePage.getContent()).thenReturn(Arrays.asList(msg1, msg2));

        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(testConversation));
        when(memberRepository.findByConversationIdAndUserIdAndIsActive(conversationId, currentUserId, true))
                .thenReturn(Optional.of(testMember));
        when(messageCacheService.getCachedMessages(conversationId, size))
                .thenReturn(Collections.emptyList()); // 缓存为空
        when(messageRepository.findByConversationId(eq(conversationId), any(Pageable.class)))
                .thenReturn(messagePage);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        doNothing().when(messageCacheService).batchCacheMessages(eq(conversationId), anyList());

        // When
        List<MessageResponse> result = messageService.getMessages(conversationId, currentUserId, page, size);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        // 注意：消息顺序应该被反转（从旧到新）
        assertEquals("数据库消息2", result.get(0).getContent(), "应该反转顺序");
        assertEquals("数据库消息1", result.get(1).getContent(), "应该反转顺序");

        verify(messageRepository, times(1)).findByConversationId(eq(conversationId), any(Pageable.class));
        verify(messageCacheService, times(1)).batchCacheMessages(eq(conversationId), anyList());
    }

    @Test
    void testGetMessages_SecondPage_FromDatabaseOnly() {
        // Given - 第二页不从缓存读取
        Long conversationId = 1L;
        Long currentUserId = 1L;
        int page = 1; // 第二页
        int size = 20;

        Message msg = Message.builder()
                .id(3L)
                .conversationId(conversationId)
                .senderId(1L)
                .content("第二页消息")
                .messageType("text")
                .createdAt(LocalDateTime.now())
                .isDeleted(false)
                .build();

        Page<Message> messagePage = mock(Page.class);
        when(messagePage.getContent()).thenReturn(Collections.singletonList(msg));

        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(testConversation));
        when(memberRepository.findByConversationIdAndUserIdAndIsActive(conversationId, currentUserId, true))
                .thenReturn(Optional.of(testMember));
        when(messageRepository.findByConversationId(eq(conversationId), any(Pageable.class)))
                .thenReturn(messagePage);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When
        List<MessageResponse> result = messageService.getMessages(conversationId, currentUserId, page, size);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("第二页消息", result.get(0).getContent());

        verify(messageCacheService, never()).getCachedMessages(anyLong(), anyInt()); // 不查缓存
        verify(messageRepository, times(1)).findByConversationId(eq(conversationId), any(Pageable.class));
        verify(messageCacheService, never()).batchCacheMessages(anyLong(), anyList()); // 不缓存第二页
    }

    @Test
    void testGetMessages_ConversationNotFound_ThrowsException() {
        // Given
        Long conversationId = 999L;
        Long currentUserId = 1L;
        int page = 0;
        int size = 20;

        when(conversationRepository.findById(conversationId)).thenReturn(Optional.empty());

        // When & Then
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> messageService.getMessages(conversationId, currentUserId, page, size),
                "群聊不存在应该抛出异常"
        );

        assertEquals(ErrorCode.NOT_FOUND_ERROR.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("群聊不存在"));
        verify(memberRepository, never())
                .findByConversationIdAndUserIdAndIsActive(anyLong(), anyLong(), anyBoolean());
        verify(messageCacheService, never()).getCachedMessages(anyLong(), anyInt());
    }

    @Test
    void testGetMessages_UserNotMember_ThrowsException() {
        // Given
        Long conversationId = 1L;
        Long currentUserId = 999L; // 不是群成员
        int page = 0;
        int size = 20;

        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(testConversation));
        when(memberRepository.findByConversationIdAndUserIdAndIsActive(conversationId, currentUserId, true))
                .thenReturn(Optional.empty());

        // When & Then
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> messageService.getMessages(conversationId, currentUserId, page, size),
                "非群成员不能查看消息"
        );

        assertEquals(ErrorCode.NO_AUTH_ERROR.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("您不在该群聊中"));
        verify(messageCacheService, never()).getCachedMessages(anyLong(), anyInt());
        verify(messageRepository, never()).findByConversationId(any(), any());
    }

    @Test
    void testGetMessages_CacheNotEnough_FallbackToDatabase() {
        // Given - 缓存数量不够，从数据库读取
        Long conversationId = 1L;
        Long currentUserId = 1L;
        int page = 0;
        int size = 20;

        // 缓存只有5条，不够20条
        MessageResponse cachedMsg = MessageResponse.builder()
                .id(1L)
                .conversationId(conversationId)
                .senderId(1L)
                .content("缓存消息")
                .messageType("text")
                .createdAt(LocalDateTime.now())
                .build();

        List<MessageResponse> insufficientCache = Collections.singletonList(cachedMsg);

        Message dbMsg = Message.builder()
                .id(2L)
                .conversationId(conversationId)
                .senderId(1L)
                .content("数据库消息")
                .messageType("text")
                .createdAt(LocalDateTime.now())
                .isDeleted(false)
                .build();

        Page<Message> messagePage = mock(Page.class);
        when(messagePage.getContent()).thenReturn(Collections.singletonList(dbMsg));

        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(testConversation));
        when(memberRepository.findByConversationIdAndUserIdAndIsActive(conversationId, currentUserId, true))
                .thenReturn(Optional.of(testMember));
        when(messageCacheService.getCachedMessages(conversationId, size))
                .thenReturn(insufficientCache); // 缓存不够
        when(messageRepository.findByConversationId(eq(conversationId), any(Pageable.class)))
                .thenReturn(messagePage);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        doNothing().when(messageCacheService).batchCacheMessages(eq(conversationId), anyList());

        // When
        List<MessageResponse> result = messageService.getMessages(conversationId, currentUserId, page, size);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("数据库消息", result.get(0).getContent());

        verify(messageCacheService, times(1)).getCachedMessages(conversationId, size);
        verify(messageRepository, times(1)).findByConversationId(eq(conversationId), any(Pageable.class));
    }

    @Test
    void testGetMessages_EmptyResult_ReturnsEmptyList() {
        // Given - 数据库和缓存都没有消息
        Long conversationId = 1L;
        Long currentUserId = 1L;
        int page = 0;
        int size = 20;

        Page<Message> emptyPage = mock(Page.class);
        when(emptyPage.getContent()).thenReturn(Collections.emptyList());

        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(testConversation));
        when(memberRepository.findByConversationIdAndUserIdAndIsActive(conversationId, currentUserId, true))
                .thenReturn(Optional.of(testMember));
        when(messageCacheService.getCachedMessages(conversationId, size))
                .thenReturn(Collections.emptyList());
        when(messageRepository.findByConversationId(eq(conversationId), any(Pageable.class)))
                .thenReturn(emptyPage);

        // When
        List<MessageResponse> result = messageService.getMessages(conversationId, currentUserId, page, size);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        assertEquals(0, result.size());

        verify(messageCacheService, never()).batchCacheMessages(anyLong(), anyList()); // 空结果不缓存
    }

    @Test
    void testGetMessages_VerifyMessageOrder_Reversed() {
        // Given - 验证消息顺序被反转
        Long conversationId = 1L;
        Long currentUserId = 1L;
        int page = 0;
        int size = 20;

        Message msg1 = Message.builder()
                .id(1L)
                .conversationId(conversationId)
                .senderId(1L)
                .content("消息1")
                .messageType("text")
                .createdAt(LocalDateTime.now().minusMinutes(3))
                .isDeleted(false)
                .build();

        Message msg2 = Message.builder()
                .id(2L)
                .conversationId(conversationId)
                .senderId(1L)
                .content("消息2")
                .messageType("text")
                .createdAt(LocalDateTime.now().minusMinutes(2))
                .isDeleted(false)
                .build();

        Message msg3 = Message.builder()
                .id(3L)
                .conversationId(conversationId)
                .senderId(1L)
                .content("消息3")
                .messageType("text")
                .createdAt(LocalDateTime.now().minusMinutes(1))
                .isDeleted(false)
                .build();

        // 数据库返回的是新到旧的顺序
        Page<Message> messagePage = mock(Page.class);
        when(messagePage.getContent()).thenReturn(Arrays.asList(msg3, msg2, msg1));

        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(testConversation));
        when(memberRepository.findByConversationIdAndUserIdAndIsActive(conversationId, currentUserId, true))
                .thenReturn(Optional.of(testMember));
        when(messageCacheService.getCachedMessages(conversationId, size))
                .thenReturn(Collections.emptyList());
        when(messageRepository.findByConversationId(eq(conversationId), any(Pageable.class)))
                .thenReturn(messagePage);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        doNothing().when(messageCacheService).batchCacheMessages(eq(conversationId), anyList());

        // When
        List<MessageResponse> result = messageService.getMessages(conversationId, currentUserId, page, size);

        // Then
        assertEquals(3, result.size());
        // 应该反转为从旧到新的顺序
        assertEquals("消息1", result.get(0).getContent());
        assertEquals("消息2", result.get(1).getContent());
        assertEquals("消息3", result.get(2).getContent());
    }

    @Test
    void testGetMessages_VerifyBatchCacheCalled() {
        // Given - 验证批量缓存被调用
        Long conversationId = 1L;
        Long currentUserId = 1L;
        int page = 0;
        int size = 20;

        Message msg = Message.builder()
                .id(1L)
                .conversationId(conversationId)
                .senderId(1L)
                .content("消息")
                .messageType("text")
                .createdAt(LocalDateTime.now())
                .isDeleted(false)
                .build();

        Page<Message> messagePage = mock(Page.class);
        when(messagePage.getContent()).thenReturn(Collections.singletonList(msg));

        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(testConversation));
        when(memberRepository.findByConversationIdAndUserIdAndIsActive(conversationId, currentUserId, true))
                .thenReturn(Optional.of(testMember));
        when(messageCacheService.getCachedMessages(conversationId, size))
                .thenReturn(Collections.emptyList());
        when(messageRepository.findByConversationId(eq(conversationId), any(Pageable.class)))
                .thenReturn(messagePage);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        doNothing().when(messageCacheService).batchCacheMessages(eq(conversationId), anyList());

        // When
        messageService.getMessages(conversationId, currentUserId, page, size);

        // Then - 验证批量缓存方法被调用
        ArgumentCaptor<List<MessageResponse>> cacheCaptor = ArgumentCaptor.forClass(List.class);
        verify(messageCacheService, times(1))
                .batchCacheMessages(eq(conversationId), cacheCaptor.capture());

        List<MessageResponse> cachedMessages = cacheCaptor.getValue();
        assertEquals(1, cachedMessages.size());
        assertEquals("消息", cachedMessages.get(0).getContent());
    }

    @Test
    void testGetMessages_WithDifferentPageSize_Success() {
        // Given - 测试不同的页面大小
        Long conversationId = 1L;
        Long currentUserId = 1L;
        int page = 0;
        int size = 50; // 不同的页面大小

        MessageResponse cachedMsg = MessageResponse.builder()
                .id(1L)
                .conversationId(conversationId)
                .senderId(1L)
                .content("消息")
                .messageType("text")
                .createdAt(LocalDateTime.now())
                .build();

        // 返回足够的缓存消息
        List<MessageResponse> cachedMessages = Collections.nCopies(50, cachedMsg);

        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(testConversation));
        when(memberRepository.findByConversationIdAndUserIdAndIsActive(conversationId, currentUserId, true))
                .thenReturn(Optional.of(testMember));
        when(messageCacheService.getCachedMessages(conversationId, size))
                .thenReturn(cachedMessages);

        // When
        List<MessageResponse> result = messageService.getMessages(conversationId, currentUserId, page, size);

        // Then
        assertEquals(50, result.size());
        verify(messageCacheService, times(1)).getCachedMessages(conversationId, 50);
    }

    @Test
    void testGetMessages_SenderNotFound_UsesDefaultUsername() {
        // Given - convertToResponse 中用户不存在
        Long conversationId = 1L;
        Long currentUserId = 1L;
        int page = 0;
        int size = 20;

        Message msg = Message.builder()
                .id(1L)
                .conversationId(conversationId)
                .senderId(999L) // 不存在的用户
                .content("消息")
                .messageType("text")
                .createdAt(LocalDateTime.now())
                .isDeleted(false)
                .build();

        Page<Message> messagePage = mock(Page.class);
        when(messagePage.getContent()).thenReturn(Collections.singletonList(msg));

        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(testConversation));
        when(memberRepository.findByConversationIdAndUserIdAndIsActive(conversationId, currentUserId, true))
                .thenReturn(Optional.of(testMember));
        when(messageCacheService.getCachedMessages(conversationId, size))
                .thenReturn(Collections.emptyList());
        when(messageRepository.findByConversationId(eq(conversationId), any(Pageable.class)))
                .thenReturn(messagePage);
        when(userRepository.findById(999L)).thenReturn(Optional.empty()); // 用户不存在
        doNothing().when(messageCacheService).batchCacheMessages(eq(conversationId), anyList());

        // When
        List<MessageResponse> result = messageService.getMessages(conversationId, currentUserId, page, size);

        // Then
        assertEquals(1, result.size());
        assertEquals("未知用户", result.get(0).getSenderUsername());
        assertEquals("", result.get(0).getSenderEmail());
    }
}
