package com.sg.nusiss.gamevaultbackend.config;

import io.minio.MinioClient;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @ClassName MinioConfig
 * @Author HUANG ZHENJIA
 * @Date 2025/10/19
 * @Description
 */

@Data
@Configuration
@ConfigurationProperties(prefix = "minio")
public class MinioConfig {

    /**
     * MinIO 服务端点
     */
    private String endpoint;

    /**
     * 访问密钥
     */
    private String accessKey;

    /**
     * 秘密密钥
     */
    private String secretKey;

    /**
     * 默认存储桶
     */
    private String bucketName;

    /**
     * 图片存储桶
     */
    private String imageBucket;

    /**
     * 视频存储桶
     */
    private String videoBucket;

    /**
     * 文件存储桶
     */
    private String fileBucket;

    /**
     * 音频存储桶
     */
    private String audioBucket;

    /**
     * 创建 MinioClient Bean
     */
    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }
}
