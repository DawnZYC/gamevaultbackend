package com.sg.nusiss.gamevaultbackend.dto.file.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * @ClassName FileListRequest
 * @Author HUANG ZHENJIA
 * @Date 2025/10/19
 * @Description
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileListRequest {

    /**
     * 用户ID（可选，管理员可以查询其他用户的文件）
     */
    private Long userId;

    /**
     * 文件类型（image/video/document/audio）
     */
    private String fileType;

    /**
     * 业务类型（message/conversation/forum/profile）
     */
    private String bizType;

    /**
     * 业务关联ID
     */
    private String bizId;

    /**
     * 状态（1-正常 2-删除 3-审核中）
     */
    private Integer status = 1;

    /**
     * 页码（从0开始）
     */
    private Integer page = 0;

    /**
     * 每页大小
     */
    private Integer size = 20;

    /**
     * 排序字段（createdAt/fileSize/downloadCount）
     */
    private String sortBy = "createdAt";

    /**
     * 排序方向（asc/desc）
     */
    private String sortDir = "desc";
}
