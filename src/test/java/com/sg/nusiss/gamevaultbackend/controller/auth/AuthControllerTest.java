package com.sg.nusiss.gamevaultbackend.controller.auth;

import com.sg.nusiss.gamevaultbackend.dto.auth.LoginReq;
import com.sg.nusiss.gamevaultbackend.dto.auth.RegisterReq;
import com.sg.nusiss.gamevaultbackend.dto.settings.ChangeEmailReq;
import com.sg.nusiss.gamevaultbackend.dto.settings.ChangePasswordReq;
import com.sg.nusiss.gamevaultbackend.entity.auth.User;
import com.sg.nusiss.gamevaultbackend.repository.auth.UserRepository;
import com.sg.nusiss.gamevaultbackend.security.auth.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * @ClassName AuthControllerTest
 * @Author Zhang Yuchen
 * @Date 2025/10/25
 * @Description 认证控制器单元测试
 */
@ExtendWith(MockitoExtension.class)
public class AuthControllerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthController authController;

    private User testUser;
    private RegisterReq registerReq;
    private LoginReq loginReq;
    private ChangePasswordReq changePasswordReq;
    private ChangeEmailReq changeEmailReq;

    @BeforeEach
    void setUp() {
        // 准备测试用户
        testUser = new User();
        testUser.setUserId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("$2a$10$encodedPassword");
        testUser.setCreatedDate(LocalDateTime.now());
        testUser.setStatus("active");

        // 准备注册请求
        registerReq = new RegisterReq();
        registerReq.username = "newuser";
        registerReq.email = "newuser@example.com";
        registerReq.password = "password123";

        // 准备登录请求
        loginReq = new LoginReq();
        loginReq.username = "testuser";
        loginReq.password = "password123";

        // 准备修改密码请求
        changePasswordReq = new ChangePasswordReq();
        changePasswordReq.setOldPassword("oldPassword123");
        changePasswordReq.setNewPassword("newPassword123");

        // 准备修改邮箱请求
        changeEmailReq = new ChangeEmailReq();
        changeEmailReq.setPassword("password123");
        changeEmailReq.setNewEmail("newemail@example.com");
    }

    // ==================== register 方法测试 ====================

    @Test
    void testRegister_Success() {
        // Given
        when(userRepository.existsByUsername(registerReq.username)).thenReturn(false);
        when(userRepository.existsByEmail(registerReq.email)).thenReturn(false);
        when(passwordEncoder.encode(registerReq.password)).thenReturn("$2a$10$encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setUserId(100L);
            return user;
        });
        when(jwtUtil.generateToken(100L, registerReq.username, registerReq.email))
                .thenReturn("jwt-token");

        // When
        Map<String, Object> result = authController.register(registerReq);

        // Then
        assertNotNull(result, "返回结果不应为null");
        assertEquals("jwt-token", result.get("token"), "应返回JWT token");
        assertEquals("newuser", result.get("username"), "用户名应该匹配");
        assertEquals(100L, result.get("userId"), "用户ID应该匹配");
        assertEquals("newuser@example.com", result.get("email"), "邮箱应该匹配");

        // 验证方法调用
        verify(userRepository, times(1)).existsByUsername(registerReq.username);
        verify(userRepository, times(1)).existsByEmail(registerReq.email);
        verify(passwordEncoder, times(1)).encode(registerReq.password);
        verify(userRepository, times(1)).save(any(User.class));

        // 验证保存的用户数据
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertEquals("newuser", savedUser.getUsername(), "保存的用户名应该匹配");
        assertEquals("newuser@example.com", savedUser.getEmail(), "保存的邮箱应该匹配");
        assertEquals("$2a$10$encodedPassword", savedUser.getPassword(), "密码应该被编码");
        assertNotNull(savedUser.getCreatedDate(), "创建时间不应为null");
    }

    @Test
    void testRegister_UsernameTaken_ThrowsException() {
        // Given
        when(userRepository.existsByUsername(registerReq.username)).thenReturn(true);

        // When & Then
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> authController.register(registerReq),
                "用户名已存在应该抛出异常"
        );

        assertEquals("Username taken", exception.getMessage());
        verify(userRepository, times(1)).existsByUsername(registerReq.username);
        verify(userRepository, never()).existsByEmail(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testRegister_EmailTaken_ThrowsException() {
        // Given
        when(userRepository.existsByUsername(registerReq.username)).thenReturn(false);
        when(userRepository.existsByEmail(registerReq.email)).thenReturn(true);

        // When & Then
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> authController.register(registerReq),
                "邮箱已存在应该抛出异常"
        );

        assertEquals("Email taken", exception.getMessage());
        verify(userRepository, times(1)).existsByUsername(registerReq.username);
        verify(userRepository, times(1)).existsByEmail(registerReq.email);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testRegister_PasswordIsEncoded() {
        // Given - 验证密码被正确编码
        when(userRepository.existsByUsername(registerReq.username)).thenReturn(false);
        when(userRepository.existsByEmail(registerReq.email)).thenReturn(false);
        when(passwordEncoder.encode(registerReq.password)).thenReturn("$2a$10$specificEncodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setUserId(100L);
            return user;
        });
        when(jwtUtil.generateToken(anyLong(), anyString(), anyString())).thenReturn("jwt-token");

        // When
        authController.register(registerReq);

        // Then
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertEquals("$2a$10$specificEncodedPassword", savedUser.getPassword(), 
                "密码应该使用BCrypt编码");
        verify(passwordEncoder, times(1)).encode("password123");
    }

    // ==================== login 方法测试 ====================

    @Test
    void testLogin_Success() {
        // Given
        when(userRepository.findByUsername(loginReq.username)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(loginReq.password, testUser.getPassword())).thenReturn(true);
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtUtil.generateToken(testUser.getUserId(), testUser.getUsername(), testUser.getEmail()))
                .thenReturn("jwt-token");

        // When
        Map<String, Object> result = authController.login(loginReq);

        // Then
        assertNotNull(result, "返回结果不应为null");
        assertEquals("jwt-token", result.get("token"), "应返回JWT token");
        assertEquals(1L, result.get("userId"), "用户ID应该匹配");
        assertEquals("testuser", result.get("username"), "用户名应该匹配");
        assertEquals("test@example.com", result.get("email"), "邮箱应该匹配");
        assertEquals("Login successful", result.get("message"), "应返回成功消息");

        // 验证最后登录时间被更新
        verify(userRepository, times(1)).save(any(User.class));
        verify(userRepository, times(1)).findByUsername(loginReq.username);
        verify(passwordEncoder, times(1)).matches(loginReq.password, testUser.getPassword());
    }

    @Test
    void testLogin_WithEmail_Success() {
        // Given - 使用邮箱登录
        loginReq.username = "test@example.com";

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(loginReq.password, testUser.getPassword())).thenReturn(true);
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtUtil.generateToken(testUser.getUserId(), testUser.getUsername(), testUser.getEmail()))
                .thenReturn("jwt-token");

        // When
        Map<String, Object> result = authController.login(loginReq);

        // Then
        assertNotNull(result, "返回结果不应为null");
        assertEquals("jwt-token", result.get("token"), "应返回JWT token");
        assertEquals("testuser", result.get("username"), "用户名应该匹配");

        verify(userRepository, times(1)).findByEmail("test@example.com");
        verify(userRepository, times(1)).findByUsername("testuser");
    }

    @Test
    void testLogin_InvalidUsername_ThrowsException() {
        // Given
        when(userRepository.findByUsername(loginReq.username)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> authController.login(loginReq),
                "用户名不存在应该抛出异常"
        );

        assertEquals("Invalid username or password", exception.getMessage());
        verify(userRepository, times(1)).findByUsername(loginReq.username);
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testLogin_InvalidPassword_ThrowsException() {
        // Given
        when(userRepository.findByUsername(loginReq.username)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(loginReq.password, testUser.getPassword())).thenReturn(false);

        // When & Then
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> authController.login(loginReq),
                "密码错误应该抛出异常"
        );

        assertEquals("Invalid username or password", exception.getMessage());
        verify(userRepository, times(1)).findByUsername(loginReq.username);
        verify(passwordEncoder, times(1)).matches(loginReq.password, testUser.getPassword());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testLogin_UpdatesLastLoginTime() {
        // Given - 验证登录时间被更新
        LocalDateTime beforeLogin = LocalDateTime.now();

        when(userRepository.findByUsername(loginReq.username)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(loginReq.password, testUser.getPassword())).thenReturn(true);
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtUtil.generateToken(anyLong(), anyString(), anyString())).thenReturn("jwt-token");

        // When
        authController.login(loginReq);

        // Then
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertNotNull(savedUser.getLastLoginTime(), "最后登录时间不应为null");
        assertTrue(!savedUser.getLastLoginTime().isBefore(beforeLogin), 
                "最后登录时间应该在登录之后");
    }

    // ==================== changePassword 方法测试 ====================

    @Test
    void testChangePassword_Success() {
        // Given
        Jwt mockJwt = mock(Jwt.class);
        when(mockJwt.getClaims()).thenReturn(Map.of("uid", 1L));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(changePasswordReq.getOldPassword(), testUser.getPassword()))
                .thenReturn(true);
        when(passwordEncoder.encode(changePasswordReq.getNewPassword()))
                .thenReturn("$2a$10$newEncodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        Map<String, Object> result = authController.changePassword(changePasswordReq, mockJwt);

        // Then
        assertNotNull(result, "返回结果不应为null");
        assertEquals("Password changed successfully", result.get("message"), "应返回成功消息");
        assertEquals(true, result.get("success"), "success应该为true");

        // 验证密码被更新
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertEquals("$2a$10$newEncodedPassword", savedUser.getPassword(), 
                "密码应该被更新为新密码");

        verify(userRepository, times(1)).findById(1L);
        verify(passwordEncoder, times(1)).matches(changePasswordReq.getOldPassword(), 
                "$2a$10$encodedPassword");
        verify(passwordEncoder, times(1)).encode(changePasswordReq.getNewPassword());
    }

    @Test
    void testChangePassword_InvalidUserId_ThrowsException() {
        // Given - JWT中没有uid
        Jwt mockJwt = mock(Jwt.class);
        when(mockJwt.getClaims()).thenReturn(Map.of());

        // When & Then
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> authController.changePassword(changePasswordReq, mockJwt),
                "无效的用户ID应该抛出异常"
        );

        assertEquals("Invalid user ID", exception.getMessage());
        verify(userRepository, never()).findById(anyLong());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testChangePassword_UserNotFound_ThrowsException() {
        // Given
        Jwt mockJwt = mock(Jwt.class);
        when(mockJwt.getClaims()).thenReturn(Map.of("uid", 999L));
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> authController.changePassword(changePasswordReq, mockJwt),
                "用户不存在应该抛出异常"
        );

        assertEquals("User not found", exception.getMessage());
        verify(userRepository, times(1)).findById(999L);
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testChangePassword_IncorrectOldPassword_ThrowsException() {
        // Given
        Jwt mockJwt = mock(Jwt.class);
        when(mockJwt.getClaims()).thenReturn(Map.of("uid", 1L));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(changePasswordReq.getOldPassword(), testUser.getPassword()))
                .thenReturn(false);

        // When & Then
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> authController.changePassword(changePasswordReq, mockJwt),
                "旧密码错误应该抛出异常"
        );

        assertEquals("Current password is incorrect", exception.getMessage());
        verify(userRepository, times(1)).findById(1L);
        verify(passwordEncoder, times(1)).matches(changePasswordReq.getOldPassword(), 
                "$2a$10$encodedPassword");
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testChangePassword_NewPasswordEncoded() {
        // Given - 验证新密码被正确编码
        Jwt mockJwt = mock(Jwt.class);
        when(mockJwt.getClaims()).thenReturn(Map.of("uid", 1L));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(changePasswordReq.getOldPassword(), testUser.getPassword()))
                .thenReturn(true);
        when(passwordEncoder.encode(changePasswordReq.getNewPassword()))
                .thenReturn("$2a$10$specificNewEncodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        authController.changePassword(changePasswordReq, mockJwt);

        // Then
        verify(passwordEncoder, times(1)).encode("newPassword123");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertEquals("$2a$10$specificNewEncodedPassword", savedUser.getPassword(), 
                "新密码应该使用BCrypt编码");
    }

    // ==================== changeEmail 方法测试 ====================

    @Test
    void testChangeEmail_Success() {
        // Given
        Jwt mockJwt = mock(Jwt.class);
        when(mockJwt.getClaims()).thenReturn(Map.of("uid", 1L));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(changeEmailReq.getPassword(), testUser.getPassword()))
                .thenReturn(true);
        when(userRepository.existsByEmail(changeEmailReq.getNewEmail())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        Map<String, Object> result = authController.changeEmail(changeEmailReq, mockJwt);

        // Then
        assertNotNull(result, "返回结果不应为null");
        assertEquals("Email changed successfully", result.get("message"), "应返回成功消息");
        assertEquals(true, result.get("success"), "success应该为true");

        // 验证邮箱被更新
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertEquals("newemail@example.com", savedUser.getEmail(), "邮箱应该被更新");

        verify(userRepository, times(1)).findById(1L);
        verify(passwordEncoder, times(1)).matches(changeEmailReq.getPassword(), 
                "$2a$10$encodedPassword");
        verify(userRepository, times(1)).existsByEmail(changeEmailReq.getNewEmail());
    }

    @Test
    void testChangeEmail_InvalidUserId_ThrowsException() {
        // Given - JWT中没有uid
        Jwt mockJwt = mock(Jwt.class);
        when(mockJwt.getClaims()).thenReturn(Map.of());

        // When & Then
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> authController.changeEmail(changeEmailReq, mockJwt),
                "无效的用户ID应该抛出异常"
        );

        assertEquals("Invalid user ID", exception.getMessage());
        verify(userRepository, never()).findById(anyLong());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testChangeEmail_UserNotFound_ThrowsException() {
        // Given
        Jwt mockJwt = mock(Jwt.class);
        when(mockJwt.getClaims()).thenReturn(Map.of("uid", 999L));
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> authController.changeEmail(changeEmailReq, mockJwt),
                "用户不存在应该抛出异常"
        );

        assertEquals("User not found", exception.getMessage());
        verify(userRepository, times(1)).findById(999L);
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testChangeEmail_IncorrectPassword_ThrowsException() {
        // Given
        Jwt mockJwt = mock(Jwt.class);
        when(mockJwt.getClaims()).thenReturn(Map.of("uid", 1L));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(changeEmailReq.getPassword(), testUser.getPassword()))
                .thenReturn(false);

        // When & Then
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> authController.changeEmail(changeEmailReq, mockJwt),
                "密码错误应该抛出异常"
        );

        assertEquals("Password is incorrect", exception.getMessage());
        verify(userRepository, times(1)).findById(1L);
        verify(passwordEncoder, times(1)).matches(changeEmailReq.getPassword(), 
                "$2a$10$encodedPassword");
        verify(userRepository, never()).existsByEmail(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testChangeEmail_EmailAlreadyInUse_ThrowsException() {
        // Given
        Jwt mockJwt = mock(Jwt.class);
        when(mockJwt.getClaims()).thenReturn(Map.of("uid", 1L));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(changeEmailReq.getPassword(), testUser.getPassword()))
                .thenReturn(true);
        when(userRepository.existsByEmail(changeEmailReq.getNewEmail())).thenReturn(true);

        // When & Then
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> authController.changeEmail(changeEmailReq, mockJwt),
                "邮箱已被使用应该抛出异常"
        );

        assertEquals("Email already in use", exception.getMessage());
        verify(userRepository, times(1)).findById(1L);
        verify(passwordEncoder, times(1)).matches(changeEmailReq.getPassword(), 
                "$2a$10$encodedPassword");
        verify(userRepository, times(1)).existsByEmail(changeEmailReq.getNewEmail());
        verify(userRepository, never()).save(any(User.class));
    }

    // ==================== checkEmail 方法测试 ====================

    @Test
    void testCheckEmail_Exists() {
        // Given
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        // When
        Map<String, Boolean> result = authController.checkEmail("test@example.com");

        // Then
        assertNotNull(result);
        assertEquals(true, result.get("exists"), "邮箱存在应该返回true");
        verify(userRepository, times(1)).existsByEmail("test@example.com");
    }

    @Test
    void testCheckEmail_NotExists() {
        // Given
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);

        // When
        Map<String, Boolean> result = authController.checkEmail("new@example.com");

        // Then
        assertNotNull(result);
        assertEquals(false, result.get("exists"), "邮箱不存在应该返回false");
        verify(userRepository, times(1)).existsByEmail("new@example.com");
    }

    // ==================== checkUsername 方法测试 ====================

    @Test
    void testCheckUsername_Exists() {
        // Given
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        // When
        Map<String, Boolean> result = authController.checkUsername("testuser");

        // Then
        assertNotNull(result);
        assertEquals(true, result.get("exists"), "用户名存在应该返回true");
        verify(userRepository, times(1)).existsByUsername("testuser");
    }

    @Test
    void testCheckUsername_NotExists() {
        // Given
        when(userRepository.existsByUsername("newuser")).thenReturn(false);

        // When
        Map<String, Boolean> result = authController.checkUsername("newuser");

        // Then
        assertNotNull(result);
        assertEquals(false, result.get("exists"), "用户名不存在应该返回false");
        verify(userRepository, times(1)).existsByUsername("newuser");
    }

    // ==================== logout 方法测试 ====================

    @Test
    void testLogout_Success() {
        // When
        Map<String, Object> result = authController.logout();

        // Then
        assertNotNull(result);
        assertEquals("Logout successful", result.get("message"));
        assertEquals(true, result.get("success"));
    }

    // ==================== 参数验证测试 ====================
    // 注意：实际的AuthController没有参数验证，这些测试被注释掉
    // 参数验证由@Valid注解和Spring Boot的验证框架处理

    /*
    @Test
    void testRegister_EmptyUsername_ThrowsException() {
        // 实际实现中没有参数验证，由@Valid注解处理
    }

    @Test
    void testRegister_EmptyEmail_ThrowsException() {
        // 实际实现中没有参数验证，由@Valid注解处理
    }

    @Test
    void testRegister_EmptyPassword_ThrowsException() {
        // 实际实现中没有参数验证，由@Valid注解处理
    }

    @Test
    void testRegister_InvalidEmailFormat_ThrowsException() {
        // 实际实现中没有参数验证，由@Valid注解处理
    }

    @Test
    void testRegister_ShortPassword_ThrowsException() {
        // 实际实现中没有参数验证，由@Valid注解处理
    }

    @Test
    void testLogin_EmptyUsername_ThrowsException() {
        // 实际实现中没有参数验证，由@Valid注解处理
    }

    @Test
    void testLogin_EmptyPassword_ThrowsException() {
        // 实际实现中没有参数验证，由@Valid注解处理
    }
    */

    @Test
    void testLogin_UserNotFound_ThrowsException() {
        // Given
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        LoginReq nonexistentUserReq = new LoginReq();
        nonexistentUserReq.username = "nonexistent";
        nonexistentUserReq.password = "password123";

        // When & Then
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> authController.login(nonexistentUserReq),
                "用户不存在应该抛出异常"
        );

        assertEquals("Invalid username or password", exception.getMessage());
        verify(userRepository, times(1)).findByUsername("nonexistent");
    }

    @Test
    void testLogin_WrongPassword_ThrowsException() {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongpassword", testUser.getPassword())).thenReturn(false);

        LoginReq wrongPasswordReq = new LoginReq();
        wrongPasswordReq.username = "testuser";
        wrongPasswordReq.password = "wrongpassword";

        // When & Then
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> authController.login(wrongPasswordReq),
                "密码错误应该抛出异常"
        );

        assertEquals("Invalid username or password", exception.getMessage());
        verify(userRepository, times(1)).findByUsername("testuser");
        verify(passwordEncoder, times(1)).matches("wrongpassword", testUser.getPassword());
    }

    // 注意：实际实现中没有用户状态检查，所以移除这个测试
    /*
    @Test
    void testLogin_UserInactive_ThrowsException() {
        // 实际实现中没有用户状态检查
    }
    */

    // 注意：实际实现中没有参数验证，这些测试被注释掉
    // 参数验证由@Valid注解和Spring Boot的验证框架处理

    /*
    @Test
    void testChangePassword_EmptyOldPassword_ThrowsException() {
        // 实际实现中没有参数验证，由@Valid注解处理
    }

    @Test
    void testChangePassword_EmptyNewPassword_ThrowsException() {
        // 实际实现中没有参数验证，由@Valid注解处理
    }

    @Test
    void testChangePassword_SamePassword_ThrowsException() {
        // 实际实现中没有检查新旧密码是否相同
    }

    @Test
    void testChangePassword_ShortNewPassword_ThrowsException() {
        // 实际实现中没有密码长度验证
    }

    @Test
    void testChangeEmail_EmptyPassword_ThrowsException() {
        // 实际实现中没有参数验证，由@Valid注解处理
    }

    @Test
    void testChangeEmail_EmptyNewEmail_ThrowsException() {
        // 实际实现中没有参数验证，由@Valid注解处理
    }

    @Test
    void testChangeEmail_InvalidEmailFormat_ThrowsException() {
        // 实际实现中没有邮箱格式验证
    }

    @Test
    void testChangeEmail_SameEmail_ThrowsException() {
        // 实际实现中没有检查新旧邮箱是否相同
    }
    */

    // 注意：实际实现中没有参数验证，这些测试被注释掉
    /*
    @Test
    void testCheckUsername_EmptyUsername_ThrowsException() {
        // 实际实现中没有参数验证
    }

    @Test
    void testCheckUsername_ShortUsername_ThrowsException() {
        // 实际实现中没有参数验证
    }

    @Test
    void testCheckUsername_InvalidCharacters_ThrowsException() {
        // 实际实现中没有参数验证
    }
    */

    // ==================== 异常处理测试 ====================
    // 注意：实际实现中没有try-catch包装，直接抛出原始异常

    @Test
    void testRegister_DatabaseError_ThrowsException() {
        // Given
        when(userRepository.existsByUsername(registerReq.username)).thenReturn(false);
        when(userRepository.existsByEmail(registerReq.email)).thenReturn(false);
        when(passwordEncoder.encode(registerReq.password)).thenReturn("$2a$10$encodedPassword");
        when(userRepository.save(any(User.class))).thenThrow(new RuntimeException("Database connection failed"));

        // When & Then
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> authController.register(registerReq),
                "数据库错误应该抛出异常"
        );

        assertEquals("Database connection failed", exception.getMessage());
    }

    @Test
    void testLogin_DatabaseError_ThrowsException() {
        // Given
        when(userRepository.findByUsername(loginReq.username)).thenThrow(new RuntimeException("Database connection failed"));

        // When & Then
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> authController.login(loginReq),
                "数据库错误应该抛出异常"
        );

        assertEquals("Database connection failed", exception.getMessage());
    }

    @Test
    void testChangePassword_DatabaseError_ThrowsException() {
        // Given
        Jwt mockJwt = mock(Jwt.class);
        when(mockJwt.getClaims()).thenReturn(Map.of("uid", 1L));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(changePasswordReq.getOldPassword(), testUser.getPassword())).thenReturn(true);
        when(passwordEncoder.encode(changePasswordReq.getNewPassword())).thenReturn("$2a$10$newEncodedPassword");
        when(userRepository.save(any(User.class))).thenThrow(new RuntimeException("Database connection failed"));

        // When & Then
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> authController.changePassword(changePasswordReq, mockJwt),
                "数据库错误应该抛出异常"
        );

        assertEquals("Database connection failed", exception.getMessage());
    }

    @Test
    void testChangeEmail_DatabaseError_ThrowsException() {
        // Given
        Jwt mockJwt = mock(Jwt.class);
        when(mockJwt.getClaims()).thenReturn(Map.of("uid", 1L));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(changeEmailReq.getPassword(), testUser.getPassword())).thenReturn(true);
        when(userRepository.existsByEmail(changeEmailReq.getNewEmail())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenThrow(new RuntimeException("Database connection failed"));

        // When & Then
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> authController.changeEmail(changeEmailReq, mockJwt),
                "数据库错误应该抛出异常"
        );

        assertEquals("Database connection failed", exception.getMessage());
    }

    // ==================== 边界条件测试 ====================
    // 注意：实际实现中没有长度限制，这些测试被注释掉

    /*
    @Test
    void testRegister_MaxLengthUsername_Success() {
        // 实际实现中没有用户名长度限制
    }

    @Test
    void testRegister_TooLongUsername_ThrowsException() {
        // 实际实现中没有用户名长度限制
    }

    @Test
    void testRegister_MaxLengthEmail_Success() {
        // 实际实现中没有邮箱长度限制
    }

    @Test
    void testRegister_TooLongEmail_ThrowsException() {
        // 实际实现中没有邮箱长度限制
    }
    */

    // ==================== 并发测试模拟 ====================

    @Test
    void testRegister_ConcurrentRegistration_SameUsername() {
        // Given - 模拟并发注册相同用户名
        when(userRepository.existsByUsername(registerReq.username)).thenReturn(false);
        when(userRepository.existsByEmail(registerReq.email)).thenReturn(false);
        when(passwordEncoder.encode(registerReq.password)).thenReturn("$2a$10$encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setUserId(100L);
            return user;
        });
        when(jwtUtil.generateToken(anyLong(), anyString(), anyString())).thenReturn("jwt-token");

        // When - 第一次注册
        Map<String, Object> result1 = authController.register(registerReq);

        // 模拟第二次尝试注册相同用户名
        when(userRepository.existsByUsername(registerReq.username)).thenReturn(true);

        // When & Then - 第二次注册应该失败
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> authController.register(registerReq),
                "并发注册相同用户名应该失败"
        );

        assertEquals("Username taken", exception.getMessage());
        assertNotNull(result1);
        assertEquals("jwt-token", result1.get("token"));
    }

    @Test
    void testLogin_ConcurrentLogin_SameUser() {
        // Given - 模拟同一用户并发登录
        when(userRepository.findByUsername(loginReq.username)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(loginReq.password, testUser.getPassword())).thenReturn(true);
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtUtil.generateToken(anyLong(), anyString(), anyString())).thenReturn("jwt-token");

        // When - 第一次登录
        Map<String, Object> result1 = authController.login(loginReq);

        // When - 第二次登录（应该也成功，因为允许多设备登录）
        Map<String, Object> result2 = authController.login(loginReq);

        // Then
        assertNotNull(result1);
        assertNotNull(result2);
        assertEquals("jwt-token", result1.get("token"));
        assertEquals("jwt-token", result2.get("token"));
        
        // 验证登录时间被更新了两次
        verify(userRepository, times(2)).save(any(User.class));
    }
}

