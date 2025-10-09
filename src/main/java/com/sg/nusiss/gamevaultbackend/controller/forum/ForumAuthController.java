package com.sg.nusiss.gamevaultbackend.controller.forum;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.sg.nusiss.gamevaultbackend.entity.forum.ForumAccount;
import com.sg.nusiss.gamevaultbackend.entity.forum.ForumUser;
import com.sg.nusiss.gamevaultbackend.service.forum.ForumAuthService;

import com.sg.nusiss.gamevaultbackend.util.forum.ForumJwtUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * 论坛认证控制器
 */
@RestController
@RequestMapping("/api/forum/auth")
public class ForumAuthController {
    private static final Logger logger = LoggerFactory.getLogger(ForumAuthController.class);

    @Autowired
    private ForumAuthService authService;

    @Autowired
    private ForumJwtUtil jwtUtil;

    /**
     * 注册
     * 请求体: {"username":"xxx", "password":"xxx"}
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> request) {
        try {
            String username = request.get("username");
            String password = request.get("password");

            // 基本验证
            if (username == null || username.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "用户名不能为空"));
            }
            if (password == null || password.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "密码不能为空"));
            }

            // 注册
            ForumAccount account = authService.register(username.trim(), password);

            // 生成token
            String token = jwtUtil.generateToken(account.getUserId(), account.getUsername());

            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("userId", account.getUserId());
            response.put("username", account.getUsername());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("注册失败", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "注册失败"));
        }
    }

    /**
     * 登录
     * 请求体: {"username":"xxx", "password":"xxx"}
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        try {
            String username = request.get("username");
            String password = request.get("password");

            // 基本验证
            if (username == null || username.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "用户名不能为空"));
            }
            if (password == null || password.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "密码不能为空"));
            }

            // 登录
            ForumAccount account = authService.login(username.trim(), password);

            if (account == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "用户名或密码错误"));
            }

            // 生成token
            String token = jwtUtil.generateToken(account.getUserId(), account.getUsername());

            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("userId", account.getUserId());
            response.put("username", account.getUsername());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("登录失败", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "登录失败"));
        }
    }

    /**
     * 获取当前用户信息
     * 需要在请求头带上: Authorization: Bearer <token>
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(
            @RequestAttribute(value = "userId", required = false) Long userId) {

        if (userId == null) {
            logger.error("未登录访问 /me 接口");
            return ResponseEntity.status(401).body(Map.of("error", "未登录"));
        }

        try {
            ForumUser user = authService.getUserById(userId);

            if (user == null) {
                return ResponseEntity.notFound().build();
            }

            Map<String, Object> response = new HashMap<>();
            response.put("userId", user.getUserId());
            response.put("username", user.getUsername());
            response.put("nickname", user.getNickname());
            response.put("status", user.getStatus());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("获取用户信息失败", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "获取失败"));
        }
    }
}

