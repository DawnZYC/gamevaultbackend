package com.sg.nusiss.gamevaultbackend.controller.auth;

import com.sg.nusiss.gamevaultbackend.dto.auth.LoginReq;
import com.sg.nusiss.gamevaultbackend.dto.auth.RegisterReq;
import com.sg.nusiss.gamevaultbackend.dto.settings.ChangePasswordReq;
import com.sg.nusiss.gamevaultbackend.dto.settings.ChangeEmailReq;
import com.sg.nusiss.gamevaultbackend.entity.auth.User;
import com.sg.nusiss.gamevaultbackend.repository.auth.UserRepository;
import com.sg.nusiss.gamevaultbackend.security.auth.JwtUtil;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserRepository repo;
    private final BCryptPasswordEncoder encoder;
    private final AuthenticationManager authManager;
    private final JwtUtil jwt;

    public AuthController(UserRepository repo, BCryptPasswordEncoder encoder,
                          AuthenticationManager authManager, JwtUtil jwt) {
        this.repo = repo; this.encoder = encoder; this.authManager = authManager; this.jwt = jwt;
    }

    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody @Valid RegisterReq req) {
        if (repo.existsByUsername(req.username)) throw new RuntimeException("Username taken");
        if (repo.existsByEmail(req.email)) throw new RuntimeException("Email taken");

        User u = new User();
        u.setEmail(req.email);
        u.setUsername(req.username);
        u.setPassword(encoder.encode(req.password));
        u.setRegisterTime(LocalDateTime.now());
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

        authManager.authenticate(new UsernamePasswordAuthenticationToken(principal, req.password));

        User u = repo.findByUsername(principal).orElseThrow();
        u.setLastLoginTime(LocalDateTime.now());
        repo.save(u);

        String token = jwt.generateToken(u.getUserId(), u.getUsername(), u.getEmail());
        return Map.of(
                "token", token,
                "username", u.getUsername(),
                "userId", u.getUserId(),
                "email", u.getEmail()
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

        return Map.of(
                "username", jwtToken.getSubject(),
                "uid", uid,
                "email", email
        );
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
