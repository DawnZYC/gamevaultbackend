package com.sg.nusiss.gamevaultbackend.dto.file.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @ClassName InitChunkUploadResponse
 * @Author HUANG ZHENJIA
 * @Date 2025/10/19
 * @Description
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InitChunkUploadResponse {

    /**
     * 上传任务ID
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
     * 分片大小
     */
    private Integer chunkSize;

    /**
     * 总分片数
     */
    private Integer totalChunks;

    /**
     * MinIO uploadId
     */
    private String uploadId;

    /**
     * 分片上传URL列表
     */
    private List<ChunkUploadUrl> chunkUploadUrls;

    /**
     * 任务过期时间（时间戳）
     */
    private Long expiresAt;

    /**
     * 提示信息
     */
    private String message;

    /**
     * 分片上传URL信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChunkUploadUrl {
        /**
         * 分片序号（从1开始）
         */
        private Integer chunkNumber;

        /**
         * 预签名上传URL
         */
        private String uploadUrl;

        /**
         * URL过期时间（时间戳）
         */
        private Long urlExpiresAt;
    }
}
