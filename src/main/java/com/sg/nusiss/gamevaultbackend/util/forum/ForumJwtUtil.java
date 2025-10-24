package com.sg.nusiss.gamevaultbackend.util.forum;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

@Component
public class ForumJwtUtil {
    private static final Logger logger = LoggerFactory.getLogger(ForumJwtUtil.class);

    @Autowired
    private JwtDecoder jwtDecoder; // 使用gamevaultbackend的RS256 JwtDecoder

    @Autowired
    private JwtEncoder jwtEncoder; // 使用gamevaultbackend的RS256 JwtEncoder

    @Value("${auth.mock.enabled:false}")
    private boolean mockEnabled;

    @Value("${app.jwt.expiration-minutes:120}")
    private long expirationMinutes;

    // 预定义的测试 Token 映射
    private static final Map<String, MockUser> MOCK_TOKENS = new HashMap<>() {{
        put("test-token-123", new MockUser(1L, "testuser", "测试用户"));
        put("test-token-admin", new MockUser(2L, "admin", "管理员"));
        put("test-token-user2", new MockUser(3L, "user2", "用户2"));
    }};

    // 解析RS256 JWT（使用gamevaultbackend的JwtDecoder）
    public Claims parseTokenForRS256(String token) {
        try {
            // 使用注入的JwtDecoder解析RS256 JWT
            Jwt jwt = jwtDecoder.decode(token);
            
            // 将JWT转换为Claims格式
            Claims claims = Jwts.claims(jwt.getClaims());
            logger.debug("RS256 JWT解析成功: {}", jwt.getClaims());
            return claims;
        } catch (Exception e) {
            logger.debug("RS256解析失败: {}", e.getMessage());
            return null;
        }
    }

    // 生成 JWT Token（使用RS256算法，与gamevaultbackend统一）
    public String generateToken(Long userId, String username) {
        return generateToken(userId, username, null);
    }

    // 生成 JWT Token（完整版本，支持email）
    public String generateToken(Long userId, String username, String email) {
        Instant now = Instant.now();

        JwtClaimsSet.Builder builder = JwtClaimsSet.builder()
                .issuer("gamevault-auth")
                .issuedAt(now)
                .expiresAt(now.plus(expirationMinutes, ChronoUnit.MINUTES))
                .subject(username);              // sub = username

        if (userId != null) builder.claim("uid", userId);
        if (email != null) builder.claim("email", email);

        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, builder.build()))
                .getTokenValue();
        
        logger.debug("论坛系统生成JWT token: userId={}, username={}", userId, username);
        return token;
    }

    // 验证并解析 Token
    public TokenInfo validateAndParseToken(String token) {
        // 移除 Bearer 前缀
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        // 开发环境：检查是否是 Mock Token
        if (mockEnabled && MOCK_TOKENS.containsKey(token)) {
            MockUser mockUser = MOCK_TOKENS.get(token);
            logger.debug("使用 Mock Token: {} -> {}", token, mockUser.username);
            return new TokenInfo(true, mockUser.userId, mockUser.username);
        }

        // 只使用RS256算法（与gamevaultbackend统一）
        try {
            Claims rs256Claims = parseTokenForRS256(token);
            if (rs256Claims != null) {
                // 从RS256 JWT中提取信息
                String username = rs256Claims.getSubject();
                Long userId = rs256Claims.get("uid", Long.class);
                if (userId == null) {
                    userId = rs256Claims.get("userId", Long.class);
                }
                
                if (username != null && userId != null) {
                    logger.debug("成功解析RS256 JWT: username={}, userId={}", username, userId);
                    return new TokenInfo(true, userId, username);
                }
            }
        } catch (Exception e) {
            logger.error("RS256解析失败: {}", e.getMessage());
        }

        return new TokenInfo(false, null, null);
    }

    // Token 解析结果
    public static class TokenInfo {
        public final boolean valid;
        public final Long userId;
        public final String username;

        public TokenInfo(boolean valid, Long userId, String username) {
            this.valid = valid;
            this.userId = userId;
            this.username = username;
        }
    }

    // Mock 用户
    private static class MockUser {
        final Long userId;
        final String username;
        final String nickname;

        MockUser(Long userId, String username, String nickname) {
            this.userId = userId;
            this.username = username;
            this.nickname = nickname;
        }
    }
}

