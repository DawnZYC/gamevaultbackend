package com.sg.nusiss.gamevaultbackend.entity.file;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @ClassName FileChunkInfo
 * @Author HUANG ZHENJIA
 * @Date 2025/10/19
 * @Description
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "file_chunk_info",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_file_chunk_info_task_chunk", columnNames = {"task_id", "chunk_number"})
        },
        indexes = {
                @Index(name = "idx_file_chunk_info_task_id", columnList = "task_id"),
                @Index(name = "idx_file_chunk_info_status", columnList = "status")
        })
public class FileChunkInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 关联的上传任务ID
     */
    @Column(name = "task_id", nullable = false, length = 64)
    private String taskId;

    /**
     * 分片序号（从1开始）
     */
    @Column(name = "chunk_number", nullable = false)
    private Integer chunkNumber;

    /**
     * 分片MD5值
     */
    @Column(name = "chunk_md5", length = 32)
    private String chunkMd5;

    /**
     * 分片大小
     */
    @Column(name = "chunk_size")
    private Integer chunkSize;

    /**
     * MinIO返回的ETag
     */
    @Column(name = "etag", length = 200)
    private String etag;

    /**
     * 状态: 1-待上传 2-上传中 3-已完成 4-失败
     */
    @Column(name = "status")
    private Integer status = 1;

    /**
     * 预签名上传URL
     */
    @Column(name = "upload_url", length = 2000)
    private String uploadUrl;

    /**
     * URL过期时间
     */
    @Column(name = "url_expires_at")
    private LocalDateTime urlExpiresAt;

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
