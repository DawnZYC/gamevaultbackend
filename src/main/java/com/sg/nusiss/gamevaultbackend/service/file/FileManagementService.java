package com.sg.nusiss.gamevaultbackend.service.file;

import com.sg.nusiss.gamevaultbackend.common.ErrorCode;
import com.sg.nusiss.gamevaultbackend.config.FileUploadProperties;
import com.sg.nusiss.gamevaultbackend.dto.file.request.FileDeleteRequest;
import com.sg.nusiss.gamevaultbackend.dto.file.request.FileDownloadRequest;
import com.sg.nusiss.gamevaultbackend.dto.file.request.FileListRequest;
import com.sg.nusiss.gamevaultbackend.dto.file.response.FileDownloadResponse;
import com.sg.nusiss.gamevaultbackend.dto.file.response.FileInfoResponse;
import com.sg.nusiss.gamevaultbackend.entity.file.ChatFileInfo;
import com.sg.nusiss.gamevaultbackend.entity.file.FileAccessLog;
import com.sg.nusiss.gamevaultbackend.exception.BusinessException;
import com.sg.nusiss.gamevaultbackend.repository.file.ChatFileInfoRepository;
import com.sg.nusiss.gamevaultbackend.repository.file.FileAccessLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @ClassName FileManagementService
 * @Author HUANG ZHENJIA
 * @Date 2025/10/19
 * @Description
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class FileManagementService {

    private final MinioService minioService;
    private final ChatFileInfoRepository fileInfoRepository;
    private final FileAccessLogRepository accessLogRepository;
    private final FileUploadProperties uploadProperties;

    /**
     * 获取文件信息
     */
    public FileInfoResponse getFileInfo(String fileId, Long userId) {
        ChatFileInfo fileInfo = fileInfoRepository.findByFileId(fileId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ERROR, "文件不存在"));

        // 检查权限（可选，根据业务需求）
        // if (!fileInfo.getUserId().equals(userId)) {
        //     throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权访问此文件");
        // }

        // 生成新的下载URL（如果过期）
        String downloadUrl = null;
        Long urlExpiresAt = null;

        if (fileInfo.getUrlExpiresAt() == null || fileInfo.getUrlExpiresAt().isBefore(LocalDateTime.now())) {
            int expiresInHours = uploadProperties.getPresigned().getDownloadExpireHours();
            downloadUrl = minioService.generatePresignedDownloadUrl(
                    fileInfo.getBucketName(),
                    fileInfo.getObjectKey(),
                    expiresInHours * 60
            );
            LocalDateTime expiresTime = LocalDateTime.now().plusHours(expiresInHours);
            urlExpiresAt = expiresTime.toEpochSecond(ZoneOffset.UTC);

            // 更新数据库中的URL
            fileInfo.setPresignedUrl(downloadUrl);
            fileInfo.setUrlExpiresAt(expiresTime);
            fileInfoRepository.save(fileInfo);
        } else {
            downloadUrl = fileInfo.getPresignedUrl();
            urlExpiresAt = fileInfo.getUrlExpiresAt().toEpochSecond(ZoneOffset.UTC);
        }

        return FileInfoResponse.builder()
                .fileId(fileInfo.getFileId())
                .fileName(fileInfo.getFileName())
                .fileSize(fileInfo.getFileSize())
                .fileSizeFormatted(formatFileSize(fileInfo.getFileSize()))
                .fileType(fileInfo.getFileType())
                .mimeType(fileInfo.getMimeType())
                .fileExt(fileInfo.getFileExt())
                .accessUrl(fileInfo.getAccessUrl())
                .downloadUrl(downloadUrl)
                .urlExpiresAt(urlExpiresAt)
                .thumbnailUrl(fileInfo.getThumbnailUrl())
                .width(fileInfo.getWidth())
                .height(fileInfo.getHeight())
                .duration(fileInfo.getDuration())
                .downloadCount(fileInfo.getDownloadCount())
                .status(fileInfo.getStatus())
                .userId(fileInfo.getUserId())
                .bizType(fileInfo.getBizType())
                .bizId(fileInfo.getBizId())
                .createdAt(fileInfo.getCreatedAt())
                .updatedAt(fileInfo.getUpdatedAt())
                .build();
    }

    /**
     * 获取文件下载URL
     */
    @Transactional
    public FileDownloadResponse getDownloadUrl(FileDownloadRequest request, Long userId, String ipAddress) {
        ChatFileInfo fileInfo = fileInfoRepository.findByFileId(request.getFileId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ERROR, "文件不存在"));

        // 生成下载URL
        int expiresInMinutes = request.getExpiresInMinutes() != null
                ? request.getExpiresInMinutes()
                : uploadProperties.getPresigned().getDownloadExpireHours() * 60;

        String downloadUrl = minioService.generatePresignedDownloadUrl(
                fileInfo.getBucketName(),
                fileInfo.getObjectKey(),
                expiresInMinutes
        );

        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(expiresInMinutes);

        // 更新下载次数
        fileInfo.setDownloadCount(fileInfo.getDownloadCount() + 1);
        fileInfoRepository.save(fileInfo);

        // 记录访问日志
        if (request.getRecordLog()) {
            recordAccessLog(fileInfo.getFileId(), userId, 2, ipAddress, null);
        }

        log.info("Generated download URL for file: {}, user: {}", request.getFileId(), userId);

        return FileDownloadResponse.builder()
                .fileId(fileInfo.getFileId())
                .fileName(fileInfo.getFileName())
                .fileSize(fileInfo.getFileSize())
                .fileType(fileInfo.getFileType())
                .mimeType(fileInfo.getMimeType())
                .downloadUrl(downloadUrl)
                .urlExpiresAt(expiresAt.toEpochSecond(ZoneOffset.UTC))
                .message("下载链接生成成功")
                .build();
    }

    /**
     * 查询文件列表
     */
    public Page<FileInfoResponse> listFiles(FileListRequest request, Long currentUserId) {
        // 构建查询条件
        Long userId = request.getUserId() != null ? request.getUserId() : currentUserId;

        // 构建分页和排序
        Sort sort = Sort.by(
                "desc".equalsIgnoreCase(request.getSortDir())
                        ? Sort.Direction.DESC
                        : Sort.Direction.ASC,
                request.getSortBy()
        );

        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), sort);

        // 根据不同条件查询
        Page<ChatFileInfo> filePage;

        if (request.getBizType() != null && request.getBizId() != null) {
            // 按业务类型和业务ID查询
            List<ChatFileInfo> files = fileInfoRepository.findByBizTypeAndBizIdAndStatusOrderByCreatedAtDesc(
                    request.getBizType(), request.getBizId(), request.getStatus());
            filePage = new org.springframework.data.domain.PageImpl<>(
                    files, pageable, files.size());
        } else if (request.getFileType() != null) {
            // 按文件类型查询
            List<ChatFileInfo> files = fileInfoRepository.findByUserIdAndFileTypeAndStatusOrderByCreatedAtDesc(
                    userId, request.getFileType(), request.getStatus());
            filePage = new org.springframework.data.domain.PageImpl<>(
                    files, pageable, files.size());
        } else {
            // 查询用户所有文件
            List<ChatFileInfo> files = fileInfoRepository.findByUserIdAndStatusOrderByCreatedAtDesc(
                    userId, request.getStatus());
            filePage = new org.springframework.data.domain.PageImpl<>(
                    files, pageable, files.size());
        }

        // 转换为DTO
        return filePage.map(this::convertToResponse);
    }

    /**
     * 删除文件
     */
    @Transactional
    public void deleteFile(FileDeleteRequest request, Long userId, String ipAddress) {
        ChatFileInfo fileInfo = fileInfoRepository.findByFileId(request.getFileId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ERROR, "文件不存在"));

        // 检查权限
        if (!fileInfo.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权删除此文件");
        }

        if (request.getPhysicalDelete()) {
            // 物理删除：从MinIO删除文件，从数据库删除记录
            try {
                minioService.deleteFile(fileInfo.getBucketName(), fileInfo.getObjectKey());
            } catch (Exception e) {
                log.error("Failed to delete file from MinIO: {}", request.getFileId(), e);
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "文件删除失败");
            }

            fileInfoRepository.delete(fileInfo);
            log.info("File physically deleted: {}", request.getFileId());
        } else {
            // 逻辑删除：只修改状态
            fileInfo.setStatus(2); // 2-已删除
            fileInfoRepository.save(fileInfo);
            log.info("File logically deleted: {}", request.getFileId());
        }

        // 记录访问日志
        recordAccessLog(fileInfo.getFileId(), userId, 4, ipAddress, null);
    }

    /**
     * 批量删除文件
     */
    @Transactional
    public void batchDeleteFiles(List<String> fileIds, Long userId, boolean physicalDelete) {
        for (String fileId : fileIds) {
            try {
                FileDeleteRequest request = FileDeleteRequest.builder()
                        .fileId(fileId)
                        .physicalDelete(physicalDelete)
                        .build();
                deleteFile(request, userId, null);
            } catch (Exception e) {
                log.error("Failed to delete file: {}", fileId, e);
            }
        }
    }

    /**
     * 获取用户文件统计信息
     */
    public UserFileStats getUserFileStats(Long userId) {
        Long totalSize = fileInfoRepository.sumFileSizeByUserId(userId);
        Long totalCount = fileInfoRepository.countByUserIdAndStatus(userId, 1);

        return UserFileStats.builder()
                .totalFiles(totalCount)
                .totalSize(totalSize != null ? totalSize : 0L)
                .totalSizeFormatted(formatFileSize(totalSize != null ? totalSize : 0L))
                .build();
    }

    /**
     * 记录访问日志
     */
    private void recordAccessLog(String fileId, Long userId, Integer accessType,
                                 String ipAddress, String userAgent) {
        try {
            FileAccessLog log = FileAccessLog.builder()
                    .fileId(fileId)
                    .userId(userId)
                    .accessType(accessType)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .build();

            accessLogRepository.save(log);
        } catch (Exception e) {
            // 日志记录失败不影响主流程
            log.error("Failed to record access log", e);
        }
    }

    /**
     * 转换为响应DTO
     */
    private FileInfoResponse convertToResponse(ChatFileInfo fileInfo) {
        return FileInfoResponse.builder()
                .fileId(fileInfo.getFileId())
                .fileName(fileInfo.getFileName())
                .fileSize(fileInfo.getFileSize())
                .fileSizeFormatted(formatFileSize(fileInfo.getFileSize()))
                .fileType(fileInfo.getFileType())
                .mimeType(fileInfo.getMimeType())
                .fileExt(fileInfo.getFileExt())
                .accessUrl(fileInfo.getAccessUrl())
                .thumbnailUrl(fileInfo.getThumbnailUrl())
                .width(fileInfo.getWidth())
                .height(fileInfo.getHeight())
                .duration(fileInfo.getDuration())
                .downloadCount(fileInfo.getDownloadCount())
                .status(fileInfo.getStatus())
                .userId(fileInfo.getUserId())
                .bizType(fileInfo.getBizType())
                .bizId(fileInfo.getBizId())
                .createdAt(fileInfo.getCreatedAt())
                .updatedAt(fileInfo.getUpdatedAt())
                .build();
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

    /**
     * 用户文件统计信息
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class UserFileStats {
        private Long totalFiles;
        private Long totalSize;
        private String totalSizeFormatted;
    }
}
