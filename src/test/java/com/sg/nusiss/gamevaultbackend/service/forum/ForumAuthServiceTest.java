package com.sg.nusiss.gamevaultbackend.service.forum;

import com.sg.nusiss.gamevaultbackend.entity.forum.ForumAccount;
import com.sg.nusiss.gamevaultbackend.entity.forum.ForumUser;
import com.sg.nusiss.gamevaultbackend.mapper.forum.ForumAccountMapper;
import com.sg.nusiss.gamevaultbackend.mapper.forum.ForumUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * @ClassName ForumAuthServiceTest
 * @Author Hou Zheyu
 * @Date 2025/10/25
 * @Description ForumAuthService单元测试类，覆盖所有方法
 */
@ExtendWith(MockitoExtension.class)
class ForumAuthServiceTest {

    @Mock
    private ForumAccountMapper accountMapper;

    @Mock
    private ForumUserMapper userMapper;

    @InjectMocks
    private ForumAuthService forumAuthService;

    private ForumUser testUser;
    private ForumAccount testAccount;
    private String testUsername;
    private String testPassword;

    @BeforeEach
    void setUp() {
        testUsername = "testuser";
        testPassword = "testpassword";

        testUser = new ForumUser();
        testUser.setUserId(1L);
        testUser.setUsername(testUsername);
        testUser.setNickname(testUsername);
        testUser.setStatus("active");
        testUser.setCreatedDate(LocalDateTime.now());
        testUser.setUpdatedDate(LocalDateTime.now());

        testAccount = new ForumAccount();
        testAccount.setAccountId(1L);
        testAccount.setUsername(testUsername);
        testAccount.setPassword(testPassword);
        testAccount.setUserId(1L);
        testAccount.setCreatedDate(LocalDateTime.now());
    }

    // ==================== register 方法测试 ====================

    @Test
    void testRegister_Success() {
        // Given
        when(accountMapper.existsByUsername(testUsername)).thenReturn(false);
        when(userMapper.insert(any(ForumUser.class))).thenAnswer(invocation -> {
            ForumUser user = invocation.getArgument(0);
            user.setUserId(1L); // 模拟数据库返回的ID
            return 1;
        });
        when(accountMapper.insert(any(ForumAccount.class))).thenReturn(1);

        // When
        ForumAccount result = forumAuthService.register(testUsername, testPassword);

        // Then
        assertNotNull(result);
        assertEquals(testUsername, result.getUsername());
        assertEquals(testPassword, result.getPassword());
        assertNotNull(result.getUserId());
        assertNotNull(result.getCreatedDate());

        verify(accountMapper, times(1)).existsByUsername(testUsername);
        verify(userMapper, times(1)).insert(any(ForumUser.class));
        verify(accountMapper, times(1)).insert(any(ForumAccount.class));
    }

    @Test
    void testRegister_UsernameAlreadyExists_ThrowsException() {
        // Given
        when(accountMapper.existsByUsername(testUsername)).thenReturn(true);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> forumAuthService.register(testUsername, testPassword));
        assertEquals("用户名已存在", exception.getMessage());

