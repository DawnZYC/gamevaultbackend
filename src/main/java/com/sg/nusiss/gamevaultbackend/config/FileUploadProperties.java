package com.sg.nusiss.gamevaultbackend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

/**
 * @ClassName FileUploadProperties
 * @Author HUANG ZHENJIA
 * @Date 2025/10/19
 * @Description
 */

@Data
@Configuration
@ConfigurationProperties(prefix = "file.upload")
public class FileUploadProperties {

    /**
     * 临时文件存储路径
     */
    private String tempPath;

    /**
     * 图片配置
     */
    private ImageConfig image = new ImageConfig();

    /**
     * 视频配置
     */
    private VideoConfig video = new VideoConfig();

    /**
     * 文档配置
     */
    private DocumentConfig document = new DocumentConfig();

    /**
     * 音频配置
     */
    private AudioConfig audio = new AudioConfig();

    /**
     * 分片上传配置
     */
    private ChunkConfig chunk = new ChunkConfig();

    /**
     * 预签名URL配置
     */
    private PresignedConfig presigned = new PresignedConfig();

    /**
     * 秒传配置
     */
    private QuickUploadConfig quickUpload = new QuickUploadConfig();

    /**
     * 并发配置
     */
    private ConcurrentConfig concurrent = new ConcurrentConfig();

    /**
     * 图片配置类
     */
    @Data
    public static class ImageConfig {
        private Long maxSize;
        private String allowedTypes;
        private Boolean generateThumbnail;
        private Integer thumbnailWidth;
        private Integer thumbnailHeight;

        public List<String> getAllowedTypesList() {
            return Arrays.asList(allowedTypes.split(","));
        }
    }

    /**
     * 视频配置类
     */
    @Data
    public static class VideoConfig {
        private Long maxSize;
        private String allowedTypes;
        private Boolean generateCover;

        public List<String> getAllowedTypesList() {
            return Arrays.asList(allowedTypes.split(","));
        }
    }

    /**
     * 文档配置类
     */
    @Data
    public static class DocumentConfig {
        private Long maxSize;
        private String allowedTypes;

        public List<String> getAllowedTypesList() {
            return Arrays.asList(allowedTypes.split(","));
        }
    }

    /**
     * 音频配置类
     */
    @Data
    public static class AudioConfig {
        private Long maxSize;
        private String allowedTypes;

        public List<String> getAllowedTypesList() {
            return Arrays.asList(allowedTypes.split(","));
        }
    }

    /**
     * 分片上传配置类
     */
    @Data
    public static class ChunkConfig {
        private Integer size;
        private Long minFileSize;
        private Integer taskExpireHours;
    }

    /**
     * 预签名URL配置类
     */
    @Data
    public static class PresignedConfig {
        private Integer uploadExpireMinutes;
        private Integer downloadExpireHours;
    }

    /**
     * 秒传配置类
     */
    @Data
    public static class QuickUploadConfig {
        private Boolean enabled;
    }

    /**
     * 并发配置类
     */
    @Data
    public static class ConcurrentConfig {
        private Integer maxUploadsPerUser;
    }
}
