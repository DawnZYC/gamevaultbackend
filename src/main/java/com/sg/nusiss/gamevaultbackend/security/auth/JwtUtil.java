package com.sg.nusiss.gamevaultbackend.security.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
public class JwtUtil {

    private final JwtEncoder encoder;
    private final JwtDecoder decoder;
    private final long expirationMinutes;

    public JwtUtil(JwtEncoder encoder,
                   JwtDecoder decoder,
                   @Value("${app.jwt.expiration-minutes:120}") long expirationMinutes) {
        this.encoder = encoder;
        this.decoder = decoder;
        this.expirationMinutes = expirationMinutes;
    }

    /** Backward compatibility: only username (without uid/email) */
    public String generateToken(String username) {
        return generateToken(null, username, null);
    }

    /** New usage: includes uid + username + email */
    public String generateToken(Long uid, String username, String email) {
        Instant now = Instant.now();

        JwtClaimsSet.Builder builder = JwtClaimsSet.builder()
                .issuer("gamevault-auth")
                .issuedAt(now)
                .expiresAt(now.plus(expirationMinutes, ChronoUnit.MINUTES))
                .subject(username);              // sub = username

        if (uid != null)  builder.claim("uid", uid);
        if (email != null) builder.claim("email", email);

        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, builder.build()))
                .getTokenValue();
    }

    /** Read username (sub) */
    public String getUsername(String token) {
        return decoder.decode(token).getSubject();
    }

    /** Optional: read uid/email for convenience when needed */
    public Long getUserId(String token) {
        Object v = decoder.decode(token).getClaims().get("uid");
        return (v instanceof Number) ? ((Number) v).longValue() : null;
    }
    public String getEmail(String token) {
        Object v = decoder.decode(token).getClaims().get("email");
        return v != null ? v.toString() : null;
    }

    /** 验证并解析Token，返回TokenInfo */
    public TokenInfo validateAndParseToken(String token) {
        try {
            // 移除 Bearer 前缀
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            
            Jwt jwt = decoder.decode(token);
            String username = jwt.getSubject();
            Long userId = getUserId(token);
            
            if (username != null && userId != null) {
                return new TokenInfo(true, userId, username);
            }
        } catch (Exception e) {
            // Token无效或过期
        }
        
        return new TokenInfo(false, null, null);
    }

    /** Token 解析结果 */
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
}
