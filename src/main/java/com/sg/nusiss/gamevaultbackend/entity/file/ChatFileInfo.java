package com.sg.nusiss.gamevaultbackend.entity.file;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @ClassName ChatFileInfo
 * @Author HUANG ZHENJIA
 * @Date 2025/10/19
 * @Description
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "chat_file_info", indexes = {
        @Index(name = "idx_chat_file_info_file_id", columnList = "file_id"),
        @Index(name = "idx_chat_file_info_file_md5", columnList = "file_md5"),
        @Index(name = "idx_chat_file_info_user_id", columnList = "user_id"),
        @Index(name = "idx_chat_file_info_biz_type_id", columnList = "biz_type, biz_id"),
        @Index(name = "idx_chat_file_info_created_at", columnList = "created_at"),
        @Index(name = "idx_chat_file_info_status", columnList = "status")
})
public class ChatFileInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 文件唯一标识UUID
     */
    @Column(name = "file_id", unique = true, nullable = false, length = 64)
    private String fileId;

    /**
     * 原始文件名
     */
    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    /**
     * 文件大小（字节）
     */
    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    /**
     * 文件类型（image/video/document/audio）
     */
    @Column(name = "file_type", length = 50)
    private String fileType;

    /**
     * MIME类型
     */
    @Column(name = "mime_type", length = 100)
    private String mimeType;

    /**
     * 文件扩展名
     */
    @Column(name = "file_ext", length = 20)
    private String fileExt;

    /**
     * 存储桶名称
     */
    @Column(name = "bucket_name", nullable = false, length = 100)
    private String bucketName;

    /**
     * 对象存储路径
     */
    @Column(name = "object_key", nullable = false, length = 500)
    private String objectKey;

    /**
     * 完整存储路径
     */
    @Column(name = "storage_path", length = 1000)
    private String storagePath;

    /**
     * MD5哈希值（用于秒传）
     */
    @Column(name = "file_md5", length = 32)
    private String fileMd5;

    /**
     * SHA256哈希值
     */
    @Column(name = "file_sha256", length = 64)
    private String fileSha256;

    /**
     * 状态: 1-正常 2-删除 3-审核中
     */
    @Column(name = "status")
    private Integer status = 1;

    /**
     * 访问URL
     */
    @Column(name = "access_url", length = 1000)
    private String accessUrl;

    /**
     * 预签名URL
     */
    @Column(name = "presigned_url", length = 2000)
    private String presignedUrl;

    /**
     * URL过期时间
     */
    @Column(name = "url_expires_at")
    private LocalDateTime urlExpiresAt;

    /**
     * 下载次数
     */
    @Column(name = "download_count")
    private Integer downloadCount = 0;

    /**
     * 上传用户ID
     */
    @Column(name = "user_id")
    private Long userId;

    /**
     * 业务类型（message/conversation/forum/profile/friend）
     */
    @Column(name = "biz_type", length = 50)
    private String bizType;

    /**
     * 业务关联ID
     */
    @Column(name = "biz_id", length = 100)
    private String bizId;

    /**
     * 图片/视频宽度
     */
    @Column(name = "width")
    private Integer width;

    /**
     * 图片/视频高度
     */
    @Column(name = "height")
    private Integer height;

    /**
     * 视频/音频时长（秒）
     */
    @Column(name = "duration")
    private Integer duration;

    /**
     * 缩略图URL
     */
    @Column(name = "thumbnail_url", length = 1000)
    private String thumbnailUrl;

    /**
     * 创建时间
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * 创建人ID
     */
    @Column(name = "created_by")
    private Long createdBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
