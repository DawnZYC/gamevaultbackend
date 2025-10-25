package com.sg.nusiss.gamevaultbackend.service.forum;

import com.sg.nusiss.gamevaultbackend.entity.forum.ForumUser;
import com.sg.nusiss.gamevaultbackend.mapper.forum.ForumUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * @ClassName ForumUserServiceTest
 * @Author Hou Zheyu
 * @Date 2025/10/25
 * @Description ForumUserService单元测试类，覆盖所有方法
 */
@ExtendWith(MockitoExtension.class)
class ForumUserServiceTest {

    @Mock
    private ForumUserMapper userMapper;

    @InjectMocks
    private ForumUserService forumUserService;

    private ForumUser testUser;
    private Long testUserId;
    private String testUsername;

    @BeforeEach
    void setUp() {
        testUserId = 1L;
        testUsername = "testuser";

        testUser = new ForumUser();
        testUser.setUserId(testUserId);
        testUser.setUsername(testUsername);
        testUser.setNickname("测试用户");
        testUser.setBio("测试用户简介");
        testUser.setAvatarUrl("http://example.com/avatar.jpg");
        testUser.setStatus("active");
        testUser.setCreatedDate(LocalDateTime.now());
        testUser.setUpdatedDate(LocalDateTime.now());
    }

    // ==================== getUserById 方法测试 ====================

    @Test
    void testGetUserById_Success() {
        // Given
        when(userMapper.findById(testUserId)).thenReturn(testUser);

        // When
        ForumUser result = forumUserService.getUserById(testUserId);

        // Then
        assertNotNull(result);
        assertEquals(testUserId, result.getUserId());
        assertEquals(testUsername, result.getUsername());
        assertEquals("测试用户", result.getNickname());

        verify(userMapper, times(1)).findById(testUserId);
    }

    @Test
    void testGetUserById_NullUserId_ThrowsException() {
        // Given
        Long userId = null;

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> forumUserService.getUserById(userId));
        assertEquals("用户ID不能为空", exception.getMessage());

