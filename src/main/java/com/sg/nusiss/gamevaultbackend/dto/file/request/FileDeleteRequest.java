package com.sg.nusiss.gamevaultbackend.dto.file.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * @ClassName FileDeleteRequest
 * @Author HUANG ZHENJIA
 * @Date 2025/10/19
 * @Description
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileDeleteRequest {

    /**
     * 文件ID
     */
    @NotBlank(message = "文件ID不能为空")
    private String fileId;

    /**
     * 是否物理删除（true-物理删除 false-逻辑删除，默认false）
     */
    private Boolean physicalDelete = false;
}