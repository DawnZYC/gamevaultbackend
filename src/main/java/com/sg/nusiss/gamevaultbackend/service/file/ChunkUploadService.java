package com.sg.nusiss.gamevaultbackend.service.file;

import com.sg.nusiss.gamevaultbackend.common.ErrorCode;
import com.sg.nusiss.gamevaultbackend.config.FileUploadProperties;
import com.sg.nusiss.gamevaultbackend.dto.file.request.CompleteChunkUploadRequest;
import com.sg.nusiss.gamevaultbackend.dto.file.request.InitChunkUploadRequest;
import com.sg.nusiss.gamevaultbackend.dto.file.response.CompleteChunkUploadResponse;
import com.sg.nusiss.gamevaultbackend.dto.file.response.InitChunkUploadResponse;
import com.sg.nusiss.gamevaultbackend.dto.file.response.UploadTaskResponse;
import com.sg.nusiss.gamevaultbackend.entity.file.ChatFileInfo;
import com.sg.nusiss.gamevaultbackend.entity.file.FileChunkInfo;
import com.sg.nusiss.gamevaultbackend.entity.file.FileUploadTask;
import com.sg.nusiss.gamevaultbackend.exception.BusinessException;
import com.sg.nusiss.gamevaultbackend.repository.file.ChatFileInfoRepository;
import com.sg.nusiss.gamevaultbackend.repository.file.FileChunkInfoRepository;
import com.sg.nusiss.gamevaultbackend.repository.file.FileUploadTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @ClassName ChunkUploadService
 * @Author HUANG ZHENJIA
 * @Date 2025/10/19
 * @Description
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChunkUploadService {

    private final MinioService minioService;
    private final FileUploadTaskRepository uploadTaskRepository;
    private final FileChunkInfoRepository chunkInfoRepository;
    private final ChatFileInfoRepository fileInfoRepository;
    private final FileUploadProperties uploadProperties;

    /**
     * 初始化分片上传
     */
    @Transactional
    public InitChunkUploadResponse initChunkUpload(InitChunkUploadRequest request, Long userId) {
        log.info("Initializing chunk upload: {}, totalChunks: {}, user: {}",
                request.getFileName(), request.getTotalChunks(), userId);

        // 1. 检查用户并发上传限制
        Long activeUploads = uploadTaskRepository.countByUserIdAndStatus(userId, 1);
        if (activeUploads >= uploadProperties.getConcurrent().getMaxUploadsPerUser()) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR,
                    "同时上传的文件数量已达上限，请等待其他上传完成");
        }

        // 2. 检查是否已有相同文件的上传任务
        Optional<FileUploadTask> existingTask = uploadTaskRepository.findByFileMd5AndStatus(
                request.getFileMd5(), 1);

        if (existingTask.isPresent()) {
            // 返回已存在的任务
            return buildInitResponse(existingTask.get());
        }

        // 3. 生成任务信息
        String taskId = UUID.randomUUID().toString().replace("-", "");
        String fileExt = getFileExtension(request.getFileName());
        String fileType = determineFileType(fileExt, request.getMimeType());
        String bucketName = minioService.getBucketNameByFileType(fileType);
        String objectKey = generateObjectKey(fileType, taskId, fileExt);

        // 4. 创建上传任务
        LocalDateTime expiresAt = LocalDateTime.now()
                .plusHours(uploadProperties.getChunk().getTaskExpireHours());

        FileUploadTask task = FileUploadTask.builder()
                .taskId(taskId)
                .fileMd5(request.getFileMd5())
                .fileName(request.getFileName())
                .fileSize(request.getFileSize())
                .chunkSize(request.getChunkSize())
                .totalChunks(request.getTotalChunks())
                .uploadedChunks(0)
                .bucketName(bucketName)
                .objectKey(objectKey)
                .status(1) // 1-上传中
                .userId(userId)
                .expiresAt(expiresAt)
                .build();

        uploadTaskRepository.save(task);

        // 5. 生成所有分片的上传URL
        List<InitChunkUploadResponse.ChunkUploadUrl> chunkUrls = new ArrayList<>();
        int urlExpiresInMinutes = uploadProperties.getPresigned().getUploadExpireMinutes();
        LocalDateTime urlExpiresAt = LocalDateTime.now().plusMinutes(urlExpiresInMinutes);

        for (int i = 1; i <= request.getTotalChunks(); i++) {
            // 生成分片上传URL
            String uploadUrl = minioService.generatePresignedUploadPartUrl(
                    bucketName, objectKey, i, urlExpiresInMinutes);

            // 保存分片信息
            FileChunkInfo chunkInfo = FileChunkInfo.builder()
                    .taskId(taskId)
                    .chunkNumber(i)
                    .status(1) // 1-待上传
                    .uploadUrl(uploadUrl)
                    .urlExpiresAt(urlExpiresAt)
                    .build();

            chunkInfoRepository.save(chunkInfo);

            // 添加到响应列表
            chunkUrls.add(InitChunkUploadResponse.ChunkUploadUrl.builder()
                    .chunkNumber(i)
                    .uploadUrl(uploadUrl)
                    .urlExpiresAt(urlExpiresAt.toEpochSecond(ZoneOffset.UTC))
                    .build());
        }

        log.info("Chunk upload initialized: taskId={}, totalChunks={}", taskId, request.getTotalChunks());

        return InitChunkUploadResponse.builder()
                .taskId(taskId)
                .fileName(request.getFileName())
                .fileSize(request.getFileSize())
                .chunkSize(request.getChunkSize())
                .totalChunks(request.getTotalChunks())
                .chunkUploadUrls(chunkUrls)
                .expiresAt(expiresAt.toEpochSecond(ZoneOffset.UTC))
                .message("分片上传任务创建成功，请使用提供的URL上传各个分片")
                .build();
    }

    /**
     * 完成分片上传
     */
    @Transactional
    public CompleteChunkUploadResponse completeChunkUpload(CompleteChunkUploadRequest request, Long userId) {
        log.info("Completing chunk upload: taskId={}, user={}", request.getTaskId(), userId);

        // 1. 查询上传任务
        FileUploadTask task = uploadTaskRepository.findByTaskId(request.getTaskId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ERROR, "上传任务不存在"));

        // 检查任务所属
        if (!task.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权操作此上传任务");
        }

        // 检查任务状态
        if (task.getStatus() != 1) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "任务状态异常，无法完成上传");
        }

        // 2. 更新分片信息
        for (CompleteChunkUploadRequest.ChunkInfo chunk : request.getChunks()) {
            chunkInfoRepository.updateChunkStatus(
                    request.getTaskId(),
                    chunk.getChunkNumber(),
                    3, // 3-已完成
                    chunk.getEtag(),
                    LocalDateTime.now()
            );
        }

        // 3. 检查所有分片是否都已上传
        Long completedCount = chunkInfoRepository.countByTaskIdAndStatus(request.getTaskId(), 3);
        if (completedCount < task.getTotalChunks()) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR,
                    String.format("分片上传不完整，已完成: %d/%d", completedCount, task.getTotalChunks()));
        }

        // 4. 合并分片
        String finalObjectKey;
        try {
            finalObjectKey = minioService.mergeChunks(
                    task.getBucketName(),
                    task.getObjectKey(),
                    task.getTotalChunks()
            );
        } catch (Exception e) {
            log.error("Failed to merge chunks for task: {}", request.getTaskId(), e);
            // 更新任务状态为失败
            uploadTaskRepository.updateStatusByTaskId(
                    request.getTaskId(), 4, LocalDateTime.now());
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "分片合并失败");
        }

        // 5. 更新任务状态为已完成
        task.setStatus(2);
        task.setUploadedChunks(task.getTotalChunks());
        uploadTaskRepository.save(task);

        // 6. 生成文件信息
        String fileId = UUID.randomUUID().toString().replace("-", "");
        String fileExt = getFileExtension(task.getFileName());
        String fileType = determineFileType(fileExt, null);

        ChatFileInfo fileInfo = ChatFileInfo.builder()
                .fileId(fileId)
                .fileName(task.getFileName())
                .fileSize(task.getFileSize())
                .fileType(fileType)
                .fileExt(fileExt)
                .bucketName(task.getBucketName())
                .objectKey(finalObjectKey)
                .storagePath(task.getBucketName() + "/" + finalObjectKey)
                .fileMd5(task.getFileMd5())
                .status(1)
                .userId(userId)
                .createdBy(userId)
                .build();

        fileInfoRepository.save(fileInfo);

        // 7. 生成访问URL
        int downloadExpiresInHours = uploadProperties.getPresigned().getDownloadExpireHours();
        String downloadUrl = minioService.generatePresignedDownloadUrl(
                task.getBucketName(), finalObjectKey, downloadExpiresInHours * 60);

        LocalDateTime urlExpiresAt = LocalDateTime.now().plusHours(downloadExpiresInHours);

        log.info("Chunk upload completed: taskId={}, fileId={}", request.getTaskId(), fileId);

        return CompleteChunkUploadResponse.builder()
                .fileId(fileId)
                .fileName(task.getFileName())
                .fileSize(task.getFileSize())
                .fileType(fileType)
                .fileExt(fileExt)
                .downloadUrl(downloadUrl)
                .urlExpiresAt(urlExpiresAt.toEpochSecond(ZoneOffset.UTC))
                .status("success")
                .message("文件上传完成")
                .build();
    }

    /**
     * 查询上传任务状态
     */
    public UploadTaskResponse getTaskStatus(String taskId, Long userId) {
        FileUploadTask task = uploadTaskRepository.findByTaskId(taskId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ERROR, "上传任务不存在"));

        // 检查任务所属
        if (!task.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权查看此上传任务");
        }

        // 计算上传进度
        double progress = task.getTotalChunks() > 0
                ? (task.getUploadedChunks() * 100.0 / task.getTotalChunks())
                : 0;

        String statusDesc = getStatusDescription(task.getStatus());

        return UploadTaskResponse.builder()
                .taskId(task.getTaskId())
                .fileName(task.getFileName())
                .fileSize(task.getFileSize())
                .fileMd5(task.getFileMd5())
                .chunkSize(task.getChunkSize())
                .totalChunks(task.getTotalChunks())
                .uploadedChunks(task.getUploadedChunks())
                .progress(progress)
                .status(task.getStatus())
                .statusDesc(statusDesc)
                .uploadId(task.getUploadId())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .expiresAt(task.getExpiresAt())
                .build();
    }

    /**
     * 取消上传任务
     */
    @Transactional
    public void cancelUploadTask(String taskId, Long userId) {
        FileUploadTask task = uploadTaskRepository.findByTaskId(taskId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ERROR, "上传任务不存在"));

        // 检查任务所属
        if (!task.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权操作此上传任务");
        }

        // 更新任务状态
        uploadTaskRepository.updateStatusByTaskId(taskId, 3, LocalDateTime.now());

        // 删除已上传的分片
        try {
            for (int i = 1; i <= task.getTotalChunks(); i++) {
                String partObjectName = task.getObjectKey() + ".part" + i;
                if (minioService.fileExists(task.getBucketName(), partObjectName)) {
                    minioService.deleteFile(task.getBucketName(), partObjectName);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to clean up chunks for cancelled task: {}", taskId, e);
        }

        log.info("Upload task cancelled: taskId={}", taskId);
    }

    /**
     * 构建初始化响应（已存在的任务）
     */
    private InitChunkUploadResponse buildInitResponse(FileUploadTask task) {
        List<FileChunkInfo> chunks = chunkInfoRepository.findByTaskIdOrderByChunkNumber(task.getTaskId());

        List<InitChunkUploadResponse.ChunkUploadUrl> chunkUrls = chunks.stream()
                .map(chunk -> InitChunkUploadResponse.ChunkUploadUrl.builder()
                        .chunkNumber(chunk.getChunkNumber())
                        .uploadUrl(chunk.getUploadUrl())
                        .urlExpiresAt(chunk.getUrlExpiresAt().toEpochSecond(ZoneOffset.UTC))
                        .build())
                .collect(Collectors.toList());

        return InitChunkUploadResponse.builder()
                .taskId(task.getTaskId())
                .fileName(task.getFileName())
                .fileSize(task.getFileSize())
                .chunkSize(task.getChunkSize())
                .totalChunks(task.getTotalChunks())
                .uploadId(task.getUploadId())
                .chunkUploadUrls(chunkUrls)
                .expiresAt(task.getExpiresAt().toEpochSecond(ZoneOffset.UTC))
                .message("找到已存在的上传任务")
                .build();
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }

    /**
     * 确定文件类型
     */
    private String determineFileType(String fileExt, String mimeType) {
        List<String> imageExts = uploadProperties.getImage().getAllowedTypesList();
        List<String> videoExts = uploadProperties.getVideo().getAllowedTypesList();
        List<String> audioExts = uploadProperties.getAudio().getAllowedTypesList();

        if (imageExts.contains(fileExt)) {
            return "image";
        } else if (videoExts.contains(fileExt)) {
            return "video";
        } else if (audioExts.contains(fileExt)) {
            return "audio";
        }
        return "document";
    }

    /**
     * 生成对象存储路径
     */
    private String generateObjectKey(String fileType, String fileId, String fileExt) {
        LocalDateTime now = LocalDateTime.now();
        return String.format("%s/%d/%02d/%02d/%s.%s",
                fileType,
                now.getYear(),
                now.getMonthValue(),
                now.getDayOfMonth(),
                fileId,
                fileExt);
    }

    /**
     * 获取状态描述
     */
    private String getStatusDescription(Integer status) {
        switch (status) {
            case 1: return "上传中";
            case 2: return "已完成";
            case 3: return "已取消";
            case 4: return "失败";
            default: return "未知";
        }
    }
}
