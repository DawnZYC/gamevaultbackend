package com.sg.nusiss.gamevaultbackend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.upload.path:uploads}")
    private String uploadPath;

    @Value("${app.asset-storage-path:game-assets}")
    private String assetStoragePath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 配置静态资源访问路径 - uploads (avatars, etc.)
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadPath + "/");
        
        // 配置静态资源访问路径 - game assets (dev game images, videos, zips)
        String assetPath;
        if (assetStoragePath.startsWith("/") || assetStoragePath.contains(":")) {
            // 已经是绝对路径
            assetPath = assetStoragePath.endsWith("/") ? assetStoragePath : assetStoragePath + "/";
        } else {
            // 相对路径，转换为绝对路径
            String projectRoot = System.getProperty("user.dir");
            assetPath = projectRoot + "/" + assetStoragePath;
            assetPath = assetPath.endsWith("/") ? assetPath : assetPath + "/";
        }
        
        registry.addResourceHandler("/assets/**")
                .addResourceLocations("file:" + assetPath);
    }
}
