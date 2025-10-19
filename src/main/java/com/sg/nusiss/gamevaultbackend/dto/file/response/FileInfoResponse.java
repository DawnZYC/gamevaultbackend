package com.sg.nusiss.gamevaultbackend.dto.file.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @ClassName FileInfoResponse
 * @Author HUANG ZHENJIA
 * @Date 2025/10/19
 * @Description
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileInfoResponse {

    /**
     * 文件ID
     */
    private String fileId;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 文件大小（格式化，如：1.5MB）
     */
    private String fileSizeFormatted;

    /**
     * 文件类型
     */
    private String fileType;

    /**
     * MIME类型
     */
    private String mimeType;

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
     * 图片/视频宽度
     */
    private Integer width;

    /**
     * 图片/视频高度
     */
    private Integer height;

    /**
     * 视频/音频时长（秒）
     */
    private Integer duration;

    /**
     * 下载次数
     */
    private Integer downloadCount;

    /**
     * 状态（1-正常 2-删除 3-审核中）
     */
    private Integer status;

    /**
     * 上传用户ID
     */
    private Long userId;

    /**
     * 业务类型
     */
    private String bizType;

    /**
     * 业务关联ID
     */
    private String bizId;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
