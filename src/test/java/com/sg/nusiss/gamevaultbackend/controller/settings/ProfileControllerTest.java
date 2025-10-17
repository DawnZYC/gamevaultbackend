package com.sg.nusiss.gamevaultbackend.controller.settings;

import com.sg.nusiss.gamevaultbackend.dto.settings.UpdateProfileReq;
import com.sg.nusiss.gamevaultbackend.entity.auth.User;
import com.sg.nusiss.gamevaultbackend.repository.auth.UserRepository;
import com.sg.nusiss.gamevaultbackend.service.FileUploadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.lenient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * @ClassName ProfileControllerTest
 * @Description 用户资料控制器单元测试 - 测试头像上传、资料获取和更新功能
 * @Date 2025/10/17
 */
@ExtendWith(MockitoExtension.class)
public class ProfileControllerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private FileUploadService fileUploadService;

    @InjectMocks
    private ProfileController profileController;

    private User testUser;
    private Jwt mockJwt;
    private UpdateProfileReq updateProfileReq;

    @BeforeEach
    void setUp() {
        // 准备测试用户
        testUser = new User();
        testUser.setUserId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setNickname("Test User");
        testUser.setBio("Test bio");
        testUser.setAvatarUrl("/uploads/avatars/old_avatar.jpg");
        testUser.setCreatedDate(LocalDateTime.now().minusDays(10));
        testUser.setUpdatedDate(LocalDateTime.now().minusDays(1));

        // Mock JWT - 使用lenient以避免在某些测试中未使用时出错
        mockJwt = mock(Jwt.class);
        lenient().when(mockJwt.getClaims()).thenReturn(Map.of("uid", 1L));

        // 准备更新资料请求
        updateProfileReq = new UpdateProfileReq();
        updateProfileReq.setNickname("New Nickname");
        updateProfileReq.setBio("New bio");
        updateProfileReq.setEmail("newemail@example.com");
    }

    // ==================== getProfile 方法测试 ====================

    @Test
    void testGetProfile_Success() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When
        ResponseEntity<Map<String, Object>> response = profileController.getProfile(mockJwt);

        // Then
        assertNotNull(response, "返回结果不应为null");
        assertEquals(HttpStatus.OK, response.getStatusCode(), "状态码应为200");

        Map<String, Object> result = response.getBody();
        assertNotNull(result, "响应体不应为null");
        assertEquals(1L, result.get("userId"), "用户ID应该匹配");
        assertEquals("testuser", result.get("username"), "用户名应该匹配");
        assertEquals("test@example.com", result.get("email"), "邮箱应该匹配");
        assertEquals("Test User", result.get("nickname"), "昵称应该匹配");
        assertEquals("Test bio", result.get("bio"), "个人简介应该匹配");
        assertEquals("/uploads/avatars/old_avatar.jpg", result.get("avatarUrl"), "头像URL应该匹配");
        assertNotNull(result.get("createdDate"), "创建时间不应为null");
        assertNotNull(result.get("updatedDate"), "更新时间不应为null");

        verify(userRepository, times(1)).findById(1L);
    }

    @Test
    void testGetProfile_UserNotFound() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // When
        ResponseEntity<Map<String, Object>> response = profileController.getProfile(mockJwt);

        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode(), "状态码应为404");
        verify(userRepository, times(1)).findById(1L);
    }

    @Test
    void testGetProfile_InvalidJwt_ReturnsError() {
        // Given - JWT中没有uid
        Jwt invalidJwt = mock(Jwt.class);
        when(invalidJwt.getClaims()).thenReturn(Map.of());

        // When
        ResponseEntity<Map<String, Object>> response = profileController.getProfile(invalidJwt);

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode(), 
                "状态码应为500");
        
        Map<String, Object> result = response.getBody();
        assertNotNull(result, "响应体不应为null");
        assertTrue(result.containsKey("error"), "应包含错误信息");

        verify(userRepository, never()).findById(anyLong());
    }

    @Test
    void testGetProfile_ExceptionHandling() {
        // Given
        when(userRepository.findById(1L)).thenThrow(new RuntimeException("Database error"));

        // When
        ResponseEntity<Map<String, Object>> response = profileController.getProfile(mockJwt);

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode(), 
                "状态码应为500");
        
        Map<String, Object> result = response.getBody();
        assertNotNull(result, "响应体不应为null");
        assertTrue(result.get("error").toString().contains("获取用户资料失败"), 
                "应包含错误信息");
    }

    // ==================== updateProfile 方法测试 ====================

    @Test
    void testUpdateProfile_Success() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByEmail("newemail@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        ResponseEntity<Map<String, Object>> response = 
                profileController.updateProfile(mockJwt, updateProfileReq);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode(), "状态码应为200");

        Map<String, Object> result = response.getBody();
        assertNotNull(result, "响应体不应为null");
        assertEquals("用户资料更新成功", result.get("message"), "应返回成功消息");
        assertEquals(1L, result.get("userId"), "用户ID应该匹配");
        assertEquals("New Nickname", result.get("nickname"), "昵称应该被更新");
        assertEquals("New bio", result.get("bio"), "个人简介应该被更新");
        assertEquals("newemail@example.com", result.get("email"), "邮箱应该被更新");

        // 验证用户数据被更新
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertEquals("New Nickname", savedUser.getNickname(), "昵称应该被更新");
        assertEquals("New bio", savedUser.getBio(), "个人简介应该被更新");
        assertEquals("newemail@example.com", savedUser.getEmail(), "邮箱应该被更新");

        verify(userRepository, times(1)).findById(1L);
        verify(userRepository, times(1)).existsByEmail("newemail@example.com");
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testUpdateProfile_OnlyNickname() {
        // Given - 只更新昵称
        UpdateProfileReq req = new UpdateProfileReq();
        req.setNickname("Only Nickname");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        ResponseEntity<Map<String, Object>> response = 
                profileController.updateProfile(mockJwt, req);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode(), "状态码应为200");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertEquals("Only Nickname", savedUser.getNickname(), "昵称应该被更新");
        assertEquals("test@example.com", savedUser.getEmail(), "邮箱不应该改变");
        assertEquals("Test bio", savedUser.getBio(), "个人简介不应该改变");

        verify(userRepository, never()).existsByEmail(anyString());
    }

    @Test
    void testUpdateProfile_EmailAlreadyExists() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByEmail("newemail@example.com")).thenReturn(true);

        // When
        ResponseEntity<Map<String, Object>> response = 
                profileController.updateProfile(mockJwt, updateProfileReq);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode(), "状态码应为400");

        Map<String, Object> result = response.getBody();
        assertNotNull(result, "响应体不应为null");
        assertEquals("邮箱已被其他用户使用", result.get("error"), "应返回错误消息");

        verify(userRepository, times(1)).findById(1L);
        verify(userRepository, times(1)).existsByEmail("newemail@example.com");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testUpdateProfile_SameEmail_NoCheck() {
        // Given - 更新为相同的邮箱，不需要检查
        updateProfileReq.setEmail("test@example.com");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        ResponseEntity<Map<String, Object>> response = 
                profileController.updateProfile(mockJwt, updateProfileReq);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode(), "状态码应为200");

        // 不应该检查邮箱是否存在
        verify(userRepository, never()).existsByEmail(anyString());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testUpdateProfile_UserNotFound() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // When
        ResponseEntity<Map<String, Object>> response = 
                profileController.updateProfile(mockJwt, updateProfileReq);

        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode(), "状态码应为404");
        verify(userRepository, times(1)).findById(1L);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testUpdateProfile_ExceptionHandling() {
        // Given
        when(userRepository.findById(1L)).thenThrow(new RuntimeException("Database error"));

        // When
        ResponseEntity<Map<String, Object>> response = 
                profileController.updateProfile(mockJwt, updateProfileReq);

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode(), 
                "状态码应为500");
        
        Map<String, Object> result = response.getBody();
        assertNotNull(result, "响应体不应为null");
        assertTrue(result.get("error").toString().contains("更新用户资料失败"), 
                "应包含错误信息");
    }

    // ==================== uploadAvatar 方法测试 ====================

    @Test
    void testUploadAvatar_Success() throws IOException {
        // Given
        MultipartFile mockFile = mock(MultipartFile.class);
        String newAvatarUrl = "/uploads/avatars/user_1_avatar_12345678.jpg";
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(fileUploadService.uploadAvatar(mockFile, 1L)).thenReturn(newAvatarUrl);
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(fileUploadService.deleteAvatar(anyString())).thenReturn(true);

        // When
        ResponseEntity<Map<String, Object>> response = 
                profileController.uploadAvatar(mockJwt, mockFile);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode(), "状态码应为200");

        Map<String, Object> result = response.getBody();
        assertNotNull(result, "响应体不应为null");
        assertEquals("头像上传成功", result.get("message"), "应返回成功消息");
        assertEquals(newAvatarUrl, result.get("avatarUrl"), "应返回新头像URL");

        // 验证用户头像被更新
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertEquals(newAvatarUrl, savedUser.getAvatarUrl(), "头像URL应该被更新");

        // 验证旧头像被删除
        verify(fileUploadService, times(1)).deleteAvatar("/uploads/avatars/old_avatar.jpg");
        verify(userRepository, times(1)).findById(1L);
        verify(fileUploadService, times(1)).uploadAvatar(mockFile, 1L);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testUploadAvatar_FirstTimeUpload_NoOldAvatar() throws IOException {
        // Given - 第一次上传头像，没有旧头像
        testUser.setAvatarUrl(null);
        MultipartFile mockFile = mock(MultipartFile.class);
        String newAvatarUrl = "/uploads/avatars/user_1_avatar_12345678.jpg";
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(fileUploadService.uploadAvatar(mockFile, 1L)).thenReturn(newAvatarUrl);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        ResponseEntity<Map<String, Object>> response = 
                profileController.uploadAvatar(mockJwt, mockFile);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode(), "状态码应为200");

        // 不应该尝试删除旧头像
        verify(fileUploadService, never()).deleteAvatar(anyString());
        verify(fileUploadService, times(1)).uploadAvatar(mockFile, 1L);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testUploadAvatar_EmptyOldAvatarUrl() throws IOException {
        // Given - 旧头像URL为空字符串
        testUser.setAvatarUrl("");
        MultipartFile mockFile = mock(MultipartFile.class);
        String newAvatarUrl = "/uploads/avatars/user_1_avatar_12345678.jpg";
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(fileUploadService.uploadAvatar(mockFile, 1L)).thenReturn(newAvatarUrl);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        ResponseEntity<Map<String, Object>> response = 
                profileController.uploadAvatar(mockJwt, mockFile);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode(), "状态码应为200");

        // 不应该尝试删除旧头像
        verify(fileUploadService, never()).deleteAvatar(anyString());
    }

    @Test
    void testUploadAvatar_UserNotFound() throws IOException {
        // Given
        MultipartFile mockFile = mock(MultipartFile.class);
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // When
        ResponseEntity<Map<String, Object>> response = 
                profileController.uploadAvatar(mockJwt, mockFile);

        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode(), "状态码应为404");
        verify(userRepository, times(1)).findById(1L);
        verify(fileUploadService, never()).uploadAvatar(any(), anyLong());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testUploadAvatar_IllegalArgumentException() throws IOException {
        // Given - 文件验证失败
        MultipartFile mockFile = mock(MultipartFile.class);
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(fileUploadService.uploadAvatar(mockFile, 1L))
                .thenThrow(new IllegalArgumentException("文件大小不能超过5MB"));

        // When
        ResponseEntity<Map<String, Object>> response = 
                profileController.uploadAvatar(mockJwt, mockFile);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode(), "状态码应为400");

        Map<String, Object> result = response.getBody();
        assertNotNull(result, "响应体不应为null");
        assertEquals("文件大小不能超过5MB", result.get("error"), "应返回错误消息");

        verify(userRepository, times(1)).findById(1L);
        verify(fileUploadService, times(1)).uploadAvatar(mockFile, 1L);
        verify(userRepository, never()).save(any(User.class));
        verify(fileUploadService, never()).deleteAvatar(anyString());
    }

    @Test
    void testUploadAvatar_IOException() throws IOException {
        // Given
        MultipartFile mockFile = mock(MultipartFile.class);
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(fileUploadService.uploadAvatar(mockFile, 1L))
                .thenThrow(new IOException("Failed to save file"));

        // When
        ResponseEntity<Map<String, Object>> response = 
                profileController.uploadAvatar(mockJwt, mockFile);

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode(), 
                "状态码应为500");

        Map<String, Object> result = response.getBody();
        assertNotNull(result, "响应体不应为null");
        assertTrue(result.get("error").toString().contains("头像上传失败"), 
                "应包含错误信息");
    }

    @Test
    void testUploadAvatar_OldAvatarDeleteFails_StillSuccess() throws IOException {
        // Given - 旧头像删除失败，但上传仍然成功
        MultipartFile mockFile = mock(MultipartFile.class);
        String newAvatarUrl = "/uploads/avatars/user_1_avatar_12345678.jpg";
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(fileUploadService.uploadAvatar(mockFile, 1L)).thenReturn(newAvatarUrl);
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(fileUploadService.deleteAvatar(anyString())).thenReturn(false);

        // When
        ResponseEntity<Map<String, Object>> response = 
                profileController.uploadAvatar(mockJwt, mockFile);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode(), 
                "即使旧头像删除失败，状态码也应为200");

        Map<String, Object> result = response.getBody();
        assertEquals("头像上传成功", result.get("message"), "应返回成功消息");

        verify(fileUploadService, times(1)).deleteAvatar("/uploads/avatars/old_avatar.jpg");
        verify(userRepository, times(1)).save(any(User.class));
    }

    // ==================== deleteAvatar 方法测试 ====================

    @Test
    void testDeleteAvatar_Success() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(fileUploadService.deleteAvatar("/uploads/avatars/old_avatar.jpg")).thenReturn(true);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        ResponseEntity<Map<String, Object>> response = 
                profileController.deleteAvatar(mockJwt);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode(), "状态码应为200");

        Map<String, Object> result = response.getBody();
        assertNotNull(result, "响应体不应为null");
        assertEquals("头像删除成功", result.get("message"), "应返回成功消息");

        // 验证用户头像URL被清空
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertNull(savedUser.getAvatarUrl(), "头像URL应该被设置为null");

        verify(userRepository, times(1)).findById(1L);
        verify(fileUploadService, times(1)).deleteAvatar("/uploads/avatars/old_avatar.jpg");
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testDeleteAvatar_NoAvatar_StillSuccess() {
        // Given - 用户没有头像
        testUser.setAvatarUrl(null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When
        ResponseEntity<Map<String, Object>> response = 
                profileController.deleteAvatar(mockJwt);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode(), "状态码应为200");

        // 不应该调用文件删除服务或保存用户
        verify(fileUploadService, never()).deleteAvatar(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testDeleteAvatar_EmptyAvatarUrl_StillSuccess() {
        // Given - 头像URL为空字符串
        testUser.setAvatarUrl("");
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When
        ResponseEntity<Map<String, Object>> response = 
                profileController.deleteAvatar(mockJwt);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode(), "状态码应为200");

        // 不应该调用文件删除服务或保存用户
        verify(fileUploadService, never()).deleteAvatar(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testDeleteAvatar_UserNotFound() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // When
        ResponseEntity<Map<String, Object>> response = 
                profileController.deleteAvatar(mockJwt);

        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode(), "状态码应为404");
        verify(userRepository, times(1)).findById(1L);
        verify(fileUploadService, never()).deleteAvatar(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testDeleteAvatar_FileDeleteFails_StillSuccess() {
        // Given - 文件删除失败，但数据库更新成功
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(fileUploadService.deleteAvatar("/uploads/avatars/old_avatar.jpg")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        ResponseEntity<Map<String, Object>> response = 
                profileController.deleteAvatar(mockJwt);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode(), 
                "即使文件删除失败，状态码也应为200");

        // 仍然应该更新数据库
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertNull(savedUser.getAvatarUrl(), "头像URL应该被设置为null");
    }

    @Test
    void testDeleteAvatar_ExceptionHandling() {
        // Given
        when(userRepository.findById(1L)).thenThrow(new RuntimeException("Database error"));

        // When
        ResponseEntity<Map<String, Object>> response = 
                profileController.deleteAvatar(mockJwt);

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode(), 
                "状态码应为500");
        
        Map<String, Object> result = response.getBody();
        assertNotNull(result, "响应体不应为null");
        assertTrue(result.get("error").toString().contains("头像删除失败"), 
                "应包含错误信息");
    }
}

