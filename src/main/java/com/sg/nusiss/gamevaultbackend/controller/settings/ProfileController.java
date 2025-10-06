package com.sg.nusiss.gamevaultbackend.controller.settings;

import com.sg.nusiss.gamevaultbackend.dto.settings.UpdateProfileReq;
import com.sg.nusiss.gamevaultbackend.entity.auth.User;
import com.sg.nusiss.gamevaultbackend.repository.auth.UserRepository;
import com.sg.nusiss.gamevaultbackend.service.FileUploadService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/settings")
public class ProfileController {

    private final UserRepository userRepository;
    private final FileUploadService fileUploadService;

    public ProfileController(UserRepository userRepository, FileUploadService fileUploadService) {
        this.userRepository = userRepository;
        this.fileUploadService = fileUploadService;
    }

    /**
     * 获取当前用户资料
     */
    @GetMapping("/profile")
    public ResponseEntity<Map<String, Object>> getProfile(
            @AuthenticationPrincipal Jwt jwt) {
        try {
            Long userId = getUserIdFromJwt(jwt);
            Optional<User> userOpt = userRepository.findById(userId);
            
            if (userOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            User user = userOpt.get();
            Map<String, Object> profile = new HashMap<>();
            profile.put("userId", user.getUserId());
            profile.put("username", user.getUsername());
            profile.put("email", user.getEmail());
            profile.put("nickname", user.getNickname());
            profile.put("bio", user.getBio());
            profile.put("avatarUrl", user.getAvatarUrl());
            profile.put("createdDate", user.getCreatedDate());
            profile.put("updatedDate", user.getUpdatedDate());
            
            return ResponseEntity.ok(profile);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "获取用户资料失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 更新用户资料
     */
    @PutMapping("/profile")
    public ResponseEntity<Map<String, Object>> updateProfile(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid UpdateProfileReq request) {
        try {
            Long userId = getUserIdFromJwt(jwt);
            Optional<User> userOpt = userRepository.findById(userId);
            
            if (userOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            User user = userOpt.get();
            
            // 检查邮箱是否被其他用户使用
            if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
                if (userRepository.existsByEmail(request.getEmail())) {
                    Map<String, Object> error = new HashMap<>();
                    error.put("error", "邮箱已被其他用户使用");
                    return ResponseEntity.badRequest().body(error);
                }
                user.setEmail(request.getEmail());
            }
            
            // 更新其他字段
            if (request.getNickname() != null) {
                user.setNickname(request.getNickname());
            }
            if (request.getBio() != null) {
                user.setBio(request.getBio());
            }
            if (request.getAvatarUrl() != null) {
                user.setAvatarUrl(request.getAvatarUrl());
            }
            
            userRepository.save(user);
            
            Map<String, Object> result = new HashMap<>();
            result.put("message", "用户资料更新成功");
            result.put("userId", user.getUserId());
            result.put("username", user.getUsername());
            result.put("email", user.getEmail());
            result.put("nickname", user.getNickname());
            result.put("bio", user.getBio());
            result.put("avatarUrl", user.getAvatarUrl());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "更新用户资料失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 上传头像
     */
    @PostMapping("/avatar")
    public ResponseEntity<Map<String, Object>> uploadAvatar(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam("file") MultipartFile file) {
        try {
            Long userId = getUserIdFromJwt(jwt);
            Optional<User> userOpt = userRepository.findById(userId);
            
            if (userOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            // 上传文件
            String avatarUrl = fileUploadService.uploadAvatar(file, userId);
            
            // 更新用户头像URL
            User user = userOpt.get();
            String oldAvatarUrl = user.getAvatarUrl();
            user.setAvatarUrl(avatarUrl);
            userRepository.save(user);
            
            // 删除旧头像文件
            if (oldAvatarUrl != null && !oldAvatarUrl.isEmpty()) {
                fileUploadService.deleteAvatar(oldAvatarUrl);
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("message", "头像上传成功");
            result.put("avatarUrl", avatarUrl);
            
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "头像上传失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 删除头像
     */
    @DeleteMapping("/avatar")
    public ResponseEntity<Map<String, Object>> deleteAvatar(
            @AuthenticationPrincipal Jwt jwt) {
        try {
            Long userId = getUserIdFromJwt(jwt);
            Optional<User> userOpt = userRepository.findById(userId);
            
            if (userOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            User user = userOpt.get();
            String avatarUrl = user.getAvatarUrl();
            
            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                // 删除文件
                fileUploadService.deleteAvatar(avatarUrl);
                
                // 清空数据库中的头像URL
                user.setAvatarUrl(null);
                userRepository.save(user);
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("message", "头像删除成功");
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "头像删除失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 从JWT中获取用户ID
     */
    private Long getUserIdFromJwt(Jwt jwt) {
        Object uidClaim = jwt.getClaims().get("uid");
        if (uidClaim instanceof Number) {
            return ((Number) uidClaim).longValue();
        }
        throw new RuntimeException("无法从JWT中获取用户ID");
    }
}
