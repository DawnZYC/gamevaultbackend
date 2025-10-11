package com.sg.nusiss.gamevaultbackend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsConfig {
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        config.addAllowedOriginPattern("*"); // ✅ 支持前端任意来源访问（开发时）
        config.setAllowCredentials(true);     // ✅ 允许携带 Cookie / Authorization header
        config.addAllowedHeader("*");         // ✅ 允许所有请求头
        config.addAllowedMethod("*");         // ✅ 允许所有 HTTP 方法 (GET/POST/PUT/DELETE/OPTIONS)

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
