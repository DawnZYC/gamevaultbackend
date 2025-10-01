package com.sg.nusiss.gamevaultbackend.controller.auth;

import com.sg.nusiss.gamevaultbackend.dto.auth.LoginReq;
import com.sg.nusiss.gamevaultbackend.dto.auth.RegisterReq;
import com.sg.nusiss.gamevaultbackend.dto.settings.ChangePasswordReq;
import com.sg.nusiss.gamevaultbackend.dto.settings.ChangeEmailReq;
import com.sg.nusiss.gamevaultbackend.entity.auth.User;
import com.sg.nusiss.gamevaultbackend.repository.auth.UserRepository;
import com.sg.nusiss.gamevaultbackend.security.auth.JwtUtil;
import jakarta.validation.Valid;
// 移除 AuthenticationManager 相关导入，手动验证用户凭据
// import org.springframework.security.authentication.AuthenticationManager;
// import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserRepository repo;
    private final BCryptPasswordEncoder encoder;
    private final JwtUtil jwt;

    public AuthController(UserRepository repo, BCryptPasswordEncoder encoder, JwtUtil jwt) {
        this.repo = repo; this.encoder = encoder; this.jwt = jwt;
    }

    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody @Valid RegisterReq req) {
        if (repo.existsByUsername(req.username)) throw new RuntimeException("Username taken");
        if (repo.existsByEmail(req.email)) throw new RuntimeException("Email taken");

        User u = new User();
        u.setEmail(req.email);
        u.setUsername(req.username);
        u.setPassword(encoder.encode(req.password));
        u.setCreatedDate(LocalDateTime.now());
        u = repo.save(u); // Ensure we get the generated userId

        String token = jwt.generateToken(u.getUserId(), u.getUsername(), u.getEmail());
        return Map.of(
                "token", token,
                "username", u.getUsername(),
                "userId", u.getUserId(),
                "email", u.getEmail()
        );
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody @Valid LoginReq req) {
        // Support email login: if email is provided, convert to corresponding username for authentication
        String principal = req.username;
        if (principal != null && principal.contains("@")) {
            principal = repo.findByEmail(principal)
                    .map(User::getUsername)
                    .orElse(principal); // If email doesn't exist, let authentication fail
        }

        // 手动验证用户凭据，避免使用 AuthenticationManager 造成循环依赖
        User u = repo.findByUsername(principal)
                .orElseThrow(() -> new RuntimeException("Invalid username or password"));
        
        // 验证密码
        if (!encoder.matches(req.password, u.getPassword())) {
            throw new RuntimeException("Invalid username or password");
        }

        // 更新最后登录时间
        u.setLastLoginTime(LocalDateTime.now());
        repo.save(u);

        String token = jwt.generateToken(u.getUserId(), u.getUsername(), u.getEmail());
        return Map.of(
                "token", token,
                "userId", u.getUserId(),
                "username", u.getUsername(),
                "email", u.getEmail(),
                "message", "Login successful"
        );
    }

    // Email uniqueness validation: used for frontend registration form async validation
    @GetMapping("/check-email")
    public Map<String, Boolean> checkEmail(@RequestParam("email") String email) {
        boolean exists = repo.existsByEmail(email);
        return Map.of("exists", exists);
    }

    // Username uniqueness validation: used for frontend registration form async validation
    @GetMapping("/check-username")
    public Map<String, Boolean> checkUsername(@RequestParam("username") String username) {
        boolean exists = repo.existsByUsername(username);
        return Map.of("exists", exists);
    }

    // Protected endpoint: read username/uid/email from JWT
    @GetMapping("/me")
    public Map<String,Object> me(@org.springframework.security.core.annotation.AuthenticationPrincipal
                                 org.springframework.security.oauth2.jwt.Jwt jwtToken) {
        // Backward compatibility for old tokens: may not have uid/email
        Long uid = null;
        Object uidClaim = jwtToken.getClaims().get("uid");
        if (uidClaim instanceof Number) uid = ((Number) uidClaim).longValue();

        String email = null;
        Object emailClaim = jwtToken.getClaims().get("email");
        if (emailClaim != null) email = emailClaim.toString();

        // 如果可以从数据库获取完整用户信息，优先使用数据库数据
        if (uid != null) {
            try {
                User user = repo.findById(uid).orElse(null);
                if (user != null) {
                    Map<String, Object> result = new HashMap<>();
                    result.put("userId", user.getUserId());
                    result.put("username", user.getUsername());
                    result.put("email", user.getEmail() != null ? user.getEmail() : "");
                    result.put("profile", null);  // 暂时为 null，后续可以扩展
                    result.put("createdAt", user.getCreatedDate() != null ? user.getCreatedDate().toString() : "2024-01-01T00:00:00.000Z");
                    result.put("updatedAt", user.getUpdatedDate() != null ? user.getUpdatedDate().toString() : "2024-01-01T00:00:00.000Z");
                    return result;
                }
            } catch (Exception e) {
                // 记录错误日志，然后回退到JWT数据
                System.err.println("Failed to fetch user from database: " + e.getMessage());
                e.printStackTrace();
                // 不要抛出异常，继续执行回退逻辑
            }
        }
        
        // 回退到JWT中的数据
        Map<String, Object> result = new HashMap<>();
        result.put("userId", uid != null ? uid : 0L);  // 前端期望 userId
        result.put("username", jwtToken.getSubject());
        result.put("email", email != null ? email : "");
        result.put("profile", null);  // 暂时为 null，后续可以扩展
        result.put("createdAt", "2024-01-01T00:00:00.000Z");  // 暂时使用默认值
        result.put("updatedAt", "2024-01-01T00:00:00.000Z");   // 暂时使用默认值
        return result;
    }

    // Logout endpoint: since JWT is stateless, we just return success
    // The client should clear the token from localStorage
    @PostMapping("/logout")
    public Map<String, Object> logout() {
        return Map.of(
                "message", "Logout successful",
                "success", true
        );
    }

    // Change password endpoint
    @PutMapping("/change-password")
    public Map<String, Object> changePassword(@RequestBody @Valid ChangePasswordReq req,
                                               @org.springframework.security.core.annotation.AuthenticationPrincipal
                                               org.springframework.security.oauth2.jwt.Jwt jwtToken) {
        // Get user ID from JWT
        Long userId = null;
        Object uidClaim = jwtToken.getClaims().get("uid");
        if (uidClaim instanceof Number) {
            userId = ((Number) uidClaim).longValue();
        }
        
        if (userId == null) {
            throw new RuntimeException("Invalid user ID");
        }

        // Find user
        User user = repo.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

        // Verify old password
        if (!encoder.matches(req.getOldPassword(), user.getPassword())) {
            throw new RuntimeException("Current password is incorrect");
        }

        // Update password
        user.setPassword(encoder.encode(req.getNewPassword()));
        repo.save(user);

        return Map.of(
                "message", "Password changed successfully",
                "success", true
        );
    }

    // Change email endpoint
    @PutMapping("/change-email")
    public Map<String, Object> changeEmail(@RequestBody @Valid ChangeEmailReq req,
                                           @org.springframework.security.core.annotation.AuthenticationPrincipal
                                           org.springframework.security.oauth2.jwt.Jwt jwtToken) {
        // Get user ID from JWT
        Long userId = null;
        Object uidClaim = jwtToken.getClaims().get("uid");
        if (uidClaim instanceof Number) {
            userId = ((Number) uidClaim).longValue();
        }
        
        if (userId == null) {
            throw new RuntimeException("Invalid user ID");
        }

        // Find user
        User user = repo.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

        // Verify password
        if (!encoder.matches(req.getPassword(), user.getPassword())) {
            throw new RuntimeException("Password is incorrect");
        }

        // Check if new email already exists
        if (repo.existsByEmail(req.getNewEmail())) {
            throw new RuntimeException("Email already in use");
        }

        // Update email
        user.setEmail(req.getNewEmail());
        repo.save(user);

        return Map.of(
                "message", "Email changed successfully",
                "success", true
        );
    }
}
