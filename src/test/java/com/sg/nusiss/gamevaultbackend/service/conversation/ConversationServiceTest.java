package com.sg.nusiss.gamevaultbackend.service.conversation;

import com.sg.nusiss.gamevaultbackend.common.ErrorCode;
import com.sg.nusiss.gamevaultbackend.dto.conversation.response.ConversationListResponse;
import com.sg.nusiss.gamevaultbackend.dto.conversation.response.MemberResponse;
import com.sg.nusiss.gamevaultbackend.entity.auth.User;
import com.sg.nusiss.gamevaultbackend.entity.conversation.Conversation;
import com.sg.nusiss.gamevaultbackend.entity.conversation.Member;
import com.sg.nusiss.gamevaultbackend.entity.friend.Friendship;
import com.sg.nusiss.gamevaultbackend.exception.BusinessException;
import com.sg.nusiss.gamevaultbackend.repository.auth.UserRepository;
import com.sg.nusiss.gamevaultbackend.repository.conversation.ConversationRepository;
import com.sg.nusiss.gamevaultbackend.repository.conversation.MemberRepository;
import com.sg.nusiss.gamevaultbackend.repository.friend.FriendshipRepository;
import com.sg.nusiss.gamevaultbackend.repository.message.MessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * @ClassName ConversationServiceTest
 * @Author HUANG ZHENJIA
 * @Date 2025/10/11
 * @Description
 */
