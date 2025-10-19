package com.sg.nusiss.gamevaultbackend.dto.file.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @ClassName UploadTaskResponse
 * @Author HUANG ZHENJIA
 * @Date 2025/10/19
 * @Description
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadTaskResponse {

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 文件大小
     */
    private Long fileSize;

    /**
     * 文件MD5
     */
    private String fileMd5;

    /**
     * 分片大小
     */
    private Integer chunkSize;

    /**
     * 总分片数
     */
    private Integer totalChunks;

    /**
     * 已上传分片数
     */
    private Integer uploadedChunks;

    /**
     * 上传进度（百分比）
     */
    private Double progress;

    /**
     * 状态（1-上传中 2-已完成 3-已取消 4-失败）
     */
    private Integer status;

    /**
     * 状态描述
     */
    private String statusDesc;

    /**
     * MinIO uploadId
     */
    private String uploadId;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 任务过期时间
     */
    private LocalDateTime expiresAt;
}
