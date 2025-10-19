package com.sg.nusiss.gamevaultbackend.dto.file.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @ClassName CompleteChunkUploadResponse
 * @Author HUANG ZHENJIA
 * @Date 2025/10/19
 * @Description
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompleteChunkUploadResponse {

    /**
     * 文件ID
     */
    private String fileId;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 文件大小
     */
    private Long fileSize;

    /**
     * 文件类型
     */
    private String fileType;

    /**
     * 文件扩展名
     */
    private String fileExt;

    /**
     * 访问URL
     */
    private String accessUrl;

    /**
     * 预签名下载URL
     */
    private String downloadUrl;

    /**
     * URL过期时间（时间戳）
     */
    private Long urlExpiresAt;

    /**
     * 缩略图URL
     */
    private String thumbnailUrl;

    /**
     * 上传状态（success/failed）
     */
    private String status;

    /**
     * 提示信息
     */
    private String message;
}
