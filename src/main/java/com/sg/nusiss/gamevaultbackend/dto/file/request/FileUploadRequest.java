package com.sg.nusiss.gamevaultbackend.dto.file.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * @ClassName FileUploadRequest
 * @Author HUANG ZHENJIA
 * @Date 2025/10/19
 * @Description
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadRequest {

    /**
     * 文件名
     */
    @NotBlank(message = "文件名不能为空")
    private String fileName;

    /**
     * 文件大小（字节）
     */
    @NotNull(message = "文件大小不能为空")
    private Long fileSize;

    /**
     * 文件MD5（用于秒传）
     */
    private String fileMd5;

    /**
     * 文件SHA256
     */
    private String fileSha256;

    /**
     * 文件类型（image/video/document/audio）
     */
    private String fileType;

    /**
     * MIME类型
     */
    private String mimeType;

    /**
     * 业务类型（message/conversation/forum/profile）
     */
    private String bizType;

    /**
     * 业务关联ID
     */
    private String bizId;
}
