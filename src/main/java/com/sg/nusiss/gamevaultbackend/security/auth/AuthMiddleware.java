package com.sg.nusiss.gamevaultbackend.security.auth;

import com.sg.nusiss.gamevaultbackend.security.auth.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import java.io.IOException;
import java.util.ArrayList;

/**
 * 认证中间件 - 处理JWT token验证
 * 优化版本：减少重复验证，提高性能
 */
@Component
public class AuthMiddleware extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final JwtDecoder jwtDecoder;

    public AuthMiddleware(JwtUtil jwtUtil, JwtDecoder jwtDecoder) {
        this.jwtUtil = jwtUtil;
        this.jwtDecoder = jwtDecoder;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                  HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        final String requestURI = request.getRequestURI();
        
        // 跳过不需要认证的端点
        if (isPublicEndpoint(requestURI)) {
            filterChain.doFilter(request, response);
            return;
        }

        final String authHeader = request.getHeader("Authorization");
        final String jwt;

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // 对于需要认证的端点，如果没有token，返回401
            if (isProtectedEndpoint(requestURI)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Missing or invalid authorization header\"}");
                return;
            }
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);

        try {
            // 验证JWT token
            Jwt decodedJwt = jwtDecoder.decode(jwt);
            
            // 检查token是否过期
            if (decodedJwt.getExpiresAt() != null && 
                decodedJwt.getExpiresAt().isBefore(java.time.Instant.now())) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Token expired\"}");
                return;
            }

            // 设置认证上下文
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    decodedJwt.getSubject(), 
                    null, 
                    new ArrayList<>()
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }

        } catch (JwtException e) {
            // Token无效
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Invalid token: " + e.getMessage() + "\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 检查是否是公开端点（不需要认证）
     */
    private boolean isPublicEndpoint(String requestURI) {
        return requestURI.startsWith("/api/auth/login") ||
               requestURI.startsWith("/api/auth/register") ||
               requestURI.startsWith("/api/auth/check-email") ||
               requestURI.startsWith("/api/auth/check-username") ||
               requestURI.startsWith("/api/auth/verify-email") ||
               requestURI.startsWith("/error") ||
               requestURI.equals("/favicon.ico");
    }

    /**
     * 检查是否是受保护的端点（需要认证）
     */
    private boolean isProtectedEndpoint(String requestURI) {
        return requestURI.startsWith("/api/auth/me") ||
               requestURI.startsWith("/api/auth/logout") ||
               requestURI.startsWith("/api/auth/change-") ||
               requestURI.startsWith("/api/library") ||
               requestURI.startsWith("/api/orders") ||
               requestURI.startsWith("/api/settings");
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        // 只对API请求应用此过滤器
        return !path.startsWith("/api/");
    }
}
