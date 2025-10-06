package com.sg.nusiss.gamevaultbackend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;

@Configuration
public class FileUploadConfig {

    @Value("${app.upload.path:uploads}")
    private String uploadPath;

    @Value("${app.upload.max-file-size:5MB}")
    private String maxFileSize;

    @Value("${app.upload.max-request-size:10MB}")
    private String maxRequestSize;

    @Bean
    public MultipartResolver multipartResolver() {
        StandardServletMultipartResolver resolver = new StandardServletMultipartResolver();
        return resolver;
    }

    public String getUploadPath() {
        return uploadPath;
    }

    public String getMaxFileSize() {
        return maxFileSize;
    }

    public String getMaxRequestSize() {
        return maxRequestSize;
    }
}
