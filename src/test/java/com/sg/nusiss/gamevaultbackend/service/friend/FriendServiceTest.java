package com.sg.nusiss.gamevaultbackend.service.friend;

import com.sg.nusiss.gamevaultbackend.common.ErrorCode;
import com.sg.nusiss.gamevaultbackend.dto.friend.response.FriendRequestResponse;
import com.sg.nusiss.gamevaultbackend.dto.friend.response.FriendResponse;
import com.sg.nusiss.gamevaultbackend.dto.friend.response.UserSearchResponse;
import com.sg.nusiss.gamevaultbackend.entity.auth.User;
import com.sg.nusiss.gamevaultbackend.entity.friend.FriendRequest;
import com.sg.nusiss.gamevaultbackend.entity.friend.Friendship;
import com.sg.nusiss.gamevaultbackend.exception.BusinessException;
import com.sg.nusiss.gamevaultbackend.repository.auth.UserRepository;
import com.sg.nusiss.gamevaultbackend.repository.friend.FriendRequestRepository;
import com.sg.nusiss.gamevaultbackend.repository.friend.FriendshipRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * @ClassName FriendServiceTest
 * @Author HUANG ZHENJIA
 * @Date 2025/10/11
 * @Description
 */

@ExtendWith(MockitoExtension.class)
public class FriendServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private FriendRequestRepository friendRequestRepository;

    @Mock
    private FriendshipRepository friendshipRepository;

    @InjectMocks
    private FriendService friendService;

    private User testUser;
    private User user2;
    private User user3;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUserId(1L);
        testUser.setUsername("testUser");
        testUser.setEmail("test@example.com");

        user2 = new User();
        user2.setUserId(2L);
        user2.setUsername("user2");
        user2.setEmail("user2@example.com");

        user3 = new User();
        user3.setUserId(3L);
        user3.setUsername("user3");
        user3.setEmail("user3@example.com");
    }

