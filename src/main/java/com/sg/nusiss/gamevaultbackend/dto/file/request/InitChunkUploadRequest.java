package com.sg.nusiss.gamevaultbackend.dto.file.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;

/**
 * @ClassName InitChunkUploadRequest
 * @Author HUANG ZHENJIA
 * @Date 2025/10/19
 * @Description
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InitChunkUploadRequest {

    /**
     * 文件名
     */
    @NotBlank(message = "文件名不能为空")
    private String fileName;

    /**
     * 文件大小（字节）
     */
    @NotNull(message = "文件大小不能为空")
    @Min(value = 1, message = "文件大小必须大于0")
    private Long fileSize;

    /**
     * 文件MD5
     */
    @NotBlank(message = "文件MD5不能为空")
    private String fileMd5;

    /**
     * 分片大小（字节）
     */
    @NotNull(message = "分片大小不能为空")
    @Min(value = 1048576, message = "分片大小不能小于1MB")
    private Integer chunkSize;

    /**
     * 总分片数
     */
    @NotNull(message = "总分片数不能为空")
    @Min(value = 1, message = "总分片数必须大于0")
    private Integer totalChunks;

    /**
     * 文件类型
     */
    private String fileType;

    /**
     * MIME类型
     */
    private String mimeType;

    /**
     * 业务类型
     */
    private String bizType;

    /**
     * 业务关联ID
     */
    private String bizId;
}