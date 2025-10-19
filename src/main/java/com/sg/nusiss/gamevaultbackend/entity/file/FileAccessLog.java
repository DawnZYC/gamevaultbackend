package com.sg.nusiss.gamevaultbackend.entity.file;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @ClassName FileAccessLog
 * @Author HUANG ZHENJIA
 * @Date 2025/10/19
 * @Description
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "file_access_log", indexes = {
        @Index(name = "idx_file_access_log_file_id", columnList = "file_id"),
        @Index(name = "idx_file_access_log_user_id", columnList = "user_id"),
        @Index(name = "idx_file_access_log_created_at", columnList = "created_at")
})
public class FileAccessLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 文件ID
     */
    @Column(name = "file_id", nullable = false, length = 64)
    private String fileId;

    /**
     * 访问用户ID
     */
    @Column(name = "user_id")
    private Long userId;

    /**
     * 访问类型: 1-查看 2-下载 3-分享 4-删除
     */
    @Column(name = "access_type")
    private Integer accessType;

    /**
     * IP地址
     */
    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    /**
     * 用户代理
     */
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    /**
     * 访问时间
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
