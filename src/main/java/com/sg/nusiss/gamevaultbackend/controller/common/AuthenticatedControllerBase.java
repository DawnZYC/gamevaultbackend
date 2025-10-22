package com.sg.nusiss.gamevaultbackend.controller.common;

import org.springframework.security.oauth2.jwt.Jwt;

public abstract class AuthenticatedControllerBase {
    protected String extractUserId(Jwt jwt) {
        Object uid = jwt.getClaims().get("uid");
        return (uid instanceof Number num)
                ? String.valueOf(num.longValue())
                : String.valueOf(uid);
    }
}