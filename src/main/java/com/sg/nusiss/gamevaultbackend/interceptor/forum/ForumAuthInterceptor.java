package com.sg.nusiss.gamevaultbackend.interceptor.forum;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import com.sg.nusiss.gamevaultbackend.annotation.forum.RequireForumAuth;
import com.sg.nusiss.gamevaultbackend.util.forum.ForumJwtUtil;

@Component
public class ForumAuthInterceptor implements HandlerInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(ForumAuthInterceptor.class);

    @Autowired
    private ForumJwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {

        // 如果不是方法处理器，直接放行
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;

        // 检查是否需要认证
        RequireForumAuth requireAuth = handlerMethod.getMethodAnnotation(RequireForumAuth.class);
        if (requireAuth == null) {
            requireAuth = handlerMethod.getBeanType().getAnnotation(RequireForumAuth.class);
        }

        // 获取 Token
        String authHeader = request.getHeader("Authorization");
        String token = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7); // 移除"Bearer "前缀
        } else if (authHeader != null) {
            token = authHeader;
        }
        if (token == null || token.isEmpty()) {
            token = request.getParameter("token"); // 支持 URL 参数传递
        }

        // 验证 Token
        if (token != null) {
            try {
                ForumJwtUtil.TokenInfo tokenInfo = jwtUtil.validateAndParseToken(token);
                if (tokenInfo.valid) {
                    // 将用户信息设置到 request 属性中
                    request.setAttribute("userId", tokenInfo.userId);
                    request.setAttribute("username", tokenInfo.username);
                    logger.debug("论坛认证成功 - 用户ID: {}, 用户名: {}", tokenInfo.userId, tokenInfo.username);
                    return true;
                } else {
                    logger.warn("论坛认证失败 - Token无效 - 请求路径: {} - Token: {}", request.getRequestURI(), token.substring(0, Math.min(50, token.length())) + "...");
                }
            } catch (Exception e) {
                logger.error("论坛认证异常 - 请求路径: {} - Token: {} - 错误: {}", request.getRequestURI(), token.substring(0, Math.min(50, token.length())) + "...", e.getMessage());
            }
        }

        // 如果需要认证但 Token 无效
        if (requireAuth != null && requireAuth.required()) {
            logger.warn("论坛认证失败 - 请求路径: {}", request.getRequestURI());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\":\"未授权\",\"message\":\"请先登录\"}");
            return false;
        }

        // 不需要认证，直接放行
        return true;
    }
}

