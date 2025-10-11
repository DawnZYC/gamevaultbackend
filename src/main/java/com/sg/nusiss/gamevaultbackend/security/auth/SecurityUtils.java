package com.sg.nusiss.gamevaultbackend.security.auth;

import com.sg.nusiss.gamevaultbackend.exception.BusinessException;
import com.sg.nusiss.gamevaultbackend.common.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.security.oauth2.jwt.Jwt;

/**
 * @ClassName SecurityUtils
 * @Author HUANG ZHENJIA
 * @Date 2025/10/4
 * @Description
 */

public class SecurityUtils {

    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        }

        // 从 JWT 获取 userId
        if (authentication.getPrincipal() instanceof Jwt) {
            Jwt jwt = (Jwt) authentication.getPrincipal();
            Long userId = jwt.getClaim("uid"); // 你的 JWT 中 userId 字段是 "uid"

            if (userId == null) {
                throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "无效的认证信息");
            }

            return userId;
        }

        throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
    }
}

