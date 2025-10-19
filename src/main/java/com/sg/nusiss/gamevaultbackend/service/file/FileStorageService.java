package com.sg.nusiss.gamevaultbackend.service.file;

import com.sg.nusiss.gamevaultbackend.common.ErrorCode;
import com.sg.nusiss.gamevaultbackend.config.FileUploadProperties;
import com.sg.nusiss.gamevaultbackend.dto.file.request.*;
import com.sg.nusiss.gamevaultbackend.dto.file.response.*;
import com.sg.nusiss.gamevaultbackend.entity.file.ChatFileInfo;
import com.sg.nusiss.gamevaultbackend.exception.BusinessException;
import com.sg.nusiss.gamevaultbackend.repository.file.ChatFileInfoRepository;
import com.sg.nusiss.gamevaultbackend.repository.file.FileChunkInfoRepository;
import com.sg.nusiss.gamevaultbackend.repository.file.FileUploadTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * @ClassName FileUploadService
 * @Author HUANG ZHENJIA
 * @Date 2025/10/19
 * @Description
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final MinioService minioService;
    private final ChatFileInfoRepository fileInfoRepository;
    private final FileUploadTaskRepository uploadTaskRepository;
    private final FileChunkInfoRepository chunkInfoRepository;
    private final FileUploadProperties uploadProperties;

    /**
     * 简单文件上传（小文件直传）
     */
    @Transactional
    public FileUploadResponse uploadFile(FileUploadRequest request, MultipartFile file, Long userId) {
        log.info("Starting file upload: {}, size: {}, user: {}",
                request.getFileName(), request.getFileSize(), userId);

        // 1. 文件校验
        validateFile(request, file);

        // 2. 检查是否可以秒传
        if (request.getFileMd5() != null && uploadProperties.getQuickUpload().getEnabled()) {
            Optional<ChatFileInfo> existingFile = fileInfoRepository.findFirstByFileMd5AndStatusOrderByCreatedAtDesc(
                    request.getFileMd5(), 1);

            if (existingFile.isPresent()) {
                return handleQuickUpload(existingFile.get(), request, userId);
            }
        }

        // 3. 生成文件信息
        String fileId = UUID.randomUUID().toString().replace("-", "");
        String fileExt = getFileExtension(request.getFileName());
        String fileType = determineFileType(fileExt, request.getMimeType());
        String bucketName = minioService.getBucketNameByFileType(fileType);
        String objectKey = generateObjectKey(fileType, fileId, fileExt);

        // 4. 判断是否需要分片上传
        boolean needChunkUpload = request.getFileSize() > uploadProperties.getChunk().getMinFileSize();

        if (needChunkUpload) {
            // 返回需要分片上传的响应
            return FileUploadResponse.builder()
                    .fileId(fileId)
                    .fileName(request.getFileName())
                    .fileSize(request.getFileSize())
                    .fileType(fileType)
                    .fileExt(fileExt)
                    .needChunkUpload(true)
                    .quickUpload(false)
                    .message("文件较大，请使用分片上传")
                    .build();
        }

        // 5. 小文件直接上传
        try {
            minioService.uploadFile(bucketName, objectKey, file);
        } catch (Exception e) {
            log.error("Failed to upload file to MinIO", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "文件上传失败");
        }

        // 6. 保存文件信息到数据库
        ChatFileInfo fileInfo = ChatFileInfo.builder()
                .fileId(fileId)
                .fileName(request.getFileName())
                .fileSize(request.getFileSize())
                .fileType(fileType)
                .mimeType(request.getMimeType())
                .fileExt(fileExt)
                .bucketName(bucketName)
                .objectKey(objectKey)
                .storagePath(bucketName + "/" + objectKey)
                .fileMd5(request.getFileMd5())
                .fileSha256(request.getFileSha256())
                .status(1)
                .userId(userId)
                .bizType(request.getBizType())
                .bizId(request.getBizId())
                .createdBy(userId)
                .build();

        fileInfoRepository.save(fileInfo);

        // 7. 生成访问URL
        String accessUrl = generateAccessUrl(bucketName, objectKey);

        log.info("File uploaded successfully: fileId={}, objectKey={}", fileId, objectKey);

        return FileUploadResponse.builder()
                .fileId(fileId)
                .fileName(request.getFileName())
                .fileSize(request.getFileSize())
                .fileType(fileType)
                .fileExt(fileExt)
                .accessUrl(accessUrl)
                .quickUpload(false)
                .needChunkUpload(false)
                .message("上传成功")
                .build();
    }

    /**
     * 生成预签名上传URL（前端直传）
     */
    @Transactional
    public FileUploadResponse generateUploadUrl(FileUploadRequest request, Long userId) {
        log.info("Generating upload URL: {}, user: {}", request.getFileName(), userId);

        // 1. 检查是否可以秒传
        if (request.getFileMd5() != null && uploadProperties.getQuickUpload().getEnabled()) {
            Optional<ChatFileInfo> existingFile = fileInfoRepository.findFirstByFileMd5AndStatusOrderByCreatedAtDesc(
                    request.getFileMd5(), 1);

            if (existingFile.isPresent()) {
                return handleQuickUpload(existingFile.get(), request, userId);
            }
        }

        // 2. 生成文件信息
        String fileId = UUID.randomUUID().toString().replace("-", "");
        String fileExt = getFileExtension(request.getFileName());
        String fileType = determineFileType(fileExt, request.getMimeType());
        String bucketName = minioService.getBucketNameByFileType(fileType);
        String objectKey = generateObjectKey(fileType, fileId, fileExt);

        // 3. 判断是否需要分片上传
        boolean needChunkUpload = request.getFileSize() > uploadProperties.getChunk().getMinFileSize();

        if (needChunkUpload) {
            return FileUploadResponse.builder()
                    .fileId(fileId)
                    .fileName(request.getFileName())
                    .fileSize(request.getFileSize())
                    .fileType(fileType)
                    .fileExt(fileExt)
                    .needChunkUpload(true)
                    .quickUpload(false)
                    .message("文件较大，请使用分片上传")
                    .build();
        }

        // 4. 生成预签名上传URL
        int expiresInMinutes = uploadProperties.getPresigned().getUploadExpireMinutes();
        String uploadUrl = minioService.generatePresignedUploadUrl(bucketName, objectKey, expiresInMinutes);
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(expiresInMinutes);

        // 5. 保存文件信息到数据库（状态为待上传）
        ChatFileInfo fileInfo = ChatFileInfo.builder()
                .fileId(fileId)
                .fileName(request.getFileName())
                .fileSize(request.getFileSize())
                .fileType(fileType)
                .mimeType(request.getMimeType())
                .fileExt(fileExt)
                .bucketName(bucketName)
                .objectKey(objectKey)
                .storagePath(bucketName + "/" + objectKey)
                .fileMd5(request.getFileMd5())
                .fileSha256(request.getFileSha256())
                .status(3) // 3-审核中/待上传
                .userId(userId)
                .bizType(request.getBizType())
                .bizId(request.getBizId())
                .presignedUrl(uploadUrl)
                .urlExpiresAt(expiresAt)
                .createdBy(userId)
                .build();

        fileInfoRepository.save(fileInfo);

        log.info("Generated upload URL: fileId={}, expires at: {}", fileId, expiresAt);

        return FileUploadResponse.builder()
                .fileId(fileId)
                .fileName(request.getFileName())
                .fileSize(request.getFileSize())
                .fileType(fileType)
                .fileExt(fileExt)
                .uploadUrl(uploadUrl)
                .urlExpiresAt(expiresAt.toEpochSecond(ZoneOffset.UTC))
                .quickUpload(false)
                .needChunkUpload(false)
                .message("请使用提供的URL直接上传文件")
                .build();
    }

    /**
     * 处理秒传
     */
    private FileUploadResponse handleQuickUpload(ChatFileInfo existingFile,
                                                 FileUploadRequest request, Long userId) {
        log.info("Quick upload - file already exists: {}, md5: {}",
                existingFile.getFileId(), request.getFileMd5());

        // 创建新的文件记录（引用相同的存储对象）
        String newFileId = UUID.randomUUID().toString().replace("-", "");

        ChatFileInfo newFileInfo = ChatFileInfo.builder()
                .fileId(newFileId)
                .fileName(request.getFileName())
                .fileSize(existingFile.getFileSize())
                .fileType(existingFile.getFileType())
                .mimeType(existingFile.getMimeType())
                .fileExt(existingFile.getFileExt())
                .bucketName(existingFile.getBucketName())
                .objectKey(existingFile.getObjectKey())
                .storagePath(existingFile.getStoragePath())
                .fileMd5(existingFile.getFileMd5())
                .fileSha256(existingFile.getFileSha256())
                .status(1)
                .userId(userId)
                .bizType(request.getBizType())
                .bizId(request.getBizId())
                .thumbnailUrl(existingFile.getThumbnailUrl())
                .width(existingFile.getWidth())
                .height(existingFile.getHeight())
                .duration(existingFile.getDuration())
                .createdBy(userId)
                .build();

        fileInfoRepository.save(newFileInfo);

        String accessUrl = generateAccessUrl(existingFile.getBucketName(), existingFile.getObjectKey());

        return FileUploadResponse.builder()
                .fileId(newFileId)
                .fileName(request.getFileName())
                .fileSize(existingFile.getFileSize())
                .fileType(existingFile.getFileType())
                .fileExt(existingFile.getFileExt())
                .accessUrl(accessUrl)
                .thumbnailUrl(existingFile.getThumbnailUrl())
                .quickUpload(true)
                .needChunkUpload(false)
                .message("文件秒传成功")
                .build();
    }

    /**
     * 文件校验
     */
    private void validateFile(FileUploadRequest request, MultipartFile file) {
        String fileExt = getFileExtension(request.getFileName()).toLowerCase();
        String fileType = determineFileType(fileExt, request.getMimeType());

        // 检查文件类型是否允许
        List<String> allowedTypes;
        Long maxSize;

        switch (fileType) {
            case "image":
                allowedTypes = uploadProperties.getImage().getAllowedTypesList();
                maxSize = uploadProperties.getImage().getMaxSize();
                break;
            case "video":
                allowedTypes = uploadProperties.getVideo().getAllowedTypesList();
                maxSize = uploadProperties.getVideo().getMaxSize();
                break;
            case "audio":
                allowedTypes = uploadProperties.getAudio().getAllowedTypesList();
                maxSize = uploadProperties.getAudio().getMaxSize();
                break;
            case "document":
                allowedTypes = uploadProperties.getDocument().getAllowedTypesList();
                maxSize = uploadProperties.getDocument().getMaxSize();
                break;
            default:
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的文件类型");
        }

        if (!allowedTypes.contains(fileExt)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不允许上传该类型的文件: " + fileExt);
        }

        if (request.getFileSize() > maxSize) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件大小超过限制: " + formatFileSize(maxSize));
        }
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
        // 根据扩展名判断
        List<String> imageExts = uploadProperties.getImage().getAllowedTypesList();
        List<String> videoExts = uploadProperties.getVideo().getAllowedTypesList();
        List<String> audioExts = uploadProperties.getAudio().getAllowedTypesList();
        List<String> docExts = uploadProperties.getDocument().getAllowedTypesList();

        if (imageExts.contains(fileExt)) {
            return "image";
        } else if (videoExts.contains(fileExt)) {
            return "video";
        } else if (audioExts.contains(fileExt)) {
            return "audio";
        } else if (docExts.contains(fileExt)) {
            return "document";
        }

        // 根据MIME类型判断
        if (mimeType != null) {
            if (mimeType.startsWith("image/")) {
                return "image";
            } else if (mimeType.startsWith("video/")) {
                return "video";
            } else if (mimeType.startsWith("audio/")) {
                return "audio";
            }
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
     * 生成访问URL
     */
    private String generateAccessUrl(String bucketName, String objectKey) {
        int expiresInHours = uploadProperties.getPresigned().getDownloadExpireHours();
        return minioService.generatePresignedDownloadUrl(bucketName, objectKey, expiresInHours * 60);
    }

    /**
     * 格式化文件大小
     */
    private String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.2f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", size / 1024.0 / 1024.0);
        } else {
            return String.format("%.2f GB", size / 1024.0 / 1024.0 / 1024.0);
        }
    }
}
