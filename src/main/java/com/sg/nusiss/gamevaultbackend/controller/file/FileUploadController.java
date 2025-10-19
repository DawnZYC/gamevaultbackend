package com.sg.nusiss.gamevaultbackend.controller.file;

import com.sg.nusiss.gamevaultbackend.common.BaseResponse;
import com.sg.nusiss.gamevaultbackend.common.ResultUtils;
import com.sg.nusiss.gamevaultbackend.dto.file.request.CompleteChunkUploadRequest;
import com.sg.nusiss.gamevaultbackend.dto.file.request.FileUploadRequest;
import com.sg.nusiss.gamevaultbackend.dto.file.request.InitChunkUploadRequest;
import com.sg.nusiss.gamevaultbackend.dto.file.response.CompleteChunkUploadResponse;
import com.sg.nusiss.gamevaultbackend.dto.file.response.FileUploadResponse;
import com.sg.nusiss.gamevaultbackend.dto.file.response.InitChunkUploadResponse;
import com.sg.nusiss.gamevaultbackend.dto.file.response.UploadTaskResponse;
import com.sg.nusiss.gamevaultbackend.security.auth.SecurityUtils;
import com.sg.nusiss.gamevaultbackend.service.file.ChunkUploadService;
import com.sg.nusiss.gamevaultbackend.service.file.FileStorageService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * @ClassName FileUploadController
 * @Author HUANG ZHENJIA
 * @Date 2025/10/19
 * @Description
 */
@Slf4j
@RestController
@RequestMapping("/api/file/upload")
public class FileUploadController {

    private final FileStorageService fileUploadService;
    private final ChunkUploadService chunkUploadService;

    public FileUploadController(FileStorageService fileUploadService,
                                ChunkUploadService chunkUploadService) {
        this.fileUploadService = fileUploadService;
        this.chunkUploadService = chunkUploadService;
    }

    /**
     * 小文件直接上传（后端中转）
     */
    @PostMapping("/simple")
    public BaseResponse<FileUploadResponse> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "fileName", required = false) String fileName,
            @RequestParam(value = "fileMd5", required = false) String fileMd5,
            @RequestParam(value = "bizType", required = false) String bizType,
            @RequestParam(value = "bizId", required = false) String bizId) {

        // 从 JWT 获取当前用户ID
        Long userId = SecurityUtils.getCurrentUserId();

        // 构建请求对象
        FileUploadRequest uploadRequest = FileUploadRequest.builder()
                .fileName(fileName != null ? fileName : file.getOriginalFilename())
                .fileSize(file.getSize())
                .fileMd5(fileMd5)
                .mimeType(file.getContentType())
                .bizType(bizType)
                .bizId(bizId)
                .build();

        FileUploadResponse response = fileUploadService.uploadFile(uploadRequest, file, userId);
        return ResultUtils.success(response);
    }

    /**
     * 生成预签名上传URL（前端直传）
     */
    @PostMapping("/presigned-url")
    public BaseResponse<FileUploadResponse> generateUploadUrl(
            @Valid @RequestBody FileUploadRequest request) {

        // 从 JWT 获取当前用户ID
        Long userId = SecurityUtils.getCurrentUserId();

        FileUploadResponse response = fileUploadService.generateUploadUrl(request, userId);
        return ResultUtils.success(response);
    }

    /**
     * 初始化分片上传
     */
    @PostMapping("/chunk/init")
    public BaseResponse<InitChunkUploadResponse> initChunkUpload(
            @Valid @RequestBody InitChunkUploadRequest request) {

        // 从 JWT 获取当前用户ID
        Long userId = SecurityUtils.getCurrentUserId();

        InitChunkUploadResponse response = chunkUploadService.initChunkUpload(request, userId);
        return ResultUtils.success(response);
    }

    /**
     * 完成分片上传
     */
    @PostMapping("/chunk/complete")
    public BaseResponse<CompleteChunkUploadResponse> completeChunkUpload(
            @Valid @RequestBody CompleteChunkUploadRequest request) {

        // 从 JWT 获取当前用户ID
        Long userId = SecurityUtils.getCurrentUserId();

        CompleteChunkUploadResponse response = chunkUploadService.completeChunkUpload(request, userId);
        return ResultUtils.success(response);
    }

    /**
     * 查询上传任务状态
     */
    @GetMapping("/chunk/status/{taskId}")
    public BaseResponse<UploadTaskResponse> getTaskStatus(@PathVariable String taskId) {

        // 从 JWT 获取当前用户ID
        Long userId = SecurityUtils.getCurrentUserId();

        UploadTaskResponse response = chunkUploadService.getTaskStatus(taskId, userId);
        return ResultUtils.success(response);
    }

    /**
     * 取消上传任务
     */
    @DeleteMapping("/chunk/cancel/{taskId}")
    public BaseResponse<Void> cancelUploadTask(@PathVariable String taskId) {

        // 从 JWT 获取当前用户ID
        Long userId = SecurityUtils.getCurrentUserId();

        chunkUploadService.cancelUploadTask(taskId, userId);
        return ResultUtils.success(null);
    }
}
