package com.sg.nusiss.gamevaultbackend.client;

import com.sg.nusiss.gamevaultbackend.dto.shopping.UserDTO;
import com.sg.nusiss.gamevaultbackend.security.auth.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthClient {

    private final JwtUtil jwtUtil;

    /**
     * 直接从 JWT token 解码用户信息，不再远程调用
     */
    public UserDTO getCurrentUser(String token) {
        try {
            UserDTO user = new UserDTO();
            user.setUserId(jwtUtil.getUserId(token));
            user.setUsername(jwtUtil.getUsername(token));
            user.setEmail(jwtUtil.getEmail(token));
            return user;
        } catch (Exception e) {
            // fallback，防止 token 错误时崩溃
            UserDTO fallback = new UserDTO();
            fallback.setUserId(0L);
            fallback.setUsername("anonymous");
            fallback.setEmail("anonymous@example.com");
            return fallback;
        }
    }

    public Long getCurrentUserId(String token) {
        try {
            return jwtUtil.getUserId(token);
        } catch (Exception e) {
            return null;
        }
    }
}
