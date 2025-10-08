package com.sg.nusiss.gamevaultbackend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
/**
 * @ClassName WebSocketAuthInterceptor
 * @Author HUANG ZHENJIA
 * @Date 2025/10/5
 * @Description
 */

@Component
@Slf4j
public class WebSocketAuthInterceptor implements ChannelInterceptor {
    private final JwtDecoder jwtDecoder;

    public WebSocketAuthInterceptor(JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            // 从 header 中获取 token
            List<String> authorization = accessor.getNativeHeader("Authorization");

            if (authorization != null && !authorization.isEmpty()) {
                String token = authorization.get(0);

                // 去掉 "Bearer " 前缀
                if (token.startsWith("Bearer ")) {
                    token = token.substring(7);
                }

                try {
                    // 使用 Spring Security 的 JwtDecoder 解析 token
                    Jwt jwt = jwtDecoder.decode(token);

                    // 创建认证对象
                    Authentication authentication = new JwtAuthenticationToken(jwt, new ArrayList<>());

                    // 设置到 WebSocket 会话中
                    accessor.setUser(authentication);

                    Long userId = jwt.getClaim("uid");
                    log.info("WebSocket 连接认证成功 - 用户ID: {}", userId);

                } catch (Exception e) {
                    log.error("WebSocket Token 验证失败", e);
                }
            }
        }

        return message;
    }

}
