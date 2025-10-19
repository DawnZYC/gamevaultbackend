package com.sg.nusiss.gamevaultbackend.dto.file.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @ClassName FileDownloadRequest
 * @Author HUANG ZHENJIA
 * @Date 2025/10/19
 * @Description
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileDownloadRequest {

    /**
     * 文件ID
     */
    @NotBlank(message = "文件ID不能为空")
    private String fileId;

    /**
     * 是否记录下载日志
     */
    private Boolean recordLog = true;

    /**
     * URL有效期（分钟，默认60分钟）
     */
    private Integer expiresInMinutes = 60;
}
