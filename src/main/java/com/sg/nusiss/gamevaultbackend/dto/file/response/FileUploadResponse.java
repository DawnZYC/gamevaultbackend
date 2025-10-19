package com.sg.nusiss.gamevaultbackend.dto.file.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @ClassName FileUploadResponse
 * @Author HUANG ZHENJIA
 * @Date 2025/10/19
 * @Description
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadResponse {

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
     * 预签名上传URL（用于直传）
     */
    private String uploadUrl;

    /**
     * URL过期时间（时间戳）
     */
    private Long urlExpiresAt;

    /**
     * 缩略图URL（图片/视频）
     */
    private String thumbnailUrl;

    /**
     * 是否秒传
     */
    private Boolean quickUpload;

    /**
     * 是否需要分片上传
     */
    private Boolean needChunkUpload;

    /**
     * 上传任务ID（分片上传时使用）
     */
    private String taskId;

    /**
     * 上传提示信息
     */
    private String message;
}