        verify(accountMapper, times(1)).existsByUsername(testUsername);
        verify(userMapper, never()).insert(any(ForumUser.class));
        verify(accountMapper, never()).insert(any(ForumAccount.class));
    }

    @Test
    void testRegister_EmptyUsername_Success() {
        // Given
        String username = "";
        String password = testPassword;

        // 实际实现中没有参数验证，允许空用户名
        when(accountMapper.existsByUsername(username)).thenReturn(false);
        when(userMapper.insert(any(ForumUser.class))).thenAnswer(invocation -> {
            ForumUser user = invocation.getArgument(0);
            user.setUserId(1L);
            return 1;
        });
        when(accountMapper.insert(any(ForumAccount.class))).thenReturn(1);

        // When
        ForumAccount result = forumAuthService.register(username, password);

        // Then
        assertNotNull(result);
        assertEquals("", result.getUsername());
        verify(accountMapper, times(1)).existsByUsername(username);
    }

    @Test
    void testRegister_NullUsername_Success() {
        // Given
        String username = null;
        String password = testPassword;

        // 实际实现中没有参数验证，允许null用户名
        when(accountMapper.existsByUsername(username)).thenReturn(false);
        when(userMapper.insert(any(ForumUser.class))).thenAnswer(invocation -> {
            ForumUser user = invocation.getArgument(0);
            user.setUserId(1L);
            return 1;
        });
        when(accountMapper.insert(any(ForumAccount.class))).thenReturn(1);

        // When
        ForumAccount result = forumAuthService.register(username, password);

        // Then
        assertNotNull(result);
        assertNull(result.getUsername());
        verify(accountMapper, times(1)).existsByUsername(username);
    }

    @Test
    void testRegister_EmptyPassword() {
        // Given
        String username = testUsername;
        String password = "";

        when(accountMapper.existsByUsername(username)).thenReturn(false);
        when(userMapper.insert(any(ForumUser.class))).thenAnswer(invocation -> {
            ForumUser user = invocation.getArgument(0);
            user.setUserId(1L);
            return 1;
        });
        when(accountMapper.insert(any(ForumAccount.class))).thenReturn(1);

        // When
        ForumAccount result = forumAuthService.register(username, password);

        // Then
        assertNotNull(result);
        assertEquals("", result.getPassword());
        verify(accountMapper, times(1)).existsByUsername(username);
        verify(userMapper, times(1)).insert(any(ForumUser.class));
        verify(accountMapper, times(1)).insert(any(ForumAccount.class));
    }

    @Test
    void testRegister_NullPassword() {
        // Given
        String username = testUsername;
        String password = null;

        when(accountMapper.existsByUsername(username)).thenReturn(false);
        when(userMapper.insert(any(ForumUser.class))).thenAnswer(invocation -> {
            ForumUser user = invocation.getArgument(0);
            user.setUserId(1L);
            return 1;
        });
        when(accountMapper.insert(any(ForumAccount.class))).thenReturn(1);

        // When
        ForumAccount result = forumAuthService.register(username, password);

        // Then
        assertNotNull(result);
        assertNull(result.getPassword());
        verify(accountMapper, times(1)).existsByUsername(username);
        verify(userMapper, times(1)).insert(any(ForumUser.class));
        verify(accountMapper, times(1)).insert(any(ForumAccount.class));
    }

    // ==================== login 方法测试 ====================

    @Test
    void testLogin_Success() {
        // Given
        when(accountMapper.findByUsername(testUsername)).thenReturn(testAccount);

        // When
        ForumAccount result = forumAuthService.login(testUsername, testPassword);

        // Then
        assertNotNull(result);
        assertEquals(testUsername, result.getUsername());
        assertEquals(testPassword, result.getPassword());
        assertEquals(1L, result.getUserId());

        verify(accountMapper, times(1)).findByUsername(testUsername);
    }

    @Test
    void testLogin_AccountNotFound_ReturnsNull() {
        // Given
        when(accountMapper.findByUsername(testUsername)).thenReturn(null);

        // When
        ForumAccount result = forumAuthService.login(testUsername, testPassword);

        // Then
        assertNull(result);
        verify(accountMapper, times(1)).findByUsername(testUsername);
    }

    @Test
    void testLogin_WrongPassword_ReturnsNull() {
        // Given
        String wrongPassword = "wrongpassword";
        when(accountMapper.findByUsername(testUsername)).thenReturn(testAccount);

        // When
        ForumAccount result = forumAuthService.login(testUsername, wrongPassword);

        // Then
        assertNull(result);
        verify(accountMapper, times(1)).findByUsername(testUsername);
    }

    @Test
    void testLogin_EmptyUsername_ReturnsNull() {
        // Given
        String username = "";
        when(accountMapper.findByUsername(username)).thenReturn(null);

        // When
        ForumAccount result = forumAuthService.login(username, testPassword);

        // Then
        assertNull(result);
        verify(accountMapper, times(1)).findByUsername(username);
    }

    @Test
    void testLogin_NullUsername_ReturnsNull() {
        // Given
        String username = null;
        when(accountMapper.findByUsername(username)).thenReturn(null);

        // When
        ForumAccount result = forumAuthService.login(username, testPassword);

        // Then
        assertNull(result);
        verify(accountMapper, times(1)).findByUsername(username);
    }

    @Test
    void testLogin_EmptyPassword_ReturnsNull() {
        // Given
        String password = "";
        when(accountMapper.findByUsername(testUsername)).thenReturn(testAccount);

        // When
        ForumAccount result = forumAuthService.login(testUsername, password);

        // Then
        assertNull(result);
        verify(accountMapper, times(1)).findByUsername(testUsername);
    }

    @Test
    void testLogin_NullPassword_ThrowsException() {
        // Given
        String password = null;
        when(accountMapper.findByUsername(testUsername)).thenReturn(testAccount);

        // When & Then
        // 实际实现中没有null检查，会抛出NullPointerException
        assertThrows(NullPointerException.class, () -> {
            forumAuthService.login(testUsername, password);
        });
        
        verify(accountMapper, times(1)).findByUsername(testUsername);
    }

    @Test
    void testLogin_CaseSensitivePassword() {
        // Given
        String wrongCasePassword = "TESTPASSWORD";
        when(accountMapper.findByUsername(testUsername)).thenReturn(testAccount);

        // When
        ForumAccount result = forumAuthService.login(testUsername, wrongCasePassword);

        // Then
        assertNull(result);
        verify(accountMapper, times(1)).findByUsername(testUsername);
    }

    @Test
    void testLogin_WhitespaceInPassword() {
        // Given
        String passwordWithSpaces = " " + testPassword + " ";
        when(accountMapper.findByUsername(testUsername)).thenReturn(testAccount);

        // When
        ForumAccount result = forumAuthService.login(testUsername, passwordWithSpaces);

        // Then
        assertNull(result);
        verify(accountMapper, times(1)).findByUsername(testUsername);
    }

    // ==================== getUserById 方法测试 ====================

    @Test
    void testGetUserById_Success() {
        // Given
        Long userId = 1L;
        when(userMapper.findById(userId)).thenReturn(testUser);

        // When
        ForumUser result = forumAuthService.getUserById(userId);

        // Then
        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertEquals(testUsername, result.getUsername());

        verify(userMapper, times(1)).findById(userId);
    }

    @Test
    void testGetUserById_UserNotFound_ReturnsNull() {
        // Given
        Long userId = 999L;
        when(userMapper.findById(userId)).thenReturn(null);

        // When
        ForumUser result = forumAuthService.getUserById(userId);

        // Then
        assertNull(result);
        verify(userMapper, times(1)).findById(userId);
    }

    @Test
    void testGetUserById_NullUserId_ReturnsNull() {
        // Given
        Long userId = null;
        when(userMapper.findById(userId)).thenReturn(null);

        // When
        ForumUser result = forumAuthService.getUserById(userId);

        // Then
        assertNull(result);
        verify(userMapper, times(1)).findById(userId);
    }

    // ==================== 集成测试场景 ====================

    @Test
    void testRegisterAndLogin_CompleteFlow() {
        // Given - 注册
        when(accountMapper.existsByUsername(testUsername)).thenReturn(false);
        when(userMapper.insert(any(ForumUser.class))).thenAnswer(invocation -> {
            ForumUser user = invocation.getArgument(0);
            user.setUserId(1L);
            return 1;
        });
        when(accountMapper.insert(any(ForumAccount.class))).thenReturn(1);

        // When - 注册
        ForumAccount registeredAccount = forumAuthService.register(testUsername, testPassword);

        // Then - 验证注册
        assertNotNull(registeredAccount);
        assertEquals(testUsername, registeredAccount.getUsername());

        // Given - 登录
        when(accountMapper.findByUsername(testUsername)).thenReturn(registeredAccount);

        // When - 登录
        ForumAccount loginResult = forumAuthService.login(testUsername, testPassword);

        // Then - 验证登录
        assertNotNull(loginResult);
        assertEquals(testUsername, loginResult.getUsername());
        assertEquals(testPassword, loginResult.getPassword());

        verify(accountMapper, times(1)).existsByUsername(testUsername);
        verify(userMapper, times(1)).insert(any(ForumUser.class));
        verify(accountMapper, times(1)).insert(any(ForumAccount.class));
        verify(accountMapper, times(1)).findByUsername(testUsername);
    }

    @Test
    void testRegisterDuplicateUsername_ThenLoginWithWrongPassword() {
        // Given - 第一次注册
        when(accountMapper.existsByUsername(testUsername)).thenReturn(false);
        when(userMapper.insert(any(ForumUser.class))).thenAnswer(invocation -> {
            ForumUser user = invocation.getArgument(0);
            user.setUserId(1L);
            return 1;
        });
        when(accountMapper.insert(any(ForumAccount.class))).thenReturn(1);

        ForumAccount firstAccount = forumAuthService.register(testUsername, testPassword);
        assertNotNull(firstAccount);

        // Given - 尝试重复注册
        when(accountMapper.existsByUsername(testUsername)).thenReturn(true);

        // When - 重复注册
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> forumAuthService.register(testUsername, testPassword));

        // Then - 验证重复注册失败
        assertEquals("用户名已存在", exception.getMessage());

        // Given - 尝试用错误密码登录
        when(accountMapper.findByUsername(testUsername)).thenReturn(firstAccount);

        // When - 用错误密码登录
        ForumAccount loginResult = forumAuthService.login(testUsername, "wrongpassword");

        // Then - 验证登录失败
        assertNull(loginResult);
    }

    @Test
    void testMultipleUsersRegistration() {
        // Given
        String username1 = "user1";
        String username2 = "user2";
        String password1 = "pass1";
        String password2 = "pass2";

        // 注册第一个用户
        when(accountMapper.existsByUsername(username1)).thenReturn(false);
        when(userMapper.insert(any(ForumUser.class))).thenAnswer(invocation -> {
            ForumUser user = invocation.getArgument(0);
            user.setUserId(1L);
            return 1;
        });
        when(accountMapper.insert(any(ForumAccount.class))).thenReturn(1);

        ForumAccount account1 = forumAuthService.register(username1, password1);
        assertNotNull(account1);

        // 注册第二个用户
        when(accountMapper.existsByUsername(username2)).thenReturn(false);
        when(userMapper.insert(any(ForumUser.class))).thenAnswer(invocation -> {
            ForumUser user = invocation.getArgument(0);
            user.setUserId(2L);
            return 1;
        });
        when(accountMapper.insert(any(ForumAccount.class))).thenReturn(1);

        ForumAccount account2 = forumAuthService.register(username2, password2);
        assertNotNull(account2);

        // 验证两个用户不同
        assertNotEquals(account1.getUserId(), account2.getUserId());

        // 验证登录
        when(accountMapper.findByUsername(username1)).thenReturn(account1);
        when(accountMapper.findByUsername(username2)).thenReturn(account2);

        ForumAccount login1 = forumAuthService.login(username1, password1);
        ForumAccount login2 = forumAuthService.login(username2, password2);

        assertNotNull(login1);
        assertNotNull(login2);
        assertEquals(username1, login1.getUsername());
        assertEquals(username2, login2.getUsername());
    }
}
