package com.sg.nusiss.gamevaultbackend.config.forum;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import com.sg.nusiss.gamevaultbackend.interceptor.forum.ForumAuthInterceptor;

@Configuration
public class ForumWebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private ForumAuthInterceptor authInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/forum/**")  // 只拦截论坛 API
                .excludePathPatterns(
                        "/api/forum/auth/login",
                        "/api/forum/auth/register",// 认证相关不拦截
                        "/api/test/**"       // 测试接口不拦截
                );
    }
}

