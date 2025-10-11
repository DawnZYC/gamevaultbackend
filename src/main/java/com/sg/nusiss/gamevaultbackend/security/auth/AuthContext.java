package com.sg.nusiss.gamevaultbackend.security.auth;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class AuthContext {

    public Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            Object uid = jwt.getClaims().get("uid");
            if (uid instanceof Number) {
                return ((Number) uid).longValue();
            }
        }
        return null;
    }
}
