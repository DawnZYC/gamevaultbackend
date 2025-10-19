package com.sg.nusiss.gamevaultbackend.entity.file;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @ClassName FileUploadTask
 * @Author HUANG ZHENJIA
 * @Date 2025/10/19
 * @Description
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "file_upload_task", indexes = {
        @Index(name = "idx_file_upload_task_task_id", columnList = "task_id"),
        @Index(name = "idx_file_upload_task_user_id", columnList = "user_id"),
        @Index(name = "idx_file_upload_task_status_expires", columnList = "status, expires_at"),
        @Index(name = "idx_file_upload_task_file_md5", columnList = "file_md5")
})
public class FileUploadTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 上传任务唯一标识
     */
    @Column(name = "task_id", unique = true, nullable = false, length = 64)
    private String taskId;

    /**
     * 文件MD5值
     */
    @Column(name = "file_md5", nullable = false, length = 32)
    private String fileMd5;

    /**
     * 文件名
     */
    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    /**
     * 文件总大小
     */
    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    /**
     * 分片大小
     */
    @Column(name = "chunk_size", nullable = false)
    private Integer chunkSize;

    /**
     * 总分片数
     */
    @Column(name = "total_chunks", nullable = false)
    private Integer totalChunks;

    /**
     * 已上传分片数
     */
    @Column(name = "uploaded_chunks")
    private Integer uploadedChunks = 0;

    /**
     * MinIO uploadId
     */
    @Column(name = "upload_id", length = 200)
    private String uploadId;

    /**
     * 存储桶名称
     */
    @Column(name = "bucket_name", length = 100)
    private String bucketName;

    /**
     * 对象路径
     */
    @Column(name = "object_key", length = 500)
    private String objectKey;

    /**
     * 状态: 1-上传中 2-已完成 3-已取消 4-失败
     */
    @Column(name = "status")
    private Integer status = 1;

    /**
     * 用户ID
     */
    @Column(name = "user_id")
    private Long userId;

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
     * 任务过期时间
     */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

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