        verify(userMapper, never()).findById(anyLong());
    }

    @Test
    void testGetUserById_UserNotFound_ThrowsException() {
        // Given
        when(userMapper.findById(testUserId)).thenReturn(null);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> forumUserService.getUserById(testUserId));
        assertEquals("用户不存在", exception.getMessage());

        verify(userMapper, times(1)).findById(testUserId);
    }

    // ==================== getUserByUsername 方法测试 ====================

    @Test
    void testGetUserByUsername_Success() {
        // Given
        when(userMapper.findByUsername(testUsername)).thenReturn(testUser);

        // When
        ForumUser result = forumUserService.getUserByUsername(testUsername);

        // Then
        assertNotNull(result);
        assertEquals(testUserId, result.getUserId());
        assertEquals(testUsername, result.getUsername());

        verify(userMapper, times(1)).findByUsername(testUsername);
    }

    @Test
    void testGetUserByUsername_NullUsername_ThrowsException() {
        // Given
        String username = null;

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> forumUserService.getUserByUsername(username));
        assertEquals("用户名不能为空", exception.getMessage());

        verify(userMapper, never()).findByUsername(anyString());
    }

    @Test
    void testGetUserByUsername_EmptyUsername_ThrowsException() {
        // Given
        String username = "";

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> forumUserService.getUserByUsername(username));
        assertEquals("用户名不能为空", exception.getMessage());

        verify(userMapper, never()).findByUsername(anyString());
    }

    @Test
    void testGetUserByUsername_WhitespaceUsername_ThrowsException() {
        // Given
        String username = "   ";

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> forumUserService.getUserByUsername(username));
        assertEquals("用户名不能为空", exception.getMessage());

        verify(userMapper, never()).findByUsername(anyString());
    }

    @Test
    void testGetUserByUsername_UserNotFound_ThrowsException() {
        // Given
        when(userMapper.findByUsername(testUsername)).thenReturn(null);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> forumUserService.getUserByUsername(testUsername));
        assertEquals("用户不存在", exception.getMessage());

        verify(userMapper, times(1)).findByUsername(testUsername);
    }

    @Test
    void testGetUserByUsername_TrimsUsername() {
        // Given
        String usernameWithSpaces = "  " + testUsername + "  ";
        when(userMapper.findByUsername(testUsername)).thenReturn(testUser);

        // When
        ForumUser result = forumUserService.getUserByUsername(usernameWithSpaces);

        // Then
        assertNotNull(result);
        verify(userMapper, times(1)).findByUsername(testUsername);
    }

    // ==================== createUser 方法测试 ====================

    @Test
    void testCreateUser_Success() {
        // Given
        Long userId = 2L;
        String username = "newuser";
        String nickname = "新用户";

        when(userMapper.findById(userId)).thenReturn(null);
        when(userMapper.existsByUsername(username)).thenReturn(false);
        when(userMapper.insert(any(ForumUser.class))).thenReturn(1);

        // When
        ForumUser result = forumUserService.createUser(userId, username, nickname);

        // Then
        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertEquals(username, result.getUsername());
        assertEquals(nickname, result.getNickname());

        verify(userMapper, times(1)).findById(userId);
        verify(userMapper, times(1)).existsByUsername(username);
        verify(userMapper, times(1)).insert(any(ForumUser.class));
    }

    @Test
    void testCreateUser_WithNullNickname() {
        // Given
        Long userId = 2L;
        String username = "newuser";
        String nickname = null;

        when(userMapper.findById(userId)).thenReturn(null);
        when(userMapper.existsByUsername(username)).thenReturn(false);
        when(userMapper.insert(any(ForumUser.class))).thenReturn(1);

        // When
        ForumUser result = forumUserService.createUser(userId, username, nickname);

        // Then
        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertEquals(username, result.getUsername());
        assertEquals(username, result.getNickname()); // 应该使用username作为nickname

        verify(userMapper, times(1)).findById(userId);
        verify(userMapper, times(1)).existsByUsername(username);
        verify(userMapper, times(1)).insert(any(ForumUser.class));
    }

    @Test
    void testCreateUser_UserAlreadyExists_ReturnsExistingUser() {
        // Given
        Long userId = 2L;
        String username = "existinguser";
        String nickname = "现有用户";

        when(userMapper.findById(userId)).thenReturn(testUser);

        // When
        ForumUser result = forumUserService.createUser(userId, username, nickname);

        // Then
        assertNotNull(result);
        assertEquals(testUser, result);

        verify(userMapper, times(1)).findById(userId);
        verify(userMapper, never()).existsByUsername(anyString());
        verify(userMapper, never()).insert(any(ForumUser.class));
    }

    @Test
    void testCreateUser_NullUserId_ThrowsException() {
        // Given
        Long userId = null;
        String username = "newuser";
        String nickname = "新用户";

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> forumUserService.createUser(userId, username, nickname));
        assertEquals("用户ID不能为空", exception.getMessage());

        verify(userMapper, never()).findById(anyLong());
    }

    @Test
    void testCreateUser_NullUsername_ThrowsException() {
        // Given
        Long userId = 2L;
        String username = null;
        String nickname = "新用户";

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> forumUserService.createUser(userId, username, nickname));
        assertEquals("用户名不能为空", exception.getMessage());

        verify(userMapper, never()).findById(anyLong());
    }

    @Test
    void testCreateUser_EmptyUsername_ThrowsException() {
        // Given
        Long userId = 2L;
        String username = "";
        String nickname = "新用户";

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> forumUserService.createUser(userId, username, nickname));
        assertEquals("用户名不能为空", exception.getMessage());

        verify(userMapper, never()).findById(anyLong());
    }

    @Test
    void testCreateUser_UsernameAlreadyExists_ThrowsException() {
        // Given
        Long userId = 2L;
        String username = "existinguser";
        String nickname = "新用户";

        when(userMapper.findById(userId)).thenReturn(null);
        when(userMapper.existsByUsername(username)).thenReturn(true);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> forumUserService.createUser(userId, username, nickname));
        assertEquals("用户名已被使用", exception.getMessage());

        verify(userMapper, times(1)).findById(userId);
        verify(userMapper, times(1)).existsByUsername(username);
        verify(userMapper, never()).insert(any(ForumUser.class));
    }

    @Test
    void testCreateUser_InsertFails_ThrowsException() {
        // Given
        Long userId = 2L;
        String username = "newuser";
        String nickname = "新用户";

        when(userMapper.findById(userId)).thenReturn(null);
        when(userMapper.existsByUsername(username)).thenReturn(false);
        when(userMapper.insert(any(ForumUser.class))).thenReturn(0);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> forumUserService.createUser(userId, username, nickname));
        assertEquals("创建用户失败", exception.getMessage());

        verify(userMapper, times(1)).findById(userId);
        verify(userMapper, times(1)).existsByUsername(username);
        verify(userMapper, times(1)).insert(any(ForumUser.class));
    }

    // ==================== updateUserProfile 方法测试 ====================

    @Test
    void testUpdateUserProfile_Success() {
        // Given
        String newNickname = "新昵称";
        String newBio = "新简介";
        String newAvatarUrl = "http://example.com/new-avatar.jpg";

        when(userMapper.findById(testUserId)).thenReturn(testUser);
        when(userMapper.update(any(ForumUser.class))).thenReturn(1);

        // When
        ForumUser result = forumUserService.updateUserProfile(testUserId, newNickname, newBio, newAvatarUrl);

        // Then
        assertNotNull(result);
        assertEquals(newNickname, result.getNickname());
        assertEquals(newBio, result.getBio());
        assertEquals(newAvatarUrl, result.getAvatarUrl());
        assertNotNull(result.getUpdatedDate());

        verify(userMapper, times(1)).findById(testUserId);
        verify(userMapper, times(1)).update(any(ForumUser.class));
    }

    @Test
    void testUpdateUserProfile_OnlyNickname() {
        // Given
        String newNickname = "新昵称";
        String newBio = null;
        String newAvatarUrl = null;

        when(userMapper.findById(testUserId)).thenReturn(testUser);
        when(userMapper.update(any(ForumUser.class))).thenReturn(1);

        // When
        ForumUser result = forumUserService.updateUserProfile(testUserId, newNickname, newBio, newAvatarUrl);

        // Then
        assertNotNull(result);
        assertEquals(newNickname, result.getNickname());
        assertNotNull(result.getUpdatedDate());

        verify(userMapper, times(1)).findById(testUserId);
        verify(userMapper, times(1)).update(any(ForumUser.class));
    }

    @Test
    void testUpdateUserProfile_EmptyNickname_NoUpdate() {
        // Given
        String newNickname = "";
        String newBio = "新简介";
        String newAvatarUrl = "http://example.com/new-avatar.jpg";

        when(userMapper.findById(testUserId)).thenReturn(testUser);
        when(userMapper.update(any(ForumUser.class))).thenReturn(1);

        // When
        ForumUser result = forumUserService.updateUserProfile(testUserId, newNickname, newBio, newAvatarUrl);

        // Then
        assertNotNull(result);
        assertEquals("测试用户", result.getNickname()); // 保持原昵称
        assertEquals(newBio, result.getBio());
        assertEquals(newAvatarUrl, result.getAvatarUrl());

        verify(userMapper, times(1)).findById(testUserId);
        verify(userMapper, times(1)).update(any(ForumUser.class));
    }

    @Test
    void testUpdateUserProfile_NullUserId_ThrowsException() {
        // Given
        Long userId = null;
        String newNickname = "新昵称";
        String newBio = "新简介";
        String newAvatarUrl = "http://example.com/new-avatar.jpg";

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> forumUserService.updateUserProfile(userId, newNickname, newBio, newAvatarUrl));
        assertEquals("用户ID不能为空", exception.getMessage());

        verify(userMapper, never()).findById(anyLong());
    }

    @Test
    void testUpdateUserProfile_UserNotFound_ThrowsException() {
        // Given
        String newNickname = "新昵称";
        String newBio = "新简介";
        String newAvatarUrl = "http://example.com/new-avatar.jpg";

        when(userMapper.findById(testUserId)).thenReturn(null);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> forumUserService.updateUserProfile(testUserId, newNickname, newBio, newAvatarUrl));
        assertEquals("用户不存在", exception.getMessage());

        verify(userMapper, times(1)).findById(testUserId);
    }

    @Test
    void testUpdateUserProfile_UpdateFails_ThrowsException() {
        // Given
        String newNickname = "新昵称";
        String newBio = "新简介";
        String newAvatarUrl = "http://example.com/new-avatar.jpg";

        when(userMapper.findById(testUserId)).thenReturn(testUser);
        when(userMapper.update(any(ForumUser.class))).thenReturn(0);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> forumUserService.updateUserProfile(testUserId, newNickname, newBio, newAvatarUrl));
        assertEquals("更新用户信息失败", exception.getMessage());

        verify(userMapper, times(1)).findById(testUserId);
        verify(userMapper, times(1)).update(any(ForumUser.class));
    }

    @Test
    void testUpdateUserProfile_NoChanges_NoUpdate() {
        // Given
        String newNickname = null;
        String newBio = null;
        String newAvatarUrl = null;

        when(userMapper.findById(testUserId)).thenReturn(testUser);

        // When
        ForumUser result = forumUserService.updateUserProfile(testUserId, newNickname, newBio, newAvatarUrl);

        // Then
        assertNotNull(result);
        assertEquals("测试用户", result.getNickname()); // 保持原值
        assertEquals("测试用户简介", result.getBio()); // 保持原值
        assertEquals("http://example.com/avatar.jpg", result.getAvatarUrl()); // 保持原值

        verify(userMapper, times(1)).findById(testUserId);
        verify(userMapper, never()).update(any(ForumUser.class));
    }

    // ==================== userExists 方法测试 ====================

    @Test
    void testUserExists_UserExists_ReturnsTrue() {
        // Given
        when(userMapper.findById(testUserId)).thenReturn(testUser);

        // When
        boolean result = forumUserService.userExists(testUserId);

        // Then
        assertTrue(result);
        verify(userMapper, times(1)).findById(testUserId);
    }

    @Test
    void testUserExists_UserNotExists_ReturnsFalse() {
        // Given
        when(userMapper.findById(testUserId)).thenReturn(null);

        // When
        boolean result = forumUserService.userExists(testUserId);

        // Then
        assertFalse(result);
        verify(userMapper, times(1)).findById(testUserId);
    }

    @Test
    void testUserExists_NullUserId_ReturnsFalse() {
        // Given
        Long userId = null;

        // When
        boolean result = forumUserService.userExists(userId);

        // Then
        assertFalse(result);
        verify(userMapper, never()).findById(anyLong());
    }

    @Test
    void testUserExists_Exception_ReturnsFalse() {
        // Given
        when(userMapper.findById(testUserId)).thenThrow(new RuntimeException("Database error"));

        // When
        boolean result = forumUserService.userExists(testUserId);

        // Then
        assertFalse(result);
        verify(userMapper, times(1)).findById(testUserId);
    }

    // ==================== usernameExists 方法测试 ====================

    @Test
    void testUsernameExists_UsernameExists_ReturnsTrue() {
        // Given
        when(userMapper.existsByUsername(testUsername)).thenReturn(true);

        // When
        boolean result = forumUserService.usernameExists(testUsername);

        // Then
        assertTrue(result);
        verify(userMapper, times(1)).existsByUsername(testUsername);
    }

    @Test
    void testUsernameExists_UsernameNotExists_ReturnsFalse() {
        // Given
        when(userMapper.existsByUsername(testUsername)).thenReturn(false);

        // When
        boolean result = forumUserService.usernameExists(testUsername);

        // Then
        assertFalse(result);
        verify(userMapper, times(1)).existsByUsername(testUsername);
    }

    @Test
    void testUsernameExists_NullUsername_ReturnsFalse() {
        // Given
        String username = null;

        // When
        boolean result = forumUserService.usernameExists(username);

        // Then
        assertFalse(result);
        verify(userMapper, never()).existsByUsername(anyString());
    }

    @Test
    void testUsernameExists_EmptyUsername_ReturnsFalse() {
        // Given
        String username = "";

        // When
        boolean result = forumUserService.usernameExists(username);

        // Then
        assertFalse(result);
        verify(userMapper, never()).existsByUsername(anyString());
    }

    @Test
    void testUsernameExists_WhitespaceUsername_ReturnsFalse() {
        // Given
        String username = "   ";

        // When
        boolean result = forumUserService.usernameExists(username);

        // Then
        assertFalse(result);
        verify(userMapper, never()).existsByUsername(anyString());
    }

    @Test
    void testUsernameExists_TrimsUsername() {
        // Given
        String usernameWithSpaces = "  " + testUsername + "  ";
        when(userMapper.existsByUsername(testUsername)).thenReturn(true);

        // When
        boolean result = forumUserService.usernameExists(usernameWithSpaces);

        // Then
        assertTrue(result);
        verify(userMapper, times(1)).existsByUsername(testUsername);
    }

    // ==================== getActiveUsers 方法测试 ====================

    @Test
    void testGetActiveUsers_Success() {
        // Given
        int page = 0;
        int size = 10;
        List<ForumUser> users = Arrays.asList(testUser);

        when(userMapper.findActiveUsers(page * size, size)).thenReturn(users);

        // When
        List<ForumUser> result = forumUserService.getActiveUsers(page, size);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testUser, result.get(0));

        verify(userMapper, times(1)).findActiveUsers(page * size, size);
    }

    @Test
    void testGetActiveUsers_InvalidPageAndSize() {
        // Given
        int page = -1;
        int size = 0;

        when(userMapper.findActiveUsers(0, 20)).thenReturn(new java.util.ArrayList<>());

        // When
        List<ForumUser> result = forumUserService.getActiveUsers(page, size);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(userMapper, times(1)).findActiveUsers(0, 20);
    }

    @Test
    void testGetActiveUsers_EmptyResult() {
        // Given
        int page = 0;
        int size = 10;

        when(userMapper.findActiveUsers(page * size, size)).thenReturn(new java.util.ArrayList<>());

        // When
        List<ForumUser> result = forumUserService.getActiveUsers(page, size);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(userMapper, times(1)).findActiveUsers(page * size, size);
    }

    // ==================== getActiveUserCount 方法测试 ====================

    @Test
    void testGetActiveUserCount_Success() {
        // Given
        int expectedCount = 5;
        when(userMapper.countActiveUsers()).thenReturn(expectedCount);

        // When
        int result = forumUserService.getActiveUserCount();

        // Then
        assertEquals(expectedCount, result);
        verify(userMapper, times(1)).countActiveUsers();
    }

    // ==================== getUsersByIds 方法测试 ====================

    @Test
    void testGetUsersByIds_Success() {
        // Given
        List<Long> userIds = Arrays.asList(1L, 2L, 3L);
        List<ForumUser> users = Arrays.asList(testUser);

        when(userMapper.findByIds(userIds)).thenReturn(users);

        // When
        List<ForumUser> result = forumUserService.getUsersByIds(userIds);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testUser, result.get(0));

        verify(userMapper, times(1)).findByIds(userIds);
    }

    @Test
    void testGetUsersByIds_NullUserIds_ThrowsException() {
        // Given
        List<Long> userIds = null;

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> forumUserService.getUsersByIds(userIds));
        assertEquals("用户ID列表不能为空", exception.getMessage());

        verify(userMapper, never()).findByIds(anyList());
    }

    @Test
    void testGetUsersByIds_EmptyUserIds_ThrowsException() {
        // Given
        List<Long> userIds = Collections.emptyList();

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> forumUserService.getUsersByIds(userIds));
        assertEquals("用户ID列表不能为空", exception.getMessage());

        verify(userMapper, never()).findByIds(anyList());
    }

    // ==================== isUserActive 方法测试 ====================

    @Test
    void testIsUserActive_UserActive_ReturnsTrue() {
        // Given
        testUser.setStatus("active");
        when(userMapper.findById(testUserId)).thenReturn(testUser);

        // When
        boolean result = forumUserService.isUserActive(testUserId);

        // Then
        assertTrue(result);
        verify(userMapper, times(1)).findById(testUserId);
    }

    @Test
    void testIsUserActive_UserInactive_ReturnsFalse() {
        // Given
        testUser.setStatus("inactive");
        when(userMapper.findById(testUserId)).thenReturn(testUser);

        // When
        boolean result = forumUserService.isUserActive(testUserId);

        // Then
        assertFalse(result);
        verify(userMapper, times(1)).findById(testUserId);
    }

    @Test
    void testIsUserActive_UserBanned_ReturnsFalse() {
        // Given
        testUser.setStatus("banned");
        when(userMapper.findById(testUserId)).thenReturn(testUser);

        // When
        boolean result = forumUserService.isUserActive(testUserId);

        // Then
        assertFalse(result);
        verify(userMapper, times(1)).findById(testUserId);
    }

    @Test
    void testIsUserActive_UserNotFound_ReturnsFalse() {
        // Given
        when(userMapper.findById(testUserId)).thenReturn(null);

        // When
        boolean result = forumUserService.isUserActive(testUserId);

        // Then
        assertFalse(result);
        verify(userMapper, times(1)).findById(testUserId);
    }

    @Test
    void testIsUserActive_NullUserId_ReturnsFalse() {
        // Given
        Long userId = null;

        // When
        boolean result = forumUserService.isUserActive(userId);

        // Then
        assertFalse(result);
        verify(userMapper, never()).findById(anyLong());
    }

    @Test
    void testIsUserActive_Exception_ReturnsFalse() {
        // Given
        when(userMapper.findById(testUserId)).thenThrow(new RuntimeException("Database error"));

        // When
        boolean result = forumUserService.isUserActive(testUserId);

        // Then
        assertFalse(result);
        verify(userMapper, times(1)).findById(testUserId);
    }
}
