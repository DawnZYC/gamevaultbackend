package com.sg.nusiss.gamevaultbackend.dto.file.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * @ClassName CompleteChunkUploadRequest
 * @Author HUANG ZHENJIA
 * @Date 2025/10/19
 * @Description
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompleteChunkUploadRequest {

    /**
     * 上传任务ID
     */
    @NotBlank(message = "任务ID不能为空")
    private String taskId;

    /**
     * 已上传的分片信息列表
     */
    @NotEmpty(message = "分片信息不能为空")
    private List<ChunkInfo> chunks;

    /**
     * 分片信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChunkInfo {
        /**
         * 分片序号
         */
        @NotNull(message = "分片序号不能为空")
        private Integer chunkNumber;

        /**
         * MinIO返回的ETag
         */
        @NotBlank(message = "ETag不能为空")
        private String etag;

        /**
         * 分片MD5（可选，用于校验）
         */
        private String chunkMd5;
    }
}