@ExtendWith(MockitoExtension.class)
class ConversationServiceTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FriendshipRepository friendshipRepository;

    @InjectMocks
    private ConversationService conversationService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUserId(1L);
        testUser.setUsername("testUser");
        testUser.setEmail("test@example.com");
    }

    // ==================== createConversation 方法测试 ====================

    @Test
    void testCreateConversation_Success() {
        // Given
        String title = "测试群聊";
        Long ownerId = 1L;

        when(userRepository.findById(ownerId)).thenReturn(Optional.of(testUser));

        when(conversationRepository.save(any(Conversation.class))).thenAnswer(invocation -> {
            Conversation conv = invocation.getArgument(0);
            conv.setId(1L);
            conv.setUuid(UUID.randomUUID().toString());
            conv.setCreatedAt(LocalDateTime.now());
            conv.setUpdatedAt(LocalDateTime.now());
            return conv;
        });

        when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Conversation result = conversationService.createConversation(title, ownerId);

        // Then - 断言验证结果
        assertNotNull(result, "创建的群聊不应为null");
        assertEquals(title, result.getTitle(), "群聊标题应该匹配");
        assertEquals(ownerId, result.getOwnerId(), "群主ID应该匹配");
        assertEquals("active", result.getStatus(), "群聊状态应该是active");
        assertNotNull(result.getId(), "群聊ID不应为null");

        // 验证Repository方法调用次数
        verify(userRepository, times(1)).findById(ownerId);
        verify(conversationRepository, times(1)).save(any(Conversation.class));
        verify(memberRepository, times(1)).save(any(Member.class));

        // 验证Conversation保存的参数
        ArgumentCaptor<Conversation> convCaptor = ArgumentCaptor.forClass(Conversation.class);
        verify(conversationRepository).save(convCaptor.capture());
        Conversation savedConv = convCaptor.getValue();
        assertEquals(title, savedConv.getTitle(), "保存的conversation标题应该正确");
        assertEquals(ownerId, savedConv.getOwnerId(), "保存的conversation群主ID应该正确");

        // 验证Member保存的参数
        ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);
        verify(memberRepository).save(memberCaptor.capture());
        Member savedMember = memberCaptor.getValue();

        assertEquals("owner", savedMember.getRole(), "群主的角色应该是owner");
        assertEquals(testUser, savedMember.getUser(), "成员的用户应该是testUser");
        assertEquals(result, savedMember.getConversation(), "成员所属的群聊应该正确");
        assertNotNull(savedMember.getJoinedAt(), "加入时间不应为null");
        assertTrue(savedMember.getIsActive(), "成员应该是活跃状态");
    }

    @Test
    void testCreateConversation_TitleIsNull_ThrowsException() {
        // Given
        String title = null;
        Long ownerId = 1L;

        // When & Then
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> conversationService.createConversation(title, ownerId),
                "标题为null应该抛出BusinessException"
        );

        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), exception.getCode(), "错误码应该是PARAMS_ERROR");
        assertTrue(exception.getMessage().contains("群聊标题不能为空"),
                "错误消息应包含'群聊标题不能为空'");

        // 验证不应该调用后续方法
        verify(userRepository, never()).findById(anyLong());
        verify(conversationRepository, never()).save(any(Conversation.class));
        verify(memberRepository, never()).save(any(Member.class));
    }

    @Test
    void testCreateConversation_TitleIsEmpty_ThrowsException() {
        // Given
        String title = "";
        Long ownerId = 1L;

        // When & Then
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> conversationService.createConversation(title, ownerId),
                "标题为空字符串应该抛出BusinessException"
        );

        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("群聊标题不能为空"));
        verify(userRepository, never()).findById(anyLong());
    }

    @Test
    void testCreateConversation_TitleIsBlank_ThrowsException() {
        // Given
        String title = "   "; // 只有空格
        Long ownerId = 1L;

        // When & Then
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> conversationService.createConversation(title, ownerId),
                "标题只有空格应该抛出BusinessException"
        );

        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("群聊标题不能为空"));
        verify(userRepository, never()).findById(anyLong());
    }

    @Test
    void testCreateConversation_OwnerIdIsNull_ThrowsException() {
        // Given
        String title = "测试群聊";
        Long ownerId = null;

        // When & Then
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> conversationService.createConversation(title, ownerId),
                "ownerId为null应该抛出BusinessException"
        );

        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("ownerId 不能为空"));
        verify(userRepository, never()).findById(any());
        verify(conversationRepository, never()).save(any());
    }

    @Test
    void testCreateConversation_UserNotFound_ThrowsException() {
        // Given
        String title = "测试群聊";
        Long ownerId = 999L;

        when(userRepository.findById(ownerId)).thenReturn(Optional.empty());

        // When & Then
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> conversationService.createConversation(title, ownerId),
                "用户不存在应该抛出BusinessException"
        );

        assertEquals(ErrorCode.NOT_FOUND_ERROR.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("用户不存在"));

        // 验证只调用了findById，没有调用save
        verify(userRepository, times(1)).findById(ownerId);
        verify(conversationRepository, never()).save(any(Conversation.class));
        verify(memberRepository, never()).save(any(Member.class));
    }

    @Test
    void testCreateConversation_WithSpecialCharactersInTitle_Success() {
        // Given - 测试特殊字符标题
        String title = "测试群聊!@#$%^&*()_+-=[]{}|;':\",./<>?";
        Long ownerId = 1L;

        when(userRepository.findById(ownerId)).thenReturn(Optional.of(testUser));
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(invocation -> {
            Conversation conv = invocation.getArgument(0);
            conv.setId(1L);
            return conv;
        });
        when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Conversation result = conversationService.createConversation(title, ownerId);

        // Then
        assertNotNull(result);
        assertEquals(title, result.getTitle(), "应该能处理特殊字符标题");
        verify(conversationRepository, times(1)).save(any(Conversation.class));
    }

    @Test
    void testCreateConversation_WithLongTitle_Success() {
        // Given - 测试长标题（边界情况）
        String title = "A".repeat(255); // 假设数据库限制是255
        Long ownerId = 1L;

        when(userRepository.findById(ownerId)).thenReturn(Optional.of(testUser));
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(invocation -> {
            Conversation conv = invocation.getArgument(0);
            conv.setId(1L);
            return conv;
        });
        when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Conversation result = conversationService.createConversation(title, ownerId);

        // Then
        assertNotNull(result);
        assertEquals(255, result.getTitle().length(), "应该能处理最大长度标题");
    }

    @Test
    void testCreateConversation_VerifyConversationBuilderFields() {
        // Given
        String title = "Builder测试";
        Long ownerId = 1L;

        when(userRepository.findById(ownerId)).thenReturn(Optional.of(testUser));
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        conversationService.createConversation(title, ownerId);

        // Then - 验证使用Builder创建的Conversation字段
        ArgumentCaptor<Conversation> captor = ArgumentCaptor.forClass(Conversation.class);
        verify(conversationRepository).save(captor.capture());
        Conversation saved = captor.getValue();

        assertEquals(title, saved.getTitle(), "标题应该正确设置");
        assertEquals(ownerId, saved.getOwnerId(), "群主ID应该正确设置");
        assertNotNull(saved.getStatus(), "状态不应为null");
        assertEquals("active", saved.getStatus(), "默认状态应该是active");
    }

    @Test
    void testCreateConversation_VerifyMemberJoinedAtIsSet() {
        // Given
        String title = "时间测试";
        Long ownerId = 1L;
        LocalDateTime beforeCall = LocalDateTime.now();

        when(userRepository.findById(ownerId)).thenReturn(Optional.of(testUser));
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(invocation -> {
            Conversation conv = invocation.getArgument(0);
            conv.setId(1L);
            return conv;
        });
        when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        conversationService.createConversation(title, ownerId);
        LocalDateTime afterCall = LocalDateTime.now();

        // Then - 验证joinedAt时间是否在合理范围内
        ArgumentCaptor<Member> captor = ArgumentCaptor.forClass(Member.class);
        verify(memberRepository).save(captor.capture());
        Member saved = captor.getValue();

        assertNotNull(saved.getJoinedAt(), "joinedAt不应为null");
        assertTrue(
                !saved.getJoinedAt().isBefore(beforeCall) && !saved.getJoinedAt().isAfter(afterCall),
                "joinedAt应该在方法调用期间"
        );
    }

    // ==================== getUserConversations 方法测试 ====================

    @Test
    void testGetUserConversations_Success() {
        // Given
        Long userId = 1L;

        // 创建测试用的Conversation对象
        Conversation conversation1 = Conversation.builder()
                .id(1L)
                .uuid(UUID.randomUUID().toString())
                .title("测试群聊1")
                .ownerId(1L)
                .status("active")
                .nextSeq(1L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Conversation conversation2 = Conversation.builder()
                .id(2L)
                .uuid(UUID.randomUUID().toString())
                .title("测试群聊2")
                .ownerId(2L)
                .status("active")
                .nextSeq(1L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // 创建Member对象
        Member member1 = Member.builder()
                .id(1L)
                .conversation(conversation1)
                .user(testUser)
                .role("owner")
                .joinedAt(LocalDateTime.now())
                .isActive(true)
                .build();

        Member member2 = Member.builder()
                .id(2L)
                .conversation(conversation2)
                .user(testUser)
                .role("member")
                .joinedAt(LocalDateTime.now())
                .isActive(true)
                .build();

        List<Member> members = Arrays.asList(member1, member2);

        when(memberRepository.findByUserId(userId)).thenReturn(members);

        // When
        List<ConversationListResponse> result = conversationService.getUserConversations(userId);

        // Then
        assertNotNull(result, "结果不应为null");
        assertEquals(2, result.size(), "应该返回2个群聊");

        // 验证第一个群聊
        ConversationListResponse response1 = result.get(0);
        assertEquals(1L, response1.getId(), "第一个群聊ID应该是1");
        assertEquals("测试群聊1", response1.getTitle(), "第一个群聊标题应该匹配");
        assertEquals(1L, response1.getOwnerId(), "第一个群聊群主ID应该是1");
        assertEquals("active", response1.getStatus(), "第一个群聊状态应该是active");
        assertNotNull(response1.getCreatedAt(), "创建时间不应为null");

        // 验证第二个群聊
        ConversationListResponse response2 = result.get(1);
        assertEquals(2L, response2.getId(), "第二个群聊ID应该是2");
        assertEquals("测试群聊2", response2.getTitle(), "第二个群聊标题应该匹配");
        assertEquals(2L, response2.getOwnerId(), "第二个群聊群主ID应该是2");

        // 验证方法调用
        verify(memberRepository, times(1)).findByUserId(userId);
    }

    @Test
    void testGetUserConversations_UserHasNoConversations_ReturnsEmptyList() {
        // Given
        Long userId = 1L;
        when(memberRepository.findByUserId(userId)).thenReturn(Collections.emptyList());

        // When
        List<ConversationListResponse> result = conversationService.getUserConversations(userId);

        // Then
        assertNotNull(result, "结果不应为null");
        assertTrue(result.isEmpty(), "结果应该是空列表");
        assertEquals(0, result.size(), "列表大小应该为0");

        verify(memberRepository, times(1)).findByUserId(userId);
    }

    @Test
    void testGetUserConversations_WithSingleConversation() {
        // Given
        Long userId = 1L;

        Conversation conversation = Conversation.builder()
                .id(1L)
                .uuid(UUID.randomUUID().toString())
                .title("单个群聊")
                .ownerId(1L)
                .status("active")
                .createdAt(LocalDateTime.now())
                .build();

        Member member = Member.builder()
                .id(1L)
                .conversation(conversation)
                .user(testUser)
                .role("owner")
                .joinedAt(LocalDateTime.now())
                .isActive(true)
                .build();

        when(memberRepository.findByUserId(userId)).thenReturn(Collections.singletonList(member));

        // When
        List<ConversationListResponse> result = conversationService.getUserConversations(userId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size(), "应该返回1个群聊");
        assertEquals("单个群聊", result.get(0).getTitle());
    }

    @Test
    void testGetUserConversations_WithMultipleRoles() {
        // Given - 用户在不同群聊中有不同角色
        Long userId = 1L;

        Conversation conv1 = Conversation.builder()
                .id(1L)
                .uuid(UUID.randomUUID().toString())
                .title("我是群主")
                .ownerId(userId)
                .status("active")
                .createdAt(LocalDateTime.now())
                .build();

        Conversation conv2 = Conversation.builder()
                .id(2L)
                .uuid(UUID.randomUUID().toString())
                .title("我是成员")
                .ownerId(2L)
                .status("active")
                .createdAt(LocalDateTime.now())
                .build();

        Member ownerMember = Member.builder()
                .id(1L)
                .conversation(conv1)
                .user(testUser)
                .role("owner")
                .isActive(true)
                .build();

        Member regularMember = Member.builder()
                .id(2L)
                .conversation(conv2)
                .user(testUser)
                .role("member")
                .isActive(true)
                .build();

        when(memberRepository.findByUserId(userId))
                .thenReturn(Arrays.asList(ownerMember, regularMember));

        // When
        List<ConversationListResponse> result = conversationService.getUserConversations(userId);

        // Then
        assertEquals(2, result.size(), "应该返回2个群聊");

        // 验证可以同时获取作为群主和成员的群聊
        boolean hasOwnerConv = result.stream()
                .anyMatch(r -> r.getTitle().equals("我是群主"));
        boolean hasMemberConv = result.stream()
                .anyMatch(r -> r.getTitle().equals("我是成员"));

        assertTrue(hasOwnerConv, "应该包含用户作为群主的群聊");
        assertTrue(hasMemberConv, "应该包含用户作为成员的群聊");
    }

    @Test
    void testGetUserConversations_WithDissolvedConversation() {
        // Given - 包含已解散的群聊
        Long userId = 1L;

        Conversation activeConv = Conversation.builder()
                .id(1L)
                .uuid(UUID.randomUUID().toString())
                .title("活跃群聊")
                .ownerId(userId)
                .status("active")
                .createdAt(LocalDateTime.now())
                .build();

        Conversation dissolvedConv = Conversation.builder()
                .id(2L)
                .uuid(UUID.randomUUID().toString())
                .title("已解散群聊")
                .ownerId(userId)
                .status("dissolved")
                .createdAt(LocalDateTime.now())
                .dissolvedAt(LocalDateTime.now())
                .build();

        Member member1 = Member.builder()
                .id(1L)
                .conversation(activeConv)
                .user(testUser)
                .role("owner")
                .isActive(true)
                .build();

        Member member2 = Member.builder()
                .id(2L)
                .conversation(dissolvedConv)
                .user(testUser)
                .role("owner")
                .isActive(false)
                .build();

        when(memberRepository.findByUserId(userId))
                .thenReturn(Arrays.asList(member1, member2));

        // When
        List<ConversationListResponse> result = conversationService.getUserConversations(userId);

        // Then
        assertEquals(2, result.size(), "应该返回2个群聊（包括已解散的）");

        // 验证已解散的群聊也被返回
        boolean hasDissolvedConv = result.stream()
                .anyMatch(r -> r.getStatus().equals("dissolved"));

        assertTrue(hasDissolvedConv, "应该包含已解散的群聊");
    }

    @Test
    void testGetUserConversations_VerifyResponseFields() {
        // Given - 验证返回的DTO字段是否完整
        Long userId = 1L;
        LocalDateTime createdTime = LocalDateTime.now();

        Conversation conversation = Conversation.builder()
                .id(100L)
                .uuid(UUID.randomUUID().toString())
                .title("字段测试群聊")
                .ownerId(50L)
                .status("active")
                .createdAt(createdTime)
                .build();

        Member member = Member.builder()
                .id(1L)
                .conversation(conversation)
                .user(testUser)
                .role("member")
                .isActive(true)
                .build();

        when(memberRepository.findByUserId(userId))
                .thenReturn(Collections.singletonList(member));

        // When
        List<ConversationListResponse> result = conversationService.getUserConversations(userId);

        // Then
        assertEquals(1, result.size());
        ConversationListResponse response = result.get(0);

        // 详细验证每个字段
        assertEquals(100L, response.getId(), "ID应该匹配");
        assertEquals("字段测试群聊", response.getTitle(), "标题应该匹配");
        assertEquals(50L, response.getOwnerId(), "群主ID应该匹配");
        assertEquals("active", response.getStatus(), "状态应该匹配");
        assertEquals(createdTime, response.getCreatedAt(), "创建时间应该匹配");
    }

    @Test
    void testGetUserConversations_WithDuplicateConversations_ShouldDeduplicate() {
        // Given - 测试去重功能（理论上不应该发生，但测试边界情况）
        Long userId = 1L;

        Conversation conversation = Conversation.builder()
                .id(1L)
                .uuid(UUID.randomUUID().toString())
                .title("重复群聊")
                .ownerId(userId)
                .status("active")
                .createdAt(LocalDateTime.now())
                .build();

        // 同一个conversation的两个member记录（不应该发生，但测试去重逻辑）
        Member member1 = Member.builder()
                .id(1L)
                .conversation(conversation)
                .user(testUser)
                .role("owner")
                .isActive(true)
                .build();

        Member member2 = Member.builder()
                .id(2L)
                .conversation(conversation) // 同一个conversation对象
                .user(testUser)
                .role("member")
                .isActive(true)
                .build();

        when(memberRepository.findByUserId(userId))
                .thenReturn(Arrays.asList(member1, member2));

        // When
        List<ConversationListResponse> result = conversationService.getUserConversations(userId);

        // Then
        assertEquals(1, result.size(), "应该去重，只返回1个群聊");
        assertEquals("重复群聊", result.get(0).getTitle());
    }

    @Test
    void testGetUserConversations_WithNullUserId_ShouldHandleGracefully() {
        // Given - 测试null userId（虽然service没有显式检查，但测试行为）
        Long userId = null;

        when(memberRepository.findByUserId(userId)).thenReturn(Collections.emptyList());

        // When
        List<ConversationListResponse> result = conversationService.getUserConversations(userId);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ==================== dissolveConversation 方法测试 ====================

    @Test
    void testDissolveConversation_Success() {
        // Given
        Long conversationId = 1L;
        Long currentUserId = 1L;

        Conversation conversation = Conversation.builder()
                .id(conversationId)
                .uuid(UUID.randomUUID().toString())
                .title("待解散群聊")
                .ownerId(currentUserId)
                .status("active")
                .createdAt(LocalDateTime.now())
                .build();

        User user2 = new User();
        user2.setUserId(2L);
        user2.setUsername("member1");
        user2.setEmail("member1@test.com");

        User user3 = new User();
        user3.setUserId(3L);
        user3.setUsername("member2");
        user3.setEmail("member2@test.com");

        Member member1 = Member.builder()
                .id(1L)
                .conversation(conversation)
                .user(testUser)
                .role("owner")
                .isActive(true)
                .joinedAt(LocalDateTime.now())
                .build();

        Member member2 = Member.builder()
                .id(2L)
                .conversation(conversation)
                .user(user2)
                .role("member")
                .isActive(true)
                .joinedAt(LocalDateTime.now())
                .build();

        Member member3 = Member.builder()
                .id(3L)
                .conversation(conversation)
                .user(user3)
                .role("member")
                .isActive(true)
                .joinedAt(LocalDateTime.now())
                .build();

        List<Member> activeMembers = Arrays.asList(member1, member2, member3);

        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(memberRepository.findByConversationIdAndIsActive(conversationId, true))
                .thenReturn(activeMembers);
        when(conversationRepository.save(any(Conversation.class))).thenReturn(conversation);
        when(memberRepository.saveAll(anyList())).thenReturn(activeMembers);

        // When
        conversationService.dissolveConversation(conversationId, currentUserId);

        // Then
        verify(conversationRepository, times(1)).findById(conversationId);
        verify(memberRepository, times(1)).findByConversationIdAndIsActive(conversationId, true);
        verify(conversationRepository, times(1)).save(any(Conversation.class));
        verify(memberRepository, times(1)).saveAll(anyList());

        // 验证Conversation的更新
        ArgumentCaptor<Conversation> convCaptor = ArgumentCaptor.forClass(Conversation.class);
        verify(conversationRepository).save(convCaptor.capture());
        Conversation savedConv = convCaptor.getValue();

        assertEquals("dissolved", savedConv.getStatus(), "状态应该是dissolved");
        assertNotNull(savedConv.getDissolvedAt(), "解散时间不应为null");
        assertEquals(currentUserId, savedConv.getDissolvedBy(), "解散操作人应该是当前用户");
        assertEquals("群主解散", savedConv.getDissolvedReason(), "解散原因应该是'群主解散'");

        // 验证Members的更新
        ArgumentCaptor<List<Member>> membersCaptor = ArgumentCaptor.forClass(List.class);
        verify(memberRepository).saveAll(membersCaptor.capture());
        List<Member> savedMembers = membersCaptor.getValue();

        assertEquals(3, savedMembers.size(), "应该更新3个成员");
        for (Member member : savedMembers) {
            assertFalse(member.getIsActive(), "成员应该设置为不活跃");
            assertNotNull(member.getLeftAt(), "退出时间不应为null");
            assertEquals("群聊已解散", member.getLeaveReason(), "退出原因应该是'群聊已解散'");
        }
    }

    @Test
    void testDissolveConversation_ConversationIdIsNull_ThrowsException() {
        // Given
        Long conversationId = null;
        Long currentUserId = 1L;

        // When & Then
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> conversationService.dissolveConversation(conversationId, currentUserId),
                "conversationId为null应该抛出异常"
        );

        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("群聊ID不能为空"));
        verify(conversationRepository, never()).findById(anyLong());
    }

    @Test
    void testDissolveConversation_CurrentUserIdIsNull_ThrowsException() {
        // Given
        Long conversationId = 1L;
        Long currentUserId = null;

        // When & Then
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> conversationService.dissolveConversation(conversationId, currentUserId),
                "currentUserId为null应该抛出异常"
        );

        assertEquals(ErrorCode.NOT_LOGIN_ERROR.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("用户未登录"));
        verify(conversationRepository, never()).findById(anyLong());
    }

    @Test
    void testDissolveConversation_ConversationNotFound_ThrowsException() {
        // Given
        Long conversationId = 999L;
        Long currentUserId = 1L;

        when(conversationRepository.findById(conversationId)).thenReturn(Optional.empty());

        // When & Then
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> conversationService.dissolveConversation(conversationId, currentUserId),
                "群聊不存在应该抛出异常"
        );

        assertEquals(ErrorCode.NOT_FOUND_ERROR.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("群聊不存在"));
        verify(conversationRepository, times(1)).findById(conversationId);
        verify(memberRepository, never()).findByConversationIdAndIsActive(anyLong(), anyBoolean());
    }

    @Test
    void testDissolveConversation_AlreadyDissolved_ThrowsException() {
        // Given
        Long conversationId = 1L;
        Long currentUserId = 1L;

        Conversation conversation = Conversation.builder()
                .id(conversationId)
                .uuid(UUID.randomUUID().toString())
                .title("已解散群聊")
                .ownerId(currentUserId)
                .status("dissolved")
                .dissolvedAt(LocalDateTime.now())
                .build();

        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));

        // When & Then
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> conversationService.dissolveConversation(conversationId, currentUserId),
                "群聊已解散应该抛出异常"
        );

        assertEquals(ErrorCode.OPERATION_ERROR.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("群聊已被解散"));
        verify(conversationRepository, never()).save(any());
        verify(memberRepository, never()).findByConversationIdAndIsActive(anyLong(), anyBoolean());
    }

    @Test
    void testDissolveConversation_NotOwner_ThrowsException() {
        // Given
        Long conversationId = 1L;
        Long currentUserId = 2L; // 不是群主
        Long ownerId = 1L; // 真正的群主

        Conversation conversation = Conversation.builder()
                .id(conversationId)
                .uuid(UUID.randomUUID().toString())
                .title("测试群聊")
                .ownerId(ownerId)
                .status("active")
                .build();

        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));

        // When & Then
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> conversationService.dissolveConversation(conversationId, currentUserId),
                "非群主用户不能解散群聊"
        );

        assertEquals(ErrorCode.NO_AUTH_ERROR.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("只有群主可以解散群聊"));
        verify(conversationRepository, never()).save(any());
        verify(memberRepository, never()).findByConversationIdAndIsActive(anyLong(), anyBoolean());
    }

    @Test
    void testDissolveConversation_NoActiveMembers_Success() {
        // Given - 群聊没有活跃成员
        Long conversationId = 1L;
        Long currentUserId = 1L;

        Conversation conversation = Conversation.builder()
                .id(conversationId)
                .uuid(UUID.randomUUID().toString())
                .title("无成员群聊")
                .ownerId(currentUserId)
                .status("active")
                .build();

        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(memberRepository.findByConversationIdAndIsActive(conversationId, true))
                .thenReturn(Collections.emptyList());
        when(conversationRepository.save(any(Conversation.class))).thenReturn(conversation);

        // When
        conversationService.dissolveConversation(conversationId, currentUserId);

        // Then
        verify(conversationRepository, times(1)).save(any(Conversation.class));
        verify(memberRepository, never()).saveAll(anyList()); // 没有成员需要更新

        ArgumentCaptor<Conversation> captor = ArgumentCaptor.forClass(Conversation.class);
        verify(conversationRepository).save(captor.capture());
        Conversation savedConv = captor.getValue();

        assertEquals("dissolved", savedConv.getStatus());
        assertNotNull(savedConv.getDissolvedAt());
    }

    @Test
    void testDissolveConversation_WithSingleMember_Success() {
        // Given - 只有群主一个成员
        Long conversationId = 1L;
        Long currentUserId = 1L;

        Conversation conversation = Conversation.builder()
                .id(conversationId)
                .uuid(UUID.randomUUID().toString())
                .title("单人群聊")
                .ownerId(currentUserId)
                .status("active")
                .build();

        Member ownerMember = Member.builder()
                .id(1L)
                .conversation(conversation)
                .user(testUser)
                .role("owner")
                .isActive(true)
                .joinedAt(LocalDateTime.now())
                .build();

        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(memberRepository.findByConversationIdAndIsActive(conversationId, true))
                .thenReturn(Collections.singletonList(ownerMember));
        when(conversationRepository.save(any(Conversation.class))).thenReturn(conversation);
        when(memberRepository.saveAll(anyList())).thenReturn(Collections.singletonList(ownerMember));

        // When
        conversationService.dissolveConversation(conversationId, currentUserId);

        // Then
        verify(memberRepository, times(1)).saveAll(anyList());

        ArgumentCaptor<List<Member>> captor = ArgumentCaptor.forClass(List.class);
        verify(memberRepository).saveAll(captor.capture());
        List<Member> savedMembers = captor.getValue();

        assertEquals(1, savedMembers.size());
        assertFalse(savedMembers.get(0).getIsActive());
    }

    @Test
    void testDissolveConversation_VerifyTimestamps() {
        // Given - 验证时间戳设置
        Long conversationId = 1L;
        Long currentUserId = 1L;
        LocalDateTime beforeCall = LocalDateTime.now();

        Conversation conversation = Conversation.builder()
                .id(conversationId)
                .uuid(UUID.randomUUID().toString())
                .title("时间戳测试")
                .ownerId(currentUserId)
                .status("active")
                .build();

        Member member = Member.builder()
                .id(1L)
                .conversation(conversation)
                .user(testUser)
                .role("owner")
                .isActive(true)
                .build();

        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(memberRepository.findByConversationIdAndIsActive(conversationId, true))
                .thenReturn(Collections.singletonList(member));
        when(conversationRepository.save(any(Conversation.class))).thenReturn(conversation);
        when(memberRepository.saveAll(anyList())).thenReturn(Collections.singletonList(member));

        // When
        conversationService.dissolveConversation(conversationId, currentUserId);
        LocalDateTime afterCall = LocalDateTime.now();

        // Then - 验证时间戳在合理范围内
        ArgumentCaptor<Conversation> convCaptor = ArgumentCaptor.forClass(Conversation.class);
        verify(conversationRepository).save(convCaptor.capture());
        Conversation saved = convCaptor.getValue();

        assertNotNull(saved.getDissolvedAt());
        assertTrue(
                !saved.getDissolvedAt().isBefore(beforeCall) && !saved.getDissolvedAt().isAfter(afterCall),
                "解散时间应该在方法调用期间"
        );

        // 验证Member的leftAt时间
        ArgumentCaptor<List<Member>> memberCaptor = ArgumentCaptor.forClass(List.class);
        verify(memberRepository).saveAll(memberCaptor.capture());
        Member savedMember = memberCaptor.getValue().get(0);

        assertNotNull(savedMember.getLeftAt());
        assertTrue(
                !savedMember.getLeftAt().isBefore(beforeCall) && !savedMember.getLeftAt().isAfter(afterCall),
                "退出时间应该在方法调用期间"
        );
    }

    @Test
    void testDissolveConversation_WithMixedMemberRoles_Success() {
        // Given - 测试不同角色的成员都被正确处理
        Long conversationId = 1L;
        Long currentUserId = 1L;

        Conversation conversation = Conversation.builder()
                .id(conversationId)
                .uuid(UUID.randomUUID().toString())
                .title("混合角色群聊")
                .ownerId(currentUserId)
                .status("active")
                .build();

        User admin = new User();
        admin.setUserId(2L);
        admin.setUsername("admin");

        User regularMember = new User();
        regularMember.setUserId(3L);
        regularMember.setUsername("member");

        Member ownerMember = Member.builder()
                .id(1L)
                .conversation(conversation)
                .user(testUser)
                .role("owner")
                .isActive(true)
                .build();

        Member adminMember = Member.builder()
                .id(2L)
                .conversation(conversation)
                .user(admin)
                .role("admin")
                .isActive(true)
                .build();

        Member normalMember = Member.builder()
                .id(3L)
                .conversation(conversation)
                .user(regularMember)
                .role("member")
                .isActive(true)
                .build();

        List<Member> allMembers = Arrays.asList(ownerMember, adminMember, normalMember);

        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(memberRepository.findByConversationIdAndIsActive(conversationId, true))
                .thenReturn(allMembers);
        when(conversationRepository.save(any(Conversation.class))).thenReturn(conversation);
        when(memberRepository.saveAll(anyList())).thenReturn(allMembers);

        // When
        conversationService.dissolveConversation(conversationId, currentUserId);

        // Then
        ArgumentCaptor<List<Member>> captor = ArgumentCaptor.forClass(List.class);
        verify(memberRepository).saveAll(captor.capture());
        List<Member> savedMembers = captor.getValue();

        assertEquals(3, savedMembers.size());
        // 验证所有角色的成员都被设置为不活跃
        assertTrue(savedMembers.stream().allMatch(m -> !m.getIsActive()));
        assertTrue(savedMembers.stream().allMatch(m -> m.getLeftAt() != null));
        assertTrue(savedMembers.stream().allMatch(m -> "群聊已解散".equals(m.getLeaveReason())));
    }

    // ==================== getMembers 方法测试 ====================

    @Test
    void testGetMembers_Success() {
        // Given
        Long conversationId = 1L;
        Long currentUserId = 1L;

        Conversation conversation = Conversation.builder()
                .id(conversationId)
                .uuid(UUID.randomUUID().toString())
                .title("测试群聊")
                .ownerId(currentUserId)
                .status("active")
                .build();

        User user2 = new User();
        user2.setUserId(2L);
        user2.setUsername("member1");
        user2.setEmail("member1@test.com");

        User user3 = new User();
        user3.setUserId(3L);
        user3.setUsername("member2");
        user3.setEmail("member2@test.com");

        LocalDateTime joinTime1 = LocalDateTime.now().minusDays(5);
        LocalDateTime joinTime2 = LocalDateTime.now().minusDays(3);
        LocalDateTime joinTime3 = LocalDateTime.now().minusDays(1);

        Member member1 = Member.builder()
                .id(1L)
                .conversation(conversation)
                .user(testUser)
                .role("owner")
                .joinedAt(joinTime1)
                .isActive(true)
                .build();

        Member member2 = Member.builder()
                .id(2L)
                .conversation(conversation)
                .user(user2)
                .role("member")
                .joinedAt(joinTime2)
                .isActive(true)
                .build();

        Member member3 = Member.builder()
                .id(3L)
                .conversation(conversation)
                .user(user3)
                .role("member")
                .joinedAt(joinTime3)
                .isActive(true)
                .build();

        List<Member> members = Arrays.asList(member1, member2, member3);

        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(memberRepository.existsByConversationIdAndUserIdAndIsActive(conversationId, currentUserId, true))
                .thenReturn(true);
        when(memberRepository.findByConversationIdAndIsActive(conversationId, true))
                .thenReturn(members);

        // When
        List<MemberResponse> result = conversationService.getMembers(conversationId, currentUserId);

        // Then
        assertNotNull(result, "结果不应为null");
        assertEquals(3, result.size(), "应该返回3个成员");

        // 验证第一个成员（群主）
        MemberResponse response1 = result.get(0);
        assertEquals(1L, response1.getUserId(), "用户ID应该匹配");
        assertEquals("testUser", response1.getUsername(), "用户名应该匹配");
        assertEquals("test@example.com", response1.getEmail(), "邮箱应该匹配");
        assertEquals("owner", response1.getRole(), "角色应该是owner");
        assertEquals(joinTime1, response1.getJoinedAt(), "加入时间应该匹配");

        // 验证第二个成员
        MemberResponse response2 = result.get(1);
        assertEquals(2L, response2.getUserId());
        assertEquals("member1", response2.getUsername());
        assertEquals("member1@test.com", response2.getEmail());
        assertEquals("member", response2.getRole());
        assertEquals(joinTime2, response2.getJoinedAt());

        // 验证第三个成员
        MemberResponse response3 = result.get(2);
        assertEquals(3L, response3.getUserId());
        assertEquals("member2", response3.getUsername());
        assertEquals("member2@test.com", response3.getEmail());
        assertEquals("member", response3.getRole());
        assertEquals(joinTime3, response3.getJoinedAt());

        // 验证方法调用
        verify(conversationRepository, times(1)).findById(conversationId);
        verify(memberRepository, times(1))
                .existsByConversationIdAndUserIdAndIsActive(conversationId, currentUserId, true);
        verify(memberRepository, times(1))
                .findByConversationIdAndIsActive(conversationId, true);
    }

    @Test
    void testGetMembers_ConversationNotFound_ThrowsException() {
        // Given
        Long conversationId = 999L;
        Long currentUserId = 1L;

        when(conversationRepository.findById(conversationId)).thenReturn(Optional.empty());

        // When & Then
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> conversationService.getMembers(conversationId, currentUserId),
                "群聊不存在应该抛出异常"
        );

        assertEquals(ErrorCode.NOT_FOUND_ERROR.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("群聊不存在"));
        verify(memberRepository, never())
                .existsByConversationIdAndUserIdAndIsActive(anyLong(), anyLong(), anyBoolean());
    }

    @Test
    void testGetMembers_ConversationIsDissolved_ThrowsException() {
        // Given
        Long conversationId = 1L;
        Long currentUserId = 1L;

        Conversation conversation = Conversation.builder()
                .id(conversationId)
                .uuid(UUID.randomUUID().toString())
                .title("已解散群聊")
                .ownerId(currentUserId)
                .status("dissolved")
                .dissolvedAt(LocalDateTime.now())
                .build();

        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));

        // When & Then
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> conversationService.getMembers(conversationId, currentUserId),
                "群聊已解散应该抛出异常"
        );

        assertEquals(ErrorCode.OPERATION_ERROR.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("群聊已解散"));
        verify(memberRepository, never())
                .existsByConversationIdAndUserIdAndIsActive(anyLong(), anyLong(), anyBoolean());
    }

    @Test
    void testGetMembers_UserNotInConversation_ThrowsException() {
        // Given
        Long conversationId = 1L;
        Long currentUserId = 999L; // 不在群聊中的用户

        Conversation conversation = Conversation.builder()
                .id(conversationId)
                .uuid(UUID.randomUUID().toString())
                .title("测试群聊")
                .ownerId(1L)
                .status("active")
                .build();

        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(memberRepository.existsByConversationIdAndUserIdAndIsActive(conversationId, currentUserId, true))
                .thenReturn(false);

        // When & Then
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> conversationService.getMembers(conversationId, currentUserId),
                "不在群聊中的用户应该无法查看成员"
        );

        assertEquals(ErrorCode.NO_AUTH_ERROR.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("您不在该群聊中"));
        verify(memberRepository, never())
                .findByConversationIdAndIsActive(anyLong(), anyBoolean());
    }

    @Test
    void testGetMembers_NoActiveMembers_ReturnsEmptyList() {
        // Given
        Long conversationId = 1L;
        Long currentUserId = 1L;

        Conversation conversation = Conversation.builder()
                .id(conversationId)
                .uuid(UUID.randomUUID().toString())
                .title("无活跃成员群聊")
                .ownerId(currentUserId)
                .status("active")
                .build();

        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(memberRepository.existsByConversationIdAndUserIdAndIsActive(conversationId, currentUserId, true))
                .thenReturn(true);
        when(memberRepository.findByConversationIdAndIsActive(conversationId, true))
                .thenReturn(Collections.emptyList());

        // When
        List<MemberResponse> result = conversationService.getMembers(conversationId, currentUserId);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty(), "应该返回空列表");
        assertEquals(0, result.size());
    }

    @Test
    void testGetMembers_WithSingleMember_Success() {
        // Given - 只有一个成员（群主）
        Long conversationId = 1L;
        Long currentUserId = 1L;

        Conversation conversation = Conversation.builder()
                .id(conversationId)
                .uuid(UUID.randomUUID().toString())
                .title("单人群聊")
                .ownerId(currentUserId)
                .status("active")
                .build();

        Member ownerMember = Member.builder()
                .id(1L)
                .conversation(conversation)
                .user(testUser)
                .role("owner")
                .joinedAt(LocalDateTime.now())
                .isActive(true)
                .build();

        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(memberRepository.existsByConversationIdAndUserIdAndIsActive(conversationId, currentUserId, true))
                .thenReturn(true);
        when(memberRepository.findByConversationIdAndIsActive(conversationId, true))
                .thenReturn(Collections.singletonList(ownerMember));

        // When
        List<MemberResponse> result = conversationService.getMembers(conversationId, currentUserId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("owner", result.get(0).getRole());
    }

    @Test
    void testGetMembers_WithDifferentRoles_Success() {
        // Given - 测试不同角色的成员
        Long conversationId = 1L;
        Long currentUserId = 1L;

        Conversation conversation = Conversation.builder()
                .id(conversationId)
                .uuid(UUID.randomUUID().toString())
                .title("多角色群聊")
                .ownerId(currentUserId)
                .status("active")
                .build();

        User admin = new User();
        admin.setUserId(2L);
        admin.setUsername("admin");
        admin.setEmail("admin@test.com");

        User member = new User();
        member.setUserId(3L);
        member.setUsername("member");
        member.setEmail("member@test.com");

        Member ownerMember = Member.builder()
                .id(1L)
                .conversation(conversation)
                .user(testUser)
                .role("owner")
                .joinedAt(LocalDateTime.now())
                .isActive(true)
                .build();

        Member adminMember = Member.builder()
                .id(2L)
                .conversation(conversation)
                .user(admin)
                .role("admin")
                .joinedAt(LocalDateTime.now())
                .isActive(true)
                .build();

        Member regularMember = Member.builder()
                .id(3L)
                .conversation(conversation)
                .user(member)
                .role("member")
                .joinedAt(LocalDateTime.now())
                .isActive(true)
                .build();

        List<Member> members = Arrays.asList(ownerMember, adminMember, regularMember);

        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(memberRepository.existsByConversationIdAndUserIdAndIsActive(conversationId, currentUserId, true))
                .thenReturn(true);
        when(memberRepository.findByConversationIdAndIsActive(conversationId, true))
                .thenReturn(members);

        // When
        List<MemberResponse> result = conversationService.getMembers(conversationId, currentUserId);

        // Then
        assertEquals(3, result.size());

        // 验证包含所有角色
        long ownerCount = result.stream().filter(r -> "owner".equals(r.getRole())).count();
        long adminCount = result.stream().filter(r -> "admin".equals(r.getRole())).count();
        long memberCount = result.stream().filter(r -> "member".equals(r.getRole())).count();

        assertEquals(1, ownerCount, "应该有1个群主");
        assertEquals(1, adminCount, "应该有1个管理员");
        assertEquals(1, memberCount, "应该有1个普通成员");
    }

    @Test
    void testGetMembers_RegularMemberCanView_Success() {
        // Given - 普通成员也能查看成员列表
        Long conversationId = 1L;
        Long currentUserId = 2L; // 普通成员

        User owner = new User();
        owner.setUserId(1L);
        owner.setUsername("owner");
        owner.setEmail("owner@test.com");

        Conversation conversation = Conversation.builder()
                .id(conversationId)
                .uuid(UUID.randomUUID().toString())
                .title("测试群聊")
                .ownerId(1L)
                .status("active")
                .build();

        Member ownerMember = Member.builder()
                .id(1L)
                .conversation(conversation)
                .user(owner)
                .role("owner")
                .joinedAt(LocalDateTime.now())
                .isActive(true)
                .build();

        Member regularMember = Member.builder()
                .id(2L)
                .conversation(conversation)
                .user(testUser)
                .role("member")
                .joinedAt(LocalDateTime.now())
                .isActive(true)
                .build();

        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(memberRepository.existsByConversationIdAndUserIdAndIsActive(conversationId, currentUserId, true))
                .thenReturn(true);
        when(memberRepository.findByConversationIdAndIsActive(conversationId, true))
                .thenReturn(Arrays.asList(ownerMember, regularMember));

        // When
        List<MemberResponse> result = conversationService.getMembers(conversationId, currentUserId);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size(), "普通成员也能查看所有成员");
    }

    @Test
    void testGetMembers_VerifyResponseFields() {
        // Given - 验证返回的DTO所有字段
        Long conversationId = 1L;
        Long currentUserId = 1L;
        LocalDateTime joinTime = LocalDateTime.of(2024, 1, 15, 10, 30, 0);

        Conversation conversation = Conversation.builder()
                .id(conversationId)
                .uuid(UUID.randomUUID().toString())
                .title("字段测试")
                .ownerId(currentUserId)
                .status("active")
                .build();

        User testUserWithDetails = new User();
        testUserWithDetails.setUserId(100L);
        testUserWithDetails.setUsername("detailedUser");
        testUserWithDetails.setEmail("detailed@test.com");

        Member member = Member.builder()
                .id(1L)
                .conversation(conversation)
                .user(testUserWithDetails)
                .role("admin")
                .joinedAt(joinTime)
                .isActive(true)
                .build();

        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(memberRepository.existsByConversationIdAndUserIdAndIsActive(conversationId, currentUserId, true))
                .thenReturn(true);
        when(memberRepository.findByConversationIdAndIsActive(conversationId, true))
                .thenReturn(Collections.singletonList(member));

        // When
        List<MemberResponse> result = conversationService.getMembers(conversationId, currentUserId);

        // Then
        assertEquals(1, result.size());
        MemberResponse response = result.get(0);

        // 详细验证每个字段
        assertEquals(100L, response.getUserId(), "用户ID应该匹配");
        assertEquals("detailedUser", response.getUsername(), "用户名应该匹配");
        assertEquals("detailed@test.com", response.getEmail(), "邮箱应该匹配");
        assertEquals("admin", response.getRole(), "角色应该匹配");
        assertEquals(joinTime, response.getJoinedAt(), "加入时间应该匹配");
        assertNotNull(response.getJoinedAt(), "加入时间不应为null");
    }

    @Test
    void testGetMembers_OnlyActiveMembers_Success() {
        // Given - 只返回活跃成员，不返回已退出的成员
        Long conversationId = 1L;
        Long currentUserId = 1L;

        Conversation conversation = Conversation.builder()
                .id(conversationId)
                .uuid(UUID.randomUUID().toString())
                .title("测试活跃成员")
                .ownerId(currentUserId)
                .status("active")
                .build();

        User user2 = new User();
        user2.setUserId(2L);
        user2.setUsername("activeUser");
        user2.setEmail("active@test.com");

        // 活跃成员
        Member activeMember1 = Member.builder()
                .id(1L)
                .conversation(conversation)
                .user(testUser)
                .role("owner")
                .isActive(true)
                .joinedAt(LocalDateTime.now())
                .build();

        Member activeMember2 = Member.builder()
                .id(2L)
                .conversation(conversation)
                .user(user2)
                .role("member")
                .isActive(true)
                .joinedAt(LocalDateTime.now())
                .build();

        // 只返回活跃成员（已退出的成员不会被查询出来）
        List<Member> activeMembers = Arrays.asList(activeMember1, activeMember2);

        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(memberRepository.existsByConversationIdAndUserIdAndIsActive(conversationId, currentUserId, true))
                .thenReturn(true);
        when(memberRepository.findByConversationIdAndIsActive(conversationId, true))
                .thenReturn(activeMembers);

        // When
        List<MemberResponse> result = conversationService.getMembers(conversationId, currentUserId);

        // Then
        assertEquals(2, result.size(), "应该只返回2个活跃成员");
        assertTrue(result.stream().allMatch(r -> r.getUserId() <= 2L),
                "不应该包含已退出的成员");
    }

    // ==================== addMembers 方法测试 ====================

    @Test
    void testAddMembers_Success() {
        // Given
        Long conversationId = 1L;
        Long currentUserId = 1L;
        List<Long> userIds = Arrays.asList(2L, 3L);

        Conversation conversation = Conversation.builder()
                .id(conversationId)
                .uuid(UUID.randomUUID().toString())
                .title("测试群聊")
                .ownerId(currentUserId)
                .status("active")
                .build();

        Member currentUserMember = Member.builder()
                .id(1L)
                .conversation(conversation)
                .user(testUser)
                .role("owner")
                .isActive(true)
                .build();

        User user2 = new User();
        user2.setUserId(2L);
        user2.setUsername("friend1");
        user2.setEmail("friend1@test.com");

        User user3 = new User();
        user3.setUserId(3L);
        user3.setUsername("friend2");
        user3.setEmail("friend2@test.com");

        Friendship friendship1 = new Friendship();
        friendship1.setId(1L);
        friendship1.setUserId(currentUserId);
        friendship1.setFriendId(2L);
        friendship1.setIsActive(true);

        Friendship friendship2 = new Friendship();
        friendship2.setId(2L);
        friendship2.setUserId(currentUserId);
        friendship2.setFriendId(3L);
        friendship2.setIsActive(true);

        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(memberRepository.findByConversationIdAndUserIdAndIsActive(conversationId, currentUserId, true))
                .thenReturn(Optional.of(currentUserMember));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));
        when(userRepository.findById(3L)).thenReturn(Optional.of(user3));
        when(friendshipRepository.findByUserIdAndFriendIdAndIsActive(currentUserId, 2L, true))
                .thenReturn(Optional.of(friendship1));
        when(friendshipRepository.findByUserIdAndFriendIdAndIsActive(currentUserId, 3L, true))
                .thenReturn(Optional.of(friendship2));
        when(memberRepository.findByConversationIdAndUserIdAndIsActive(conversationId, 2L, true))
                .thenReturn(Optional.empty());
        when(memberRepository.findByConversationIdAndUserIdAndIsActive(conversationId, 3L, true))
                .thenReturn(Optional.empty());
        when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        conversationService.addMembers(conversationId, userIds, currentUserId);

        // Then
        verify(conversationRepository, times(1)).findById(conversationId);
        verify(memberRepository, times(1))
                .findByConversationIdAndUserIdAndIsActive(conversationId, currentUserId, true);
        verify(userRepository, times(1)).findById(2L);
        verify(userRepository, times(1)).findById(3L);
        verify(friendshipRepository, times(1)).findByUserIdAndFriendIdAndIsActive(currentUserId, 2L, true);
        verify(friendshipRepository, times(1)).findByUserIdAndFriendIdAndIsActive(currentUserId, 3L, true);
        verify(memberRepository, times(2)).save(any(Member.class));

        // 验证保存的Member
        ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);
        verify(memberRepository, times(2)).save(memberCaptor.capture());
        List<Member> savedMembers = memberCaptor.getAllValues();

        assertEquals(2, savedMembers.size(), "应该保存2个新成员");
        for (Member member : savedMembers) {
            assertEquals("member", member.getRole(), "角色应该是member");
            assertTrue(member.getIsActive(), "应该是活跃状态");
            assertNotNull(member.getJoinedAt(), "加入时间不应为null");
            assertEquals(conversation, member.getConversation(), "群聊应该匹配");
        }
    }

    @Test
    void testAddMembers_ConversationNotFound_ThrowsException() {
        // Given
        Long conversationId = 999L;
        Long currentUserId = 1L;
        List<Long> userIds = Arrays.asList(2L);

        when(conversationRepository.findById(conversationId)).thenReturn(Optional.empty());

        // When & Then
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> conversationService.addMembers(conversationId, userIds, currentUserId),
                "群聊不存在应该抛出异常"
        );

        assertEquals(ErrorCode.NOT_FOUND_ERROR.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("群聊不存在"));
        verify(memberRepository, never())
                .findByConversationIdAndUserIdAndIsActive(anyLong(), anyLong(), anyBoolean());
    }

    @Test
    void testAddMembers_ConversationIsDissolved_ThrowsException() {
        // Given
        Long conversationId = 1L;
        Long currentUserId = 1L;
        List<Long> userIds = Arrays.asList(2L);

        Conversation conversation = Conversation.builder()
                .id(conversationId)
                .uuid(UUID.randomUUID().toString())
                .title("已解散群聊")
                .ownerId(currentUserId)
                .status("dissolved")
                .dissolvedAt(LocalDateTime.now())
                .build();

        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));

        // When & Then
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> conversationService.addMembers(conversationId, userIds, currentUserId),
                "群聊已解散应该抛出异常"
        );

        assertEquals(ErrorCode.OPERATION_ERROR.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("群聊已解散"));
        verify(memberRepository, never())
                .findByConversationIdAndUserIdAndIsActive(anyLong(), anyLong(), anyBoolean());
    }

    @Test
    void testAddMembers_CurrentUserNotMember_ThrowsException() {
        // Given
        Long conversationId = 1L;
        Long currentUserId = 999L; // 不是群成员
        List<Long> userIds = Arrays.asList(2L);

        Conversation conversation = Conversation.builder()
                .id(conversationId)
                .uuid(UUID.randomUUID().toString())
                .title("测试群聊")
                .ownerId(1L)
                .status("active")
                .build();

        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(memberRepository.findByConversationIdAndUserIdAndIsActive(conversationId, currentUserId, true))
                .thenReturn(Optional.empty());

        // When & Then
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> conversationService.addMembers(conversationId, userIds, currentUserId),
                "非群成员不能添加新成员"
        );

        assertEquals(ErrorCode.NO_AUTH_ERROR.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("您不是群成员"));
        verify(userRepository, never()).findById(anyLong());
    }

    @Test
    void testAddMembers_UserNotFound_ThrowsException() {
        // Given
        Long conversationId = 1L;
        Long currentUserId = 1L;
        List<Long> userIds = Arrays.asList(999L);

        Conversation conversation = Conversation.builder()
                .id(conversationId)
                .uuid(UUID.randomUUID().toString())
                .title("测试群聊")
                .ownerId(currentUserId)
                .status("active")
                .build();

        Member currentUserMember = Member.builder()
                .id(1L)
                .conversation(conversation)
                .user(testUser)
                .role("owner")
                .isActive(true)
                .build();

        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(memberRepository.findByConversationIdAndUserIdAndIsActive(conversationId, currentUserId, true))
                .thenReturn(Optional.of(currentUserMember));
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> conversationService.addMembers(conversationId, userIds, currentUserId),
                "用户不存在应该抛出异常"
        );

        assertEquals(ErrorCode.NOT_FOUND_ERROR.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("用户不存在"));
        verify(friendshipRepository, never())
                .findByUserIdAndFriendIdAndIsActive(anyLong(), anyLong(), anyBoolean());
    }

    @Test
    void testAddMembers_NotFriend_ThrowsException() {
        // Given
        Long conversationId = 1L;
        Long currentUserId = 1L;
        List<Long> userIds = Arrays.asList(2L);

        Conversation conversation = Conversation.builder()
                .id(conversationId)
                .uuid(UUID.randomUUID().toString())
                .title("测试群聊")
                .ownerId(currentUserId)
                .status("active")
                .build();

        Member currentUserMember = Member.builder()
                .id(1L)
                .conversation(conversation)
                .user(testUser)
                .role("owner")
                .isActive(true)
                .build();

        User user2 = new User();
        user2.setUserId(2L);
        user2.setUsername("notFriend");
        user2.setEmail("notfriend@test.com");

        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(memberRepository.findByConversationIdAndUserIdAndIsActive(conversationId, currentUserId, true))
                .thenReturn(Optional.of(currentUserMember));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));
        when(friendshipRepository.findByUserIdAndFriendIdAndIsActive(currentUserId, 2L, true))
                .thenReturn(Optional.empty()); // 不是好友

        // When & Then
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> conversationService.addMembers(conversationId, userIds, currentUserId),
                "只能添加好友应该抛出异常"
        );

        assertEquals(ErrorCode.OPERATION_ERROR.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("只能邀请好友加入群聊"));
        assertTrue(exception.getMessage().contains(user2.getUsername()));
        verify(memberRepository, never()).save(any(Member.class));
    }

    @Test
    void testAddMembers_UserAlreadyMember_SkipsUser() {
        // Given
        Long conversationId = 1L;
        Long currentUserId = 1L;
        List<Long> userIds = Arrays.asList(2L);

        Conversation conversation = Conversation.builder()
                .id(conversationId)
                .uuid(UUID.randomUUID().toString())
                .title("测试群聊")
                .ownerId(currentUserId)
                .status("active")
                .build();

        Member currentUserMember = Member.builder()
                .id(1L)
                .conversation(conversation)
                .user(testUser)
                .role("owner")
                .isActive(true)
                .build();

        User user2 = new User();
        user2.setUserId(2L);
        user2.setUsername("existingMember");
        user2.setEmail("existing@test.com");

        Member existingMember = Member.builder()
                .id(2L)
                .conversation(conversation)
                .user(user2)
                .role("member")
                .isActive(true)
                .build();

        Friendship friendship = new Friendship();
        friendship.setUserId(currentUserId);
        friendship.setFriendId(2L);
        friendship.setIsActive(true);

        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(memberRepository.findByConversationIdAndUserIdAndIsActive(conversationId, currentUserId, true))
                .thenReturn(Optional.of(currentUserMember));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));
        when(friendshipRepository.findByUserIdAndFriendIdAndIsActive(currentUserId, 2L, true))
                .thenReturn(Optional.of(friendship));
        when(memberRepository.findByConversationIdAndUserIdAndIsActive(conversationId, 2L, true))
                .thenReturn(Optional.of(existingMember)); // 已经是成员

        // When
        conversationService.addMembers(conversationId, userIds, currentUserId);

        // Then
        verify(memberRepository, never()).save(any(Member.class)); // 不应该保存
    }

    @Test
    void testAddMembers_WithSingleUser_Success() {
        // Given
        Long conversationId = 1L;
        Long currentUserId = 1L;
        List<Long> userIds = Collections.singletonList(2L);

        Conversation conversation = Conversation.builder()
                .id(conversationId)
                .uuid(UUID.randomUUID().toString())
                .title("测试群聊")
                .ownerId(currentUserId)
                .status("active")
                .build();

        Member currentUserMember = Member.builder()
                .id(1L)
                .conversation(conversation)
                .user(testUser)
                .role("owner")
                .isActive(true)
                .build();

        User user2 = new User();
        user2.setUserId(2L);
        user2.setUsername("newMember");
        user2.setEmail("new@test.com");

        Friendship friendship = new Friendship();
        friendship.setUserId(currentUserId);
        friendship.setFriendId(2L);
        friendship.setIsActive(true);

        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(memberRepository.findByConversationIdAndUserIdAndIsActive(conversationId, currentUserId, true))
                .thenReturn(Optional.of(currentUserMember));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));
        when(friendshipRepository.findByUserIdAndFriendIdAndIsActive(currentUserId, 2L, true))
                .thenReturn(Optional.of(friendship));
        when(memberRepository.findByConversationIdAndUserIdAndIsActive(conversationId, 2L, true))
                .thenReturn(Optional.empty());
        when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        conversationService.addMembers(conversationId, userIds, currentUserId);

        // Then
        verify(memberRepository, times(1)).save(any(Member.class));
    }

    @Test
    void testAddMembers_RegularMemberCanAddFriends_Success() {
        // Given - 普通成员也能添加好友
        Long conversationId = 1L;
        Long currentUserId = 2L; // 普通成员

        Conversation conversation = Conversation.builder()
                .id(conversationId)
                .uuid(UUID.randomUUID().toString())
                .title("测试群聊")
                .ownerId(1L)
                .status("active")
                .build();

        Member currentUserMember = Member.builder()
                .id(2L)
                .conversation(conversation)
                .user(testUser)
                .role("member") // 普通成员
                .isActive(true)
                .build();

        User user3 = new User();
        user3.setUserId(3L);
        user3.setUsername("friend");
        user3.setEmail("friend@test.com");

        Friendship friendship = new Friendship();
        friendship.setUserId(currentUserId);
        friendship.setFriendId(3L);
        friendship.setIsActive(true);

        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(memberRepository.findByConversationIdAndUserIdAndIsActive(conversationId, currentUserId, true))
                .thenReturn(Optional.of(currentUserMember));
        when(userRepository.findById(3L)).thenReturn(Optional.of(user3));
        when(friendshipRepository.findByUserIdAndFriendIdAndIsActive(currentUserId, 3L, true))
                .thenReturn(Optional.of(friendship));
        when(memberRepository.findByConversationIdAndUserIdAndIsActive(conversationId, 3L, true))
                .thenReturn(Optional.empty());
        when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        conversationService.addMembers(conversationId, Collections.singletonList(3L), currentUserId);

        // Then
        verify(memberRepository, times(1)).save(any(Member.class));
    }

    @Test
    void testAddMembers_VerifyMemberFields() {
        // Given - 验证新成员的字段
        Long conversationId = 1L;
        Long currentUserId = 1L;
        List<Long> userIds = Collections.singletonList(2L);
        LocalDateTime beforeCall = LocalDateTime.now();

        Conversation conversation = Conversation.builder()
                .id(conversationId)
                .uuid(UUID.randomUUID().toString())
                .title("测试群聊")
                .ownerId(currentUserId)
                .status("active")
                .build();

        Member currentUserMember = Member.builder()
                .id(1L)
                .conversation(conversation)
                .user(testUser)
                .role("owner")
                .isActive(true)
                .build();

        User user2 = new User();
        user2.setUserId(2L);
        user2.setUsername("newMember");
        user2.setEmail("new@test.com");

        Friendship friendship = new Friendship();
        friendship.setUserId(currentUserId);
        friendship.setFriendId(2L);
        friendship.setIsActive(true);

        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(memberRepository.findByConversationIdAndUserIdAndIsActive(conversationId, currentUserId, true))
                .thenReturn(Optional.of(currentUserMember));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));
        when(friendshipRepository.findByUserIdAndFriendIdAndIsActive(currentUserId, 2L, true))
                .thenReturn(Optional.of(friendship));
        when(memberRepository.findByConversationIdAndUserIdAndIsActive(conversationId, 2L, true))
                .thenReturn(Optional.empty());
        when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        conversationService.addMembers(conversationId, userIds, currentUserId);
        LocalDateTime afterCall = LocalDateTime.now();

        // Then
        ArgumentCaptor<Member> captor = ArgumentCaptor.forClass(Member.class);
        verify(memberRepository).save(captor.capture());
        Member savedMember = captor.getValue();

        assertEquals(conversation, savedMember.getConversation(), "群聊应该匹配");
        assertEquals(user2, savedMember.getUser(), "用户应该匹配");
        assertEquals("member", savedMember.getRole(), "角色应该是member");
        assertTrue(savedMember.getIsActive(), "应该是活跃状态");
        assertNotNull(savedMember.getJoinedAt(), "加入时间不应为null");
        assertTrue(
                !savedMember.getJoinedAt().isBefore(beforeCall) && !savedMember.getJoinedAt().isAfter(afterCall),
                "加入时间应该在方法调用期间"
        );
    }

    @Test
    void testAddMembers_WithMixedResults_PartialSuccess() {
        // Given - 部分用户已是成员，部分是新用户
        Long conversationId = 1L;
        Long currentUserId = 1L;
        List<Long> userIds = Arrays.asList(2L, 3L);

        Conversation conversation = Conversation.builder()
                .id(conversationId)
                .uuid(UUID.randomUUID().toString())
                .title("测试群聊")
                .ownerId(currentUserId)
                .status("active")
                .build();

        Member currentUserMember = Member.builder()
                .id(1L)
                .conversation(conversation)
                .user(testUser)
                .role("owner")
                .isActive(true)
                .build();

        User user2 = new User();
        user2.setUserId(2L);
        user2.setUsername("existing");
        user2.setEmail("existing@test.com");

        User user3 = new User();
        user3.setUserId(3L);
        user3.setUsername("newMember");
        user3.setEmail("new@test.com");

        Member existingMember = Member.builder()
                .id(2L)
                .conversation(conversation)
                .user(user2)
                .role("member")
                .isActive(true)
                .build();

        Friendship friendship1 = new Friendship();
        friendship1.setUserId(currentUserId);
        friendship1.setFriendId(2L);
        friendship1.setIsActive(true);

        Friendship friendship2 = new Friendship();
        friendship2.setUserId(currentUserId);
        friendship2.setFriendId(3L);
        friendship2.setIsActive(true);

        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(memberRepository.findByConversationIdAndUserIdAndIsActive(conversationId, currentUserId, true))
                .thenReturn(Optional.of(currentUserMember));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));
        when(userRepository.findById(3L)).thenReturn(Optional.of(user3));
        when(friendshipRepository.findByUserIdAndFriendIdAndIsActive(currentUserId, 2L, true))
                .thenReturn(Optional.of(friendship1));
        when(friendshipRepository.findByUserIdAndFriendIdAndIsActive(currentUserId, 3L, true))
                .thenReturn(Optional.of(friendship2));
        when(memberRepository.findByConversationIdAndUserIdAndIsActive(conversationId, 2L, true))
                .thenReturn(Optional.of(existingMember)); // 已是成员
        when(memberRepository.findByConversationIdAndUserIdAndIsActive(conversationId, 3L, true))
                .thenReturn(Optional.empty()); // 新成员
        when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        conversationService.addMembers(conversationId, userIds, currentUserId);

        // Then
        verify(memberRepository, times(1)).save(any(Member.class)); // 只保存1个新成员
    }

    @Test
    void testAddMembers_WithEmptyList_NoOperation() {
        // Given - 空的用户列表
        Long conversationId = 1L;
        Long currentUserId = 1L;
        List<Long> userIds = Collections.emptyList();

        Conversation conversation = Conversation.builder()
                .id(conversationId)
                .uuid(UUID.randomUUID().toString())
                .title("测试群聊")
                .ownerId(currentUserId)
                .status("active")
                .build();

        Member currentUserMember = Member.builder()
                .id(1L)
                .conversation(conversation)
                .user(testUser)
                .role("owner")
                .isActive(true)
                .build();

        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(memberRepository.findByConversationIdAndUserIdAndIsActive(conversationId, currentUserId, true))
                .thenReturn(Optional.of(currentUserMember));

        // When
        conversationService.addMembers(conversationId, userIds, currentUserId);

        // Then
        verify(userRepository, never()).findById(anyLong());
        verify(memberRepository, never()).save(any(Member.class));
    }
}