// ==================== searchUsers 方法测试 ====================

    @Test
    void testSearchUsers_Success() {
        // Given
        String keyword = "user";
        Long currentUserId = 1L;

        // 只返回 user2 和 user3，不包含 testUser（避免不必要的 mock）
        List<User> users = Arrays.asList(user2, user3);

        when(userRepository.searchUsers("user")).thenReturn(users);
        when(friendshipRepository.findByUserIdAndFriendIdAndIsActive(currentUserId, 2L, true))
                .thenReturn(Optional.of(new Friendship())); // user2 是好友
        when(friendshipRepository.findByUserIdAndFriendIdAndIsActive(currentUserId, 3L, true))
                .thenReturn(Optional.empty());
        when(friendRequestRepository.findExistingRequest(currentUserId, 2L, "pending"))
                .thenReturn(Optional.empty());
        when(friendRequestRepository.findExistingRequest(currentUserId, 3L, "pending"))
                .thenReturn(Optional.of(new FriendRequest())); // user3 有待处理请求

        // When
        List<UserSearchResponse> result = friendService.searchUsers(keyword, currentUserId);

        // Then
        assertNotNull(result, "结果不应为null");
        assertEquals(2, result.size(), "应该返回2个用户");

        // 验证user2（是好友）
        UserSearchResponse response1 = result.stream()
                .filter(r -> r.getUserId().equals(2L))
                .findFirst()
                .orElse(null);
        assertNotNull(response1);
        assertEquals("user2", response1.getUsername());
        assertTrue(response1.getIsFriend(), "user2应该是好友");
        assertFalse(response1.getHasPending(), "user2没有待处理请求");

        // 验证user3（有待处理请求）
        UserSearchResponse response2 = result.stream()
                .filter(r -> r.getUserId().equals(3L))
                .findFirst()
                .orElse(null);
        assertNotNull(response2);
        assertEquals("user3", response2.getUsername());
        assertFalse(response2.getIsFriend(), "user3不是好友");
        assertTrue(response2.getHasPending(), "user3有待处理请求");

        verify(userRepository, times(1)).searchUsers("user");
    }

    @Test
    void testSearchUsers_KeywordIsNull_ThrowsException() {
        // Given
        String keyword = null;
        Long currentUserId = 1L;

        // When & Then
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> friendService.searchUsers(keyword, currentUserId),
                "关键词为null应该抛出异常"
        );

        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("搜索关键词不能为空"));
        verify(userRepository, never()).searchUsers(anyString());
    }

    @Test
    void testSearchUsers_KeywordIsEmpty_ThrowsException() {
        // Given
        String keyword = "";
        Long currentUserId = 1L;

        // When & Then
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> friendService.searchUsers(keyword, currentUserId)
        );

        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("搜索关键词不能为空"));
    }

    @Test
    void testSearchUsers_KeywordIsBlank_ThrowsException() {
        // Given
        String keyword = "   ";
        Long currentUserId = 1L;

        // When & Then
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> friendService.searchUsers(keyword, currentUserId)
        );

        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("搜索关键词不能为空"));
    }

    @Test
    void testSearchUsers_NoResults_ReturnsEmptyList() {
        // Given
        String keyword = "nonexistent";
        Long currentUserId = 1L;

        when(userRepository.searchUsers("nonexistent")).thenReturn(Collections.emptyList());

        // When
        List<UserSearchResponse> result = friendService.searchUsers(keyword, currentUserId);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        assertEquals(0, result.size());
    }

    @Test
    void testSearchUsers_OnlySelfInResults_ReturnsEmpty() {
        // Given - 搜索结果只有自己
        String keyword = "testUser";
        Long currentUserId = 1L;

        when(userRepository.searchUsers("testUser")).thenReturn(Collections.singletonList(testUser));

        // When
        List<UserSearchResponse> result = friendService.searchUsers(keyword, currentUserId);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty(), "应该排除自己");
        // 不需要验证 friendship 和 friendRequest 的 mock，因为被过滤了
    }

    @Test
    void testSearchUsers_TrimsKeyword() {
        // Given - 关键词有空格
        String keyword = "  user  ";
        Long currentUserId = 1L;

        when(userRepository.searchUsers("user")).thenReturn(Collections.emptyList());

        // When
        friendService.searchUsers(keyword, currentUserId);

        // Then
        verify(userRepository, times(1)).searchUsers("user"); // 验证trim后的值
    }

    @Test
    void testSearchUsers_AllUsersAreFriends() {
        // Given - 所有搜索结果都是好友
        String keyword = "user";
        Long currentUserId = 1L;

        when(userRepository.searchUsers("user")).thenReturn(Arrays.asList(user2, user3));
        when(friendshipRepository.findByUserIdAndFriendIdAndIsActive(currentUserId, 2L, true))
                .thenReturn(Optional.of(new Friendship()));
        when(friendshipRepository.findByUserIdAndFriendIdAndIsActive(currentUserId, 3L, true))
                .thenReturn(Optional.of(new Friendship()));
        when(friendRequestRepository.findExistingRequest(currentUserId, 2L, "pending"))
                .thenReturn(Optional.empty());
        when(friendRequestRepository.findExistingRequest(currentUserId, 3L, "pending"))
                .thenReturn(Optional.empty());

        // When
        List<UserSearchResponse> result = friendService.searchUsers(keyword, currentUserId);

        // Then
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(UserSearchResponse::getIsFriend),
                "所有用户都应该是好友");
    }

    @Test
    void testSearchUsers_AllUsersHavePendingRequests() {
        // Given - 所有搜索结果都有待处理请求
        String keyword = "user";
        Long currentUserId = 1L;

        when(userRepository.searchUsers("user")).thenReturn(Arrays.asList(user2, user3));
        when(friendshipRepository.findByUserIdAndFriendIdAndIsActive(currentUserId, 2L, true))
                .thenReturn(Optional.empty());
        when(friendshipRepository.findByUserIdAndFriendIdAndIsActive(currentUserId, 3L, true))
                .thenReturn(Optional.empty());
        when(friendRequestRepository.findExistingRequest(currentUserId, 2L, "pending"))
                .thenReturn(Optional.of(new FriendRequest()));
        when(friendRequestRepository.findExistingRequest(currentUserId, 3L, "pending"))
                .thenReturn(Optional.of(new FriendRequest()));

        // When
        List<UserSearchResponse> result = friendService.searchUsers(keyword, currentUserId);

        // Then
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(UserSearchResponse::getHasPending),
                "所有用户都应该有待处理请求");
    }

    @Test
    void testSearchUsers_MixedResults_WithSelfExcluded() {
        // Given - 搜索结果包含自己，应该被排除
        String keyword = "user";
        Long currentUserId = 1L;

        when(userRepository.searchUsers("user")).thenReturn(Arrays.asList(testUser, user2, user3));
        when(friendshipRepository.findByUserIdAndFriendIdAndIsActive(currentUserId, 2L, true))
                .thenReturn(Optional.empty());
        when(friendshipRepository.findByUserIdAndFriendIdAndIsActive(currentUserId, 3L, true))
                .thenReturn(Optional.empty());
        when(friendRequestRepository.findExistingRequest(currentUserId, 2L, "pending"))
                .thenReturn(Optional.empty());
        when(friendRequestRepository.findExistingRequest(currentUserId, 3L, "pending"))
                .thenReturn(Optional.empty());

        // When
        List<UserSearchResponse> result = friendService.searchUsers(keyword, currentUserId);

        // Then
        assertEquals(2, result.size(), "应该排除自己，只返回2个用户");
        assertFalse(result.stream().anyMatch(r -> r.getUid().equals(1L)),
                "结果中不应该包含自己");
    }

    // ==================== sendFriendRequest 方法测试 ====================

    @Test
    void testSendFriendRequest_Success() {
        // Given
        Long fromUserId = 1L;
        Long toUserId = 2L;
        String message = "你好，我想加你为好友";

        when(userRepository.findById(toUserId)).thenReturn(Optional.of(user2));
        when(friendshipRepository.findByUserIdAndFriendIdAndIsActive(fromUserId, toUserId, true))
                .thenReturn(Optional.empty());
        when(friendRequestRepository.findExistingRequest(fromUserId, toUserId, "pending"))
                .thenReturn(Optional.empty());
        when(friendRequestRepository.save(any(FriendRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        friendService.sendFriendRequest(fromUserId, toUserId, message);

        // Then
        verify(userRepository, times(1)).findById(toUserId);
        verify(friendshipRepository, times(1))
                .findByUserIdAndFriendIdAndIsActive(fromUserId, toUserId, true);
        verify(friendRequestRepository, times(1))
                .findExistingRequest(fromUserId, toUserId, "pending");
        verify(friendRequestRepository, times(1)).save(any(FriendRequest.class));

        // 验证保存的 FriendRequest
        ArgumentCaptor<FriendRequest> captor = ArgumentCaptor.forClass(FriendRequest.class);
        verify(friendRequestRepository).save(captor.capture());
        FriendRequest savedRequest = captor.getValue();

        assertEquals(fromUserId, savedRequest.getFromUserId(), "fromUserId应该匹配");
        assertEquals(toUserId, savedRequest.getToUserId(), "toUserId应该匹配");
        assertEquals(message, savedRequest.getMessage(), "消息应该匹配");
        assertEquals("pending", savedRequest.getStatus(), "状态应该是pending");
    }

    @Test
    void testSendFriendRequest_WithNullMessage_Success() {
        // Given - 消息可以为null
        Long fromUserId = 1L;
        Long toUserId = 2L;
        String message = null;

        when(userRepository.findById(toUserId)).thenReturn(Optional.of(user2));
        when(friendshipRepository.findByUserIdAndFriendIdAndIsActive(fromUserId, toUserId, true))
                .thenReturn(Optional.empty());
        when(friendRequestRepository.findExistingRequest(fromUserId, toUserId, "pending"))
                .thenReturn(Optional.empty());
        when(friendRequestRepository.save(any(FriendRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        friendService.sendFriendRequest(fromUserId, toUserId, message);

        // Then
        verify(friendRequestRepository, times(1)).save(any(FriendRequest.class));

        ArgumentCaptor<FriendRequest> captor = ArgumentCaptor.forClass(FriendRequest.class);
        verify(friendRequestRepository).save(captor.capture());
        FriendRequest savedRequest = captor.getValue();

        assertNull(savedRequest.getMessage(), "消息可以为null");
    }

    @Test
    void testSendFriendRequest_WithEmptyMessage_Success() {
        // Given - 消息可以为空字符串
        Long fromUserId = 1L;
        Long toUserId = 2L;
        String message = "";

        when(userRepository.findById(toUserId)).thenReturn(Optional.of(user2));
        when(friendshipRepository.findByUserIdAndFriendIdAndIsActive(fromUserId, toUserId, true))
                .thenReturn(Optional.empty());
        when(friendRequestRepository.findExistingRequest(fromUserId, toUserId, "pending"))
                .thenReturn(Optional.empty());
        when(friendRequestRepository.save(any(FriendRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        friendService.sendFriendRequest(fromUserId, toUserId, message);

        // Then
        verify(friendRequestRepository, times(1)).save(any(FriendRequest.class));
    }

    @Test
    void testSendFriendRequest_ToUserNotFound_ThrowsException() {
        // Given
        Long fromUserId = 1L;
        Long toUserId = 999L;
        String message = "你好";

        when(userRepository.findById(toUserId)).thenReturn(Optional.empty());

        // When & Then
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> friendService.sendFriendRequest(fromUserId, toUserId, message),
                "目标用户不存在应该抛出异常"
        );

        assertEquals(ErrorCode.NOT_FOUND_ERROR.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("用户不存在"));
        verify(friendshipRepository, never())
                .findByUserIdAndFriendIdAndIsActive(anyLong(), anyLong(), anyBoolean());
        verify(friendRequestRepository, never()).save(any());
    }

    @Test
    void testSendFriendRequest_AddSelf_ThrowsException() {
        // Given - 不能添加自己为好友
        Long fromUserId = 1L;
        Long toUserId = 1L;
        String message = "添加自己";

        when(userRepository.findById(toUserId)).thenReturn(Optional.of(testUser));

        // When & Then
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> friendService.sendFriendRequest(fromUserId, toUserId, message),
                "不能添加自己为好友"
        );

        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("不能添加自己为好友"));
        verify(friendshipRepository, never())
                .findByUserIdAndFriendIdAndIsActive(anyLong(), anyLong(), anyBoolean());
        verify(friendRequestRepository, never()).save(any());
    }

    @Test
    void testSendFriendRequest_AlreadyFriends_ThrowsException() {
        // Given
        Long fromUserId = 1L;
        Long toUserId = 2L;
        String message = "你好";

        Friendship existingFriendship = new Friendship();
        existingFriendship.setUserId(fromUserId);
        existingFriendship.setFriendId(toUserId);
        existingFriendship.setIsActive(true);

        when(userRepository.findById(toUserId)).thenReturn(Optional.of(user2));
        when(friendshipRepository.findByUserIdAndFriendIdAndIsActive(fromUserId, toUserId, true))
                .thenReturn(Optional.of(existingFriendship));

        // When & Then
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> friendService.sendFriendRequest(fromUserId, toUserId, message),
                "已经是好友应该抛出异常"
        );

        assertEquals(ErrorCode.OPERATION_ERROR.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("已经是好友了"));
        verify(friendRequestRepository, never())
                .findExistingRequest(anyLong(), anyLong(), anyString());
        verify(friendRequestRepository, never()).save(any());
    }

    @Test
    void testSendFriendRequest_PendingRequestExists_ThrowsException() {
        // Given
        Long fromUserId = 1L;
        Long toUserId = 2L;
        String message = "你好";

        FriendRequest existingRequest = new FriendRequest();
        existingRequest.setId(1L);
        existingRequest.setFromUserId(fromUserId);
        existingRequest.setToUserId(toUserId);
        existingRequest.setStatus("pending");

        when(userRepository.findById(toUserId)).thenReturn(Optional.of(user2));
        when(friendshipRepository.findByUserIdAndFriendIdAndIsActive(fromUserId, toUserId, true))
                .thenReturn(Optional.empty());
        when(friendRequestRepository.findExistingRequest(fromUserId, toUserId, "pending"))
                .thenReturn(Optional.of(existingRequest));

        // When & Then
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> friendService.sendFriendRequest(fromUserId, toUserId, message),
                "已有待处理请求应该抛出异常"
        );

        assertEquals(ErrorCode.OPERATION_ERROR.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("已有待处理的好友请求"));
        verify(friendRequestRepository, never()).save(any());
    }

    @Test
    void testSendFriendRequest_WithLongMessage_Success() {
        // Given - 测试长消息
        Long fromUserId = 1L;
        Long toUserId = 2L;
        String message = "这是一条很长的消息".repeat(20); // 长消息

        when(userRepository.findById(toUserId)).thenReturn(Optional.of(user2));
        when(friendshipRepository.findByUserIdAndFriendIdAndIsActive(fromUserId, toUserId, true))
                .thenReturn(Optional.empty());
        when(friendRequestRepository.findExistingRequest(fromUserId, toUserId, "pending"))
                .thenReturn(Optional.empty());
        when(friendRequestRepository.save(any(FriendRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        friendService.sendFriendRequest(fromUserId, toUserId, message);

        // Then
        verify(friendRequestRepository, times(1)).save(any(FriendRequest.class));

        ArgumentCaptor<FriendRequest> captor = ArgumentCaptor.forClass(FriendRequest.class);
        verify(friendRequestRepository).save(captor.capture());
        FriendRequest savedRequest = captor.getValue();

        assertEquals(message, savedRequest.getMessage());
    }

    @Test
    void testSendFriendRequest_VerifyRequestFields() {
        // Given - 验证所有字段
        Long fromUserId = 1L;
        Long toUserId = 2L;
        String message = "请求验证";

        when(userRepository.findById(toUserId)).thenReturn(Optional.of(user2));
        when(friendshipRepository.findByUserIdAndFriendIdAndIsActive(fromUserId, toUserId, true))
                .thenReturn(Optional.empty());
        when(friendRequestRepository.findExistingRequest(fromUserId, toUserId, "pending"))
                .thenReturn(Optional.empty());
        when(friendRequestRepository.save(any(FriendRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        friendService.sendFriendRequest(fromUserId, toUserId, message);

        // Then
        ArgumentCaptor<FriendRequest> captor = ArgumentCaptor.forClass(FriendRequest.class);
        verify(friendRequestRepository).save(captor.capture());
        FriendRequest savedRequest = captor.getValue();

        // 详细验证所有字段
        assertNotNull(savedRequest, "保存的请求不应为null");
        assertEquals(fromUserId, savedRequest.getFromUserId(), "fromUserId应该正确");
        assertEquals(toUserId, savedRequest.getToUserId(), "toUserId应该正确");
        assertEquals(message, savedRequest.getMessage(), "消息应该正确");
        assertEquals("pending", savedRequest.getStatus(), "状态应该是pending");
        assertNull(savedRequest.getHandledAt(), "处理时间应该为null");
    }

    @Test
    void testSendFriendRequest_DifferentUsers_Success() {
        // Given - 不同用户之间发送请求
        Long fromUserId = 3L;
        Long toUserId = 2L;
        String message = "你好";

        when(userRepository.findById(toUserId)).thenReturn(Optional.of(user2));
        when(friendshipRepository.findByUserIdAndFriendIdAndIsActive(fromUserId, toUserId, true))
                .thenReturn(Optional.empty());
        when(friendRequestRepository.findExistingRequest(fromUserId, toUserId, "pending"))
                .thenReturn(Optional.empty());
        when(friendRequestRepository.save(any(FriendRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        friendService.sendFriendRequest(fromUserId, toUserId, message);

        // Then
        verify(friendRequestRepository, times(1)).save(any(FriendRequest.class));

        ArgumentCaptor<FriendRequest> captor = ArgumentCaptor.forClass(FriendRequest.class);
        verify(friendRequestRepository).save(captor.capture());
        FriendRequest savedRequest = captor.getValue();

        assertEquals(fromUserId, savedRequest.getFromUserId());
        assertEquals(toUserId, savedRequest.getToUserId());
    }

    // ==================== getReceivedRequests 方法测试 ====================

    @Test
    void testGetReceivedRequests_Success() {
        // Given
        Long userId = 1L;

        FriendRequest request1 = new FriendRequest();
        request1.setId(1L);
        request1.setFromUserId(2L);
        request1.setToUserId(userId);
        request1.setMessage("你好");
        request1.setStatus("pending");
        request1.setCreatedAt(LocalDateTime.now());

        FriendRequest request2 = new FriendRequest();
        request2.setId(2L);
        request2.setFromUserId(3L);
        request2.setToUserId(userId);
        request2.setMessage("加个好友");
        request2.setStatus("pending");
        request2.setCreatedAt(LocalDateTime.now());

        List<FriendRequest> requests = Arrays.asList(request1, request2);

        when(friendRequestRepository.findByToUserIdAndStatus(userId, "pending"))
                .thenReturn(requests);
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));
        when(userRepository.findById(3L)).thenReturn(Optional.of(user3));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        // When
        List<FriendRequestResponse> result = friendService.getReceivedRequests(userId);

        // Then
        assertNotNull(result, "结果不应为null");
        assertEquals(2, result.size(), "应该返回2个请求");

        // 验证第一个请求
        FriendRequestResponse response1 = result.get(0);
        assertEquals(1L, response1.getId());
        assertEquals(2L, response1.getFromUserId());
        assertEquals("user2", response1.getFromUsername());
        assertEquals(userId, response1.getToUserId());
        assertEquals("testUser", response1.getToUsername());
        assertEquals("你好", response1.getMessage());
        assertEquals("pending", response1.getStatus());

        // 验证第二个请求
        FriendRequestResponse response2 = result.get(1);
        assertEquals(2L, response2.getId());
        assertEquals(3L, response2.getFromUserId());
        assertEquals("user3", response2.getFromUsername());

        verify(friendRequestRepository, times(1)).findByToUserIdAndStatus(userId, "pending");
    }

    @Test
    void testGetReceivedRequests_NoRequests_ReturnsEmptyList() {
        // Given
        Long userId = 1L;

        when(friendRequestRepository.findByToUserIdAndStatus(userId, "pending"))
                .thenReturn(Collections.emptyList());

        // When
        List<FriendRequestResponse> result = friendService.getReceivedRequests(userId);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        assertEquals(0, result.size());
        verify(userRepository, never()).findById(anyLong());
    }

    @Test
    void testGetReceivedRequests_WithSingleRequest_Success() {
        // Given
        Long userId = 1L;

        FriendRequest request = new FriendRequest();
        request.setId(1L);
        request.setFromUserId(2L);
        request.setToUserId(userId);
        request.setMessage("单个请求");
        request.setStatus("pending");
        request.setCreatedAt(LocalDateTime.now());

        when(friendRequestRepository.findByToUserIdAndStatus(userId, "pending"))
                .thenReturn(Collections.singletonList(request));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        // When
        List<FriendRequestResponse> result = friendService.getReceivedRequests(userId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("单个请求", result.get(0).getMessage());
    }

    @Test
    void testGetReceivedRequests_WithNullMessage_Success() {
        // Given
        Long userId = 1L;

        FriendRequest request = new FriendRequest();
        request.setId(1L);
        request.setFromUserId(2L);
        request.setToUserId(userId);
        request.setMessage(null); // null 消息
        request.setStatus("pending");
        request.setCreatedAt(LocalDateTime.now());

        when(friendRequestRepository.findByToUserIdAndStatus(userId, "pending"))
                .thenReturn(Collections.singletonList(request));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        // When
        List<FriendRequestResponse> result = friendService.getReceivedRequests(userId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertNull(result.get(0).getMessage(), "消息可以为null");
    }

    // ==================== getSentRequests 方法测试 ====================

    @Test
    void testGetSentRequests_Success() {
        // Given
        Long userId = 1L;

        FriendRequest request1 = new FriendRequest();
        request1.setId(1L);
        request1.setFromUserId(userId);
        request1.setToUserId(2L);
        request1.setMessage("请求1");
        request1.setStatus("pending");
        request1.setCreatedAt(LocalDateTime.now());

        FriendRequest request2 = new FriendRequest();
        request2.setId(2L);
        request2.setFromUserId(userId);
        request2.setToUserId(3L);
        request2.setMessage("请求2");
        request2.setStatus("pending");
        request2.setCreatedAt(LocalDateTime.now());

        List<FriendRequest> requests = Arrays.asList(request1, request2);

        when(friendRequestRepository.findByFromUserIdAndStatus(userId, "pending"))
                .thenReturn(requests);
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));
        when(userRepository.findById(3L)).thenReturn(Optional.of(user3));

        // When
        List<FriendRequestResponse> result = friendService.getSentRequests(userId);

        // Then
        assertNotNull(result, "结果不应为null");
        assertEquals(2, result.size(), "应该返回2个请求");

        // 验证第一个请求
        FriendRequestResponse response1 = result.get(0);
        assertEquals(1L, response1.getId());
        assertEquals(userId, response1.getFromUserId());
        assertEquals("testUser", response1.getFromUsername());
        assertEquals(2L, response1.getToUserId());
        assertEquals("user2", response1.getToUsername());
        assertEquals("请求1", response1.getMessage());
        assertEquals("pending", response1.getStatus());

        // 验证第二个请求
        FriendRequestResponse response2 = result.get(1);
        assertEquals(2L, response2.getId());
        assertEquals(3L, response2.getToUserId());
        assertEquals("user3", response2.getToUsername());

        verify(friendRequestRepository, times(1)).findByFromUserIdAndStatus(userId, "pending");
    }

    @Test
    void testGetSentRequests_NoRequests_ReturnsEmptyList() {
        // Given
        Long userId = 1L;

        when(friendRequestRepository.findByFromUserIdAndStatus(userId, "pending"))
                .thenReturn(Collections.emptyList());

        // When
        List<FriendRequestResponse> result = friendService.getSentRequests(userId);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        assertEquals(0, result.size());
        verify(userRepository, never()).findById(anyLong());
    }

    @Test
    void testGetSentRequests_WithSingleRequest_Success() {
        // Given
        Long userId = 1L;

        FriendRequest request = new FriendRequest();
        request.setId(1L);
        request.setFromUserId(userId);
        request.setToUserId(2L);
        request.setMessage("单个发送请求");
        request.setStatus("pending");
        request.setCreatedAt(LocalDateTime.now());

        when(friendRequestRepository.findByFromUserIdAndStatus(userId, "pending"))
                .thenReturn(Collections.singletonList(request));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));

        // When
        List<FriendRequestResponse> result = friendService.getSentRequests(userId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("单个发送请求", result.get(0).getMessage());
    }

    @Test
    void testGetSentRequests_VerifyResponseFields() {
        // Given
        Long userId = 1L;
        LocalDateTime createdTime = LocalDateTime.now();

        FriendRequest request = new FriendRequest();
        request.setId(100L);
        request.setFromUserId(userId);
        request.setToUserId(2L);
        request.setMessage("验证字段");
        request.setStatus("pending");
        request.setCreatedAt(createdTime);
        request.setHandledAt(null);

        when(friendRequestRepository.findByFromUserIdAndStatus(userId, "pending"))
                .thenReturn(Collections.singletonList(request));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));

        // When
        List<FriendRequestResponse> result = friendService.getSentRequests(userId);

        // Then
        assertEquals(1, result.size());
        FriendRequestResponse response = result.get(0);

        // 详细验证所有字段
        assertEquals(100L, response.getId(), "请求ID应该匹配");
        assertEquals(userId, response.getFromUserId(), "fromUserId应该匹配");
        assertEquals("testUser", response.getFromUsername(), "fromUsername应该匹配");
        assertEquals("test@example.com", response.getFromEmail(), "fromEmail应该匹配");
        assertEquals(2L, response.getToUserId(), "toUserId应该匹配");
        assertEquals("user2", response.getToUsername(), "toUsername应该匹配");
        assertEquals("user2@example.com", response.getToEmail(), "toEmail应该匹配");
        assertEquals("验证字段", response.getMessage(), "消息应该匹配");
        assertEquals("pending", response.getStatus(), "状态应该匹配");
        assertEquals(createdTime, response.getCreatedAt(), "创建时间应该匹配");
        assertNull(response.getHandledAt(), "处理时间应该为null");
    }

    @Test
    void testGetReceivedRequests_VerifyResponseFields() {
        // Given
        Long userId = 1L;
        LocalDateTime createdTime = LocalDateTime.now();

        FriendRequest request = new FriendRequest();
        request.setId(50L);
        request.setFromUserId(2L);
        request.setToUserId(userId);
        request.setMessage("收到的请求");
        request.setStatus("pending");
        request.setCreatedAt(createdTime);

        when(friendRequestRepository.findByToUserIdAndStatus(userId, "pending"))
                .thenReturn(Collections.singletonList(request));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        // When
        List<FriendRequestResponse> result = friendService.getReceivedRequests(userId);

        // Then
        assertEquals(1, result.size());
        FriendRequestResponse response = result.get(0);

        assertEquals(50L, response.getId());
        assertEquals(2L, response.getFromUserId());
        assertEquals("user2", response.getFromUsername());
        assertEquals(userId, response.getToUserId());
        assertEquals("testUser", response.getToUsername());
        assertEquals("收到的请求", response.getMessage());
        assertEquals("pending", response.getStatus());
        assertEquals(createdTime, response.getCreatedAt());
    }

    @Test
    void testGetReceivedRequests_FromUserNotFound_ThrowsException() {
        // Given - convertToResponse 中会查询用户，如果不存在应抛异常
        Long userId = 1L;

        FriendRequest request = new FriendRequest();
        request.setId(1L);
        request.setFromUserId(999L); // 不存在的用户
        request.setToUserId(userId);
        request.setStatus("pending");
        request.setCreatedAt(LocalDateTime.now());

        when(friendRequestRepository.findByToUserIdAndStatus(userId, "pending"))
                .thenReturn(Collections.singletonList(request));
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> friendService.getReceivedRequests(userId),
                "发送者不存在应该抛出异常"
        );

        assertEquals(ErrorCode.NOT_FOUND_ERROR.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("用户不存在"));
    }

    @Test
    void testGetReceivedRequests_ToUserNotFound_ThrowsException() {
        // Given
        Long userId = 1L;

        FriendRequest request = new FriendRequest();
        request.setId(1L);
        request.setFromUserId(2L);
        request.setToUserId(userId);
        request.setStatus("pending");
        request.setCreatedAt(LocalDateTime.now());

        when(friendRequestRepository.findByToUserIdAndStatus(userId, "pending"))
                .thenReturn(Collections.singletonList(request));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));
        when(userRepository.findById(userId)).thenReturn(Optional.empty()); // toUser不存在

        // When & Then
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> friendService.getReceivedRequests(userId),
                "接收者不存在应该抛出异常"
        );

        assertEquals(ErrorCode.NOT_FOUND_ERROR.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("用户不存在"));
    }

    @Test
    void testGetSentRequests_ToUserNotFound_ThrowsException() {
        // Given
        Long userId = 1L;

        FriendRequest request = new FriendRequest();
        request.setId(1L);
        request.setFromUserId(userId);
        request.setToUserId(999L); // 不存在的用户
        request.setStatus("pending");
        request.setCreatedAt(LocalDateTime.now());

        when(friendRequestRepository.findByFromUserIdAndStatus(userId, "pending"))
                .thenReturn(Collections.singletonList(request));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> friendService.getSentRequests(userId),
                "接收者不存在应该抛出异常"
        );

        assertEquals(ErrorCode.NOT_FOUND_ERROR.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("用户不存在"));
    }

    // ==================== handleFriendRequest 方法测试 ====================

    @Test
    void testHandleFriendRequest_Accept_Success() {
        // Given
        Long requestId = 1L;
        Boolean accept = true;
        Long currentUserId = 2L; // 接收者

        FriendRequest request = new FriendRequest();
        request.setId(requestId);
        request.setFromUserId(1L);
        request.setToUserId(currentUserId);
        request.setMessage("你好");
        request.setStatus("pending");
        request.setCreatedAt(LocalDateTime.now());

        when(friendRequestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(friendRequestRepository.save(any(FriendRequest.class))).thenReturn(request);
        when(friendshipRepository.save(any(Friendship.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        friendService.handleFriendRequest(requestId, accept, currentUserId);

        // Then
        verify(friendRequestRepository, times(1)).findById(requestId);
        verify(friendRequestRepository, times(1)).save(any(FriendRequest.class));
        verify(friendshipRepository, times(2)).save(any(Friendship.class)); // 双向关系

        // 验证 FriendRequest 更新
        ArgumentCaptor<FriendRequest> requestCaptor = ArgumentCaptor.forClass(FriendRequest.class);
        verify(friendRequestRepository).save(requestCaptor.capture());
        FriendRequest savedRequest = requestCaptor.getValue();

        assertEquals("accepted", savedRequest.getStatus(), "状态应该是accepted");
        assertNotNull(savedRequest.getHandledAt(), "处理时间不应为null");

        // 验证 Friendship 创建
        ArgumentCaptor<Friendship> friendshipCaptor = ArgumentCaptor.forClass(Friendship.class);
        verify(friendshipRepository, times(2)).save(friendshipCaptor.capture());
        List<Friendship> savedFriendships = friendshipCaptor.getAllValues();

        assertEquals(2, savedFriendships.size(), "应该创建2个好友关系");

        // 验证双向关系
        Friendship friendship1 = savedFriendships.get(0);
        Friendship friendship2 = savedFriendships.get(1);

        // 验证第一个关系（fromUser -> toUser）
        assertEquals(1L, friendship1.getUserId(), "userId应该是fromUserId");
        assertEquals(currentUserId, friendship1.getFriendId(), "friendId应该是toUserId");
        assertTrue(friendship1.getIsActive(), "应该是活跃状态");
        assertNotNull(friendship1.getCreatedAt(), "创建时间不应为null");

        // 验证第二个关系（toUser -> fromUser）
        assertEquals(currentUserId, friendship2.getUserId(), "userId应该是toUserId");
        assertEquals(1L, friendship2.getFriendId(), "friendId应该是fromUserId");
        assertTrue(friendship2.getIsActive(), "应该是活跃状态");
        assertNotNull(friendship2.getCreatedAt(), "创建时间不应为null");
    }

    @Test
    void testHandleFriendRequest_Reject_Success() {
        // Given
        Long requestId = 1L;
        Boolean accept = false; // 拒绝
        Long currentUserId = 2L;

        FriendRequest request = new FriendRequest();
        request.setId(requestId);
        request.setFromUserId(1L);
        request.setToUserId(currentUserId);
        request.setMessage("你好");
        request.setStatus("pending");
        request.setCreatedAt(LocalDateTime.now());

        when(friendRequestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(friendRequestRepository.save(any(FriendRequest.class))).thenReturn(request);

        // When
        friendService.handleFriendRequest(requestId, accept, currentUserId);

        // Then
        verify(friendRequestRepository, times(1)).findById(requestId);
        verify(friendRequestRepository, times(1)).save(any(FriendRequest.class));
        verify(friendshipRepository, never()).save(any(Friendship.class)); // 拒绝不创建好友关系

        // 验证 FriendRequest 更新
        ArgumentCaptor<FriendRequest> captor = ArgumentCaptor.forClass(FriendRequest.class);
        verify(friendRequestRepository).save(captor.capture());
        FriendRequest savedRequest = captor.getValue();

        assertEquals("rejected", savedRequest.getStatus(), "状态应该是rejected");
        assertNotNull(savedRequest.getHandledAt(), "处理时间不应为null");
    }

    @Test
    void testHandleFriendRequest_RequestNotFound_ThrowsException() {
        // Given
        Long requestId = 999L;
        Boolean accept = true;
        Long currentUserId = 2L;

        when(friendRequestRepository.findById(requestId)).thenReturn(Optional.empty());

        // When & Then
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> friendService.handleFriendRequest(requestId, accept, currentUserId),
                "请求不存在应该抛出异常"
        );

        assertEquals(ErrorCode.NOT_FOUND_ERROR.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("好友请求不存在"));
        verify(friendRequestRepository, never()).save(any());
        verify(friendshipRepository, never()).save(any());
    }

    @Test
    void testHandleFriendRequest_NotReceiver_ThrowsException() {
        // Given
        Long requestId = 1L;
        Boolean accept = true;
        Long currentUserId = 3L; // 不是接收者

        FriendRequest request = new FriendRequest();
        request.setId(requestId);
        request.setFromUserId(1L);
        request.setToUserId(2L); // 真正的接收者
        request.setStatus("pending");

        when(friendRequestRepository.findById(requestId)).thenReturn(Optional.of(request));

        // When & Then
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> friendService.handleFriendRequest(requestId, accept, currentUserId),
                "非接收者不能处理请求"
        );

        assertEquals(ErrorCode.NO_AUTH_ERROR.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("无权处理此请求"));
        verify(friendRequestRepository, never()).save(any());
        verify(friendshipRepository, never()).save(any());
    }

    @Test
    void testHandleFriendRequest_AlreadyHandled_ThrowsException() {
        // Given
        Long requestId = 1L;
        Boolean accept = true;
        Long currentUserId = 2L;

        FriendRequest request = new FriendRequest();
        request.setId(requestId);
        request.setFromUserId(1L);
        request.setToUserId(currentUserId);
        request.setStatus("accepted"); // 已经处理过了
        request.setHandledAt(LocalDateTime.now());

        when(friendRequestRepository.findById(requestId)).thenReturn(Optional.of(request));

        // When & Then
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> friendService.handleFriendRequest(requestId, accept, currentUserId),
                "已处理的请求不能再次处理"
        );

        assertEquals(ErrorCode.OPERATION_ERROR.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("请求已处理"));
        verify(friendRequestRepository, never()).save(any());
        verify(friendshipRepository, never()).save(any());
    }

    @Test
    void testHandleFriendRequest_RejectedStatus_ThrowsException() {
        // Given - 已拒绝的请求不能再处理
        Long requestId = 1L;
        Boolean accept = true;
        Long currentUserId = 2L;

        FriendRequest request = new FriendRequest();
        request.setId(requestId);
        request.setFromUserId(1L);
        request.setToUserId(currentUserId);
        request.setStatus("rejected"); // 已被拒绝
        request.setHandledAt(LocalDateTime.now());

        when(friendRequestRepository.findById(requestId)).thenReturn(Optional.of(request));

        // When & Then
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> friendService.handleFriendRequest(requestId, accept, currentUserId)
        );

        assertEquals(ErrorCode.OPERATION_ERROR.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("请求已处理"));
    }

    @Test
    void testHandleFriendRequest_VerifyHandledAtTimestamp() {
        // Given - 验证处理时间设置
        Long requestId = 1L;
        Boolean accept = true;
        Long currentUserId = 2L;
        LocalDateTime beforeCall = LocalDateTime.now();

        FriendRequest request = new FriendRequest();
        request.setId(requestId);
        request.setFromUserId(1L);
        request.setToUserId(currentUserId);
        request.setStatus("pending");
        request.setCreatedAt(LocalDateTime.now());

        when(friendRequestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(friendRequestRepository.save(any(FriendRequest.class))).thenReturn(request);
        when(friendshipRepository.save(any(Friendship.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        friendService.handleFriendRequest(requestId, accept, currentUserId);
        LocalDateTime afterCall = LocalDateTime.now();

        // Then
        ArgumentCaptor<FriendRequest> captor = ArgumentCaptor.forClass(FriendRequest.class);
        verify(friendRequestRepository).save(captor.capture());
        FriendRequest savedRequest = captor.getValue();

        assertNotNull(savedRequest.getHandledAt());
        assertTrue(
                !savedRequest.getHandledAt().isBefore(beforeCall) &&
                        !savedRequest.getHandledAt().isAfter(afterCall),
                "处理时间应该在方法调用期间"
        );
    }

    @Test
    void testHandleFriendRequest_VerifyFriendshipTimestamp() {
        // Given - 验证好友关系的创建时间
        Long requestId = 1L;
        Boolean accept = true;
        Long currentUserId = 2L;
        LocalDateTime beforeCall = LocalDateTime.now();

        FriendRequest request = new FriendRequest();
        request.setId(requestId);
        request.setFromUserId(1L);
        request.setToUserId(currentUserId);
        request.setStatus("pending");

        when(friendRequestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(friendRequestRepository.save(any(FriendRequest.class))).thenReturn(request);
        when(friendshipRepository.save(any(Friendship.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        friendService.handleFriendRequest(requestId, accept, currentUserId);
        LocalDateTime afterCall = LocalDateTime.now();

        // Then
        ArgumentCaptor<Friendship> captor = ArgumentCaptor.forClass(Friendship.class);
        verify(friendshipRepository, times(2)).save(captor.capture());
        List<Friendship> savedFriendships = captor.getAllValues();

        for (Friendship friendship : savedFriendships) {
            assertNotNull(friendship.getCreatedAt());
            assertTrue(
                    !friendship.getCreatedAt().isBefore(beforeCall) &&
                            !friendship.getCreatedAt().isAfter(afterCall),
                    "创建时间应该在方法调用期间"
            );
        }
    }

    @Test
    void testHandleFriendRequest_VerifyBidirectionalFriendship() {
        // Given - 详细验证双向好友关系
        Long requestId = 1L;
        Boolean accept = true;
        Long currentUserId = 2L;
        Long fromUserId = 1L;

        FriendRequest request = new FriendRequest();
        request.setId(requestId);
        request.setFromUserId(fromUserId);
        request.setToUserId(currentUserId);
        request.setStatus("pending");

        when(friendRequestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(friendRequestRepository.save(any(FriendRequest.class))).thenReturn(request);
        when(friendshipRepository.save(any(Friendship.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        friendService.handleFriendRequest(requestId, accept, currentUserId);

        // Then
        ArgumentCaptor<Friendship> captor = ArgumentCaptor.forClass(Friendship.class);
        verify(friendshipRepository, times(2)).save(captor.capture());
        List<Friendship> friendships = captor.getAllValues();

        assertEquals(2, friendships.size(), "应该创建2个好友关系");

        // 验证是双向关系
        Friendship f1 = friendships.get(0);
        Friendship f2 = friendships.get(1);

        // 确保是相互的关系
        boolean isCorrectBidirectional =
                (f1.getUserId().equals(fromUserId) && f1.getFriendId().equals(currentUserId) &&
                        f2.getUserId().equals(currentUserId) && f2.getFriendId().equals(fromUserId));

        assertTrue(isCorrectBidirectional, "应该是正确的双向关系");

        // 验证两个关系的 createdAt 相同
        assertEquals(f1.getCreatedAt(), f2.getCreatedAt(),
                "两个好友关系应该同时创建");
    }

    @Test
    void testHandleFriendRequest_AcceptFalse_Reject() {
        // Given - 明确测试 accept = false 的情况
        Long requestId = 1L;
        Boolean accept = false;
        Long currentUserId = 2L;

        FriendRequest request = new FriendRequest();
        request.setId(requestId);
        request.setFromUserId(1L);
        request.setToUserId(currentUserId);
        request.setStatus("pending");

        when(friendRequestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(friendRequestRepository.save(any(FriendRequest.class))).thenReturn(request);

        // When
        friendService.handleFriendRequest(requestId, accept, currentUserId);

        // Then
        ArgumentCaptor<FriendRequest> captor = ArgumentCaptor.forClass(FriendRequest.class);
        verify(friendRequestRepository).save(captor.capture());
        FriendRequest savedRequest = captor.getValue();

        assertEquals("rejected", savedRequest.getStatus(), "状态应该是rejected");
        assertNotNull(savedRequest.getHandledAt(), "处理时间不应为null");
        verify(friendshipRepository, never()).save(any()); // 拒绝不应创建好友关系
    }

    @Test
    void testHandleFriendRequest_DifferentUsersAccept_Success() {
        // Given - 不同用户之间的请求处理
        Long requestId = 1L;
        Boolean accept = true;
        Long fromUserId = 3L;
        Long toUserId = 2L;

        FriendRequest request = new FriendRequest();
        request.setId(requestId);
        request.setFromUserId(fromUserId);
        request.setToUserId(toUserId);
        request.setStatus("pending");

        when(friendRequestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(friendRequestRepository.save(any(FriendRequest.class))).thenReturn(request);
        when(friendshipRepository.save(any(Friendship.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        friendService.handleFriendRequest(requestId, accept, toUserId);

        // Then
        verify(friendshipRepository, times(2)).save(any(Friendship.class));

        ArgumentCaptor<Friendship> captor = ArgumentCaptor.forClass(Friendship.class);
        verify(friendshipRepository, times(2)).save(captor.capture());
        List<Friendship> friendships = captor.getAllValues();

        // 验证正确的用户ID
        assertTrue(
                friendships.stream().anyMatch(f ->
                        f.getUserId().equals(fromUserId) && f.getFriendId().equals(toUserId)),
                "应该包含 fromUser -> toUser 的关系"
        );
        assertTrue(
                friendships.stream().anyMatch(f ->
                        f.getUserId().equals(toUserId) && f.getFriendId().equals(fromUserId)),
                "应该包含 toUser -> fromUser 的关系"
        );
    }

    // ==================== getFriends 方法测试 ====================

    @Test
    void testGetFriends_Success() {
        // Given
        Long userId = 1L;

        Friendship friendship1 = new Friendship();
        friendship1.setId(1L);
        friendship1.setUserId(userId);
        friendship1.setFriendId(2L);
        friendship1.setRemark("好友备注1");
        friendship1.setIsActive(true);
        friendship1.setCreatedAt(LocalDateTime.now());

        Friendship friendship2 = new Friendship();
        friendship2.setId(2L);
        friendship2.setUserId(userId);
        friendship2.setFriendId(3L);
        friendship2.setRemark("好友备注2");
        friendship2.setIsActive(true);
        friendship2.setCreatedAt(LocalDateTime.now());

        List<Friendship> friendships = Arrays.asList(friendship1, friendship2);

        when(friendshipRepository.findByUserIdAndIsActive(userId, true))
                .thenReturn(friendships);
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));
        when(userRepository.findById(3L)).thenReturn(Optional.of(user3));

        // When
        List<FriendResponse> result = friendService.getFriends(userId);

        // Then
        assertNotNull(result, "结果不应为null");
        assertEquals(2, result.size(), "应该返回2个好友");

        // 验证第一个好友
        FriendResponse response1 = result.get(0);
        assertEquals(2L, response1.getUid(), "好友ID应该匹配");
        assertEquals("user2", response1.getUsername(), "用户名应该匹配");
        assertEquals("user2@example.com", response1.getEmail(), "邮箱应该匹配");
        assertEquals("好友备注1", response1.getRemark(), "备注应该匹配");
        assertNotNull(response1.getFriendSince(), "创建时间不应为null");

        // 验证第二个好友
        FriendResponse response2 = result.get(1);
        assertEquals(3L, response2.getUid());
        assertEquals("user3", response2.getUsername());
        assertEquals("好友备注2", response2.getRemark());

        verify(friendshipRepository, times(1)).findByUserIdAndIsActive(userId, true);
        verify(userRepository, times(1)).findById(2L);
        verify(userRepository, times(1)).findById(3L);
    }

    @Test
    void testGetFriends_NoFriends_ReturnsEmptyList() {
        // Given
        Long userId = 1L;

        when(friendshipRepository.findByUserIdAndIsActive(userId, true))
                .thenReturn(Collections.emptyList());

        // When
        List<FriendResponse> result = friendService.getFriends(userId);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        assertEquals(0, result.size());
        verify(userRepository, never()).findById(anyLong());
    }

    @Test
    void testGetFriends_WithSingleFriend_Success() {
        // Given
        Long userId = 1L;

        Friendship friendship = new Friendship();
        friendship.setId(1L);
        friendship.setUserId(userId);
        friendship.setFriendId(2L);
        friendship.setRemark("唯一好友");
        friendship.setIsActive(true);
        friendship.setCreatedAt(LocalDateTime.now());

        when(friendshipRepository.findByUserIdAndIsActive(userId, true))
                .thenReturn(Collections.singletonList(friendship));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));

        // When
        List<FriendResponse> result = friendService.getFriends(userId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("唯一好友", result.get(0).getRemark());
    }

    @Test
    void testGetFriends_WithNullRemark_Success() {
        // Given - 备注可以为null
        Long userId = 1L;

        Friendship friendship = new Friendship();
        friendship.setId(1L);
        friendship.setUserId(userId);
        friendship.setFriendId(2L);
        friendship.setRemark(null); // 备注为null
        friendship.setIsActive(true);
        friendship.setCreatedAt(LocalDateTime.now());

        when(friendshipRepository.findByUserIdAndIsActive(userId, true))
                .thenReturn(Collections.singletonList(friendship));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));

        // When
        List<FriendResponse> result = friendService.getFriends(userId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertNull(result.get(0).getRemark(), "备注可以为null");
    }

    @Test
    void testGetFriends_FriendNotFound_ThrowsException() {
        // Given
        Long userId = 1L;

        Friendship friendship = new Friendship();
        friendship.setId(1L);
        friendship.setUserId(userId);
        friendship.setFriendId(999L); // 不存在的好友
        friendship.setIsActive(true);

        when(friendshipRepository.findByUserIdAndIsActive(userId, true))
                .thenReturn(Collections.singletonList(friendship));
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> friendService.getFriends(userId),
                "好友信息不存在应该抛出异常"
        );

        assertEquals(ErrorCode.NOT_FOUND_ERROR.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("好友信息不存在"));
    }

    @Test
    void testGetFriends_VerifyResponseFields() {
        // Given - 验证所有字段
        Long userId = 1L;
        LocalDateTime createdTime = LocalDateTime.of(2024, 1, 15, 10, 30);

        Friendship friendship = new Friendship();
        friendship.setId(1L);
        friendship.setUserId(userId);
        friendship.setFriendId(2L);
        friendship.setRemark("测试备注");
        friendship.setIsActive(true);
        friendship.setCreatedAt(createdTime);

        when(friendshipRepository.findByUserIdAndIsActive(userId, true))
                .thenReturn(Collections.singletonList(friendship));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));

        // When
        List<FriendResponse> result = friendService.getFriends(userId);

        // Then
        assertEquals(1, result.size());
        FriendResponse response = result.get(0);

        // 详细验证所有字段
        assertEquals(2L, response.getUid(), "用户ID应该匹配");
        assertEquals("user2", response.getUsername(), "用户名应该匹配");
        assertEquals("user2@example.com", response.getEmail(), "邮箱应该匹配");
        assertEquals("测试备注", response.getRemark(), "备注应该匹配");
    }

    @Test
    void testGetFriends_OnlyActiveReturned() {
        // Given - 只返回活跃的好友关系
        Long userId = 1L;

        Friendship activeFriendship = new Friendship();
        activeFriendship.setId(1L);
        activeFriendship.setUserId(userId);
        activeFriendship.setFriendId(2L);
        activeFriendship.setIsActive(true);
        activeFriendship.setCreatedAt(LocalDateTime.now());

        // 只会查询到活跃的好友
        when(friendshipRepository.findByUserIdAndIsActive(userId, true))
                .thenReturn(Collections.singletonList(activeFriendship));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));

        // When
        List<FriendResponse> result = friendService.getFriends(userId);

        // Then
        assertEquals(1, result.size(), "应该只返回活跃的好友");
        verify(friendshipRepository, times(1))
                .findByUserIdAndIsActive(userId, true);
    }

    // ==================== deleteFriend 方法测试 ====================

    @Test
    void testDeleteFriend_Success() {
        // Given
        Long userId = 1L;
        Long friendId = 2L;

        Friendship friendship1 = new Friendship();
        friendship1.setId(1L);
        friendship1.setUserId(userId);
        friendship1.setFriendId(friendId);
        friendship1.setIsActive(true);

        Friendship friendship2 = new Friendship();
        friendship2.setId(2L);
        friendship2.setUserId(friendId);
        friendship2.setFriendId(userId);
        friendship2.setIsActive(true);

        when(friendshipRepository.findByUserIdAndFriendIdAndIsActive(userId, friendId, true))
                .thenReturn(Optional.of(friendship1));
        when(friendshipRepository.findByUserIdAndFriendIdAndIsActive(friendId, userId, true))
                .thenReturn(Optional.of(friendship2));
        when(friendshipRepository.save(any(Friendship.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        friendService.deleteFriend(userId, friendId);

        // Then
        verify(friendshipRepository, times(1))
                .findByUserIdAndFriendIdAndIsActive(userId, friendId, true);
        verify(friendshipRepository, times(1))
                .findByUserIdAndFriendIdAndIsActive(friendId, userId, true);
        verify(friendshipRepository, times(2)).save(any(Friendship.class));

        // 验证两个关系都被逻辑删除
        ArgumentCaptor<Friendship> captor = ArgumentCaptor.forClass(Friendship.class);
        verify(friendshipRepository, times(2)).save(captor.capture());
        List<Friendship> savedFriendships = captor.getAllValues();

        assertEquals(2, savedFriendships.size());
        for (Friendship friendship : savedFriendships) {
            assertFalse(friendship.getIsActive(), "应该设置为不活跃");
            assertNotNull(friendship.getDeletedAt(), "删除时间不应为null");
        }
    }

    @Test
    void testDeleteFriend_FirstRelationshipNotFound_ThrowsException() {
        // Given
        Long userId = 1L;
        Long friendId = 2L;

        when(friendshipRepository.findByUserIdAndFriendIdAndIsActive(userId, friendId, true))
                .thenReturn(Optional.empty());

        // When & Then
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> friendService.deleteFriend(userId, friendId),
                "第一个好友关系不存在应该抛出异常"
        );

        assertEquals(ErrorCode.NOT_FOUND_ERROR.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("好友关系不存在"));
        verify(friendshipRepository, never()).save(any());
    }

    @Test
    void testDeleteFriend_SecondRelationshipNotFound_ThrowsException() {
        // Given
        Long userId = 1L;
        Long friendId = 2L;

        Friendship friendship1 = new Friendship();
        friendship1.setId(1L);
        friendship1.setUserId(userId);
        friendship1.setFriendId(friendId);
        friendship1.setIsActive(true);

        when(friendshipRepository.findByUserIdAndFriendIdAndIsActive(userId, friendId, true))
                .thenReturn(Optional.of(friendship1));
        when(friendshipRepository.findByUserIdAndFriendIdAndIsActive(friendId, userId, true))
                .thenReturn(Optional.empty()); // 第二个关系不存在

        // When & Then
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> friendService.deleteFriend(userId, friendId),
                "第二个好友关系不存在应该抛出异常"
        );

        assertEquals(ErrorCode.NOT_FOUND_ERROR.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("好友关系不存在"));
        verify(friendshipRepository, never()).save(any());
    }

    @Test
    void testDeleteFriend_VerifyDeletedAtTimestamp() {
        // Given - 验证删除时间戳
        Long userId = 1L;
        Long friendId = 2L;
        LocalDateTime beforeCall = LocalDateTime.now();

        Friendship friendship1 = new Friendship();
        friendship1.setId(1L);
        friendship1.setUserId(userId);
        friendship1.setFriendId(friendId);
        friendship1.setIsActive(true);

        Friendship friendship2 = new Friendship();
        friendship2.setId(2L);
        friendship2.setUserId(friendId);
        friendship2.setFriendId(userId);
        friendship2.setIsActive(true);

        when(friendshipRepository.findByUserIdAndFriendIdAndIsActive(userId, friendId, true))
                .thenReturn(Optional.of(friendship1));
        when(friendshipRepository.findByUserIdAndFriendIdAndIsActive(friendId, userId, true))
                .thenReturn(Optional.of(friendship2));
        when(friendshipRepository.save(any(Friendship.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        friendService.deleteFriend(userId, friendId);
        LocalDateTime afterCall = LocalDateTime.now();

        // Then
        ArgumentCaptor<Friendship> captor = ArgumentCaptor.forClass(Friendship.class);
        verify(friendshipRepository, times(2)).save(captor.capture());
        List<Friendship> savedFriendships = captor.getAllValues();

        for (Friendship friendship : savedFriendships) {
            assertNotNull(friendship.getDeletedAt());
            assertTrue(
                    !friendship.getDeletedAt().isBefore(beforeCall) &&
                            !friendship.getDeletedAt().isAfter(afterCall),
                    "删除时间应该在方法调用期间"
            );
        }
    }

    @Test
    void testDeleteFriend_VerifyBothDirections() {
        // Given - 验证双向删除
        Long userId = 1L;
        Long friendId = 2L;

        Friendship friendship1 = new Friendship();
        friendship1.setId(1L);
        friendship1.setUserId(userId);
        friendship1.setFriendId(friendId);
        friendship1.setIsActive(true);

        Friendship friendship2 = new Friendship();
        friendship2.setId(2L);
        friendship2.setUserId(friendId);
        friendship2.setFriendId(userId);
        friendship2.setIsActive(true);

        when(friendshipRepository.findByUserIdAndFriendIdAndIsActive(userId, friendId, true))
                .thenReturn(Optional.of(friendship1));
        when(friendshipRepository.findByUserIdAndFriendIdAndIsActive(friendId, userId, true))
                .thenReturn(Optional.of(friendship2));
        when(friendshipRepository.save(any(Friendship.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        friendService.deleteFriend(userId, friendId);

        // Then
        ArgumentCaptor<Friendship> captor = ArgumentCaptor.forClass(Friendship.class);
        verify(friendshipRepository, times(2)).save(captor.capture());
        List<Friendship> savedFriendships = captor.getAllValues();

        // 验证两个方向都被删除
        boolean hasUserToFriend = savedFriendships.stream()
                .anyMatch(f -> f.getUserId().equals(userId) && f.getFriendId().equals(friendId));
        boolean hasFriendToUser = savedFriendships.stream()
                .anyMatch(f -> f.getUserId().equals(friendId) && f.getFriendId().equals(userId));

        assertTrue(hasUserToFriend, "应该包含 user -> friend 方向的删除");
        assertTrue(hasFriendToUser, "应该包含 friend -> user 方向的删除");
    }

    @Test
    void testDeleteFriend_DifferentUsers_Success() {
        // Given - 不同用户删除好友
        Long userId = 3L;
        Long friendId = 2L;

        Friendship friendship1 = new Friendship();
        friendship1.setId(1L);
        friendship1.setUserId(userId);
        friendship1.setFriendId(friendId);
        friendship1.setIsActive(true);

        Friendship friendship2 = new Friendship();
        friendship2.setId(2L);
        friendship2.setUserId(friendId);
        friendship2.setFriendId(userId);
        friendship2.setIsActive(true);

        when(friendshipRepository.findByUserIdAndFriendIdAndIsActive(userId, friendId, true))
                .thenReturn(Optional.of(friendship1));
        when(friendshipRepository.findByUserIdAndFriendIdAndIsActive(friendId, userId, true))
                .thenReturn(Optional.of(friendship2));
        when(friendshipRepository.save(any(Friendship.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        friendService.deleteFriend(userId, friendId);

        // Then
        verify(friendshipRepository, times(2)).save(any(Friendship.class));
    }

    @Test
    void testDeleteFriend_VerifyDeletedAtSameTime() {
        // Given - 验证两个关系同时删除
        Long userId = 1L;
        Long friendId = 2L;

        Friendship friendship1 = new Friendship();
        friendship1.setId(1L);
        friendship1.setUserId(userId);
        friendship1.setFriendId(friendId);
        friendship1.setIsActive(true);

        Friendship friendship2 = new Friendship();
        friendship2.setId(2L);
        friendship2.setUserId(friendId);
        friendship2.setFriendId(userId);
        friendship2.setIsActive(true);

        when(friendshipRepository.findByUserIdAndFriendIdAndIsActive(userId, friendId, true))
                .thenReturn(Optional.of(friendship1));
        when(friendshipRepository.findByUserIdAndFriendIdAndIsActive(friendId, userId, true))
                .thenReturn(Optional.of(friendship2));
        when(friendshipRepository.save(any(Friendship.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        friendService.deleteFriend(userId, friendId);

        // Then
        ArgumentCaptor<Friendship> captor = ArgumentCaptor.forClass(Friendship.class);
        verify(friendshipRepository, times(2)).save(captor.capture());
        List<Friendship> savedFriendships = captor.getAllValues();

        assertEquals(2, savedFriendships.size());

        // 验证删除时间相同（或非常接近）
        LocalDateTime time1 = savedFriendships.get(0).getDeletedAt();
        LocalDateTime time2 = savedFriendships.get(1).getDeletedAt();

        assertNotNull(time1);
        assertNotNull(time2);

        // 时间差应该小于1秒
        long secondsDiff = Math.abs(
                java.time.Duration.between(time1, time2).getSeconds()
        );
        assertTrue(secondsDiff < 1, "两个关系应该几乎同时删除");
    }
}
