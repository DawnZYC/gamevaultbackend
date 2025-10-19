package com.sg.nusiss.gamevaultbackend.service.file;

import com.sg.nusiss.gamevaultbackend.config.MinioConfig;
import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @ClassName MinioService
 * @Author HUANG ZHENJIA
 * @Date 2025/10/19
 * @Description
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioService {

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    /**
     * 检查存储桶是否存在，不存在则创建
     */
    public void ensureBucketExists(String bucketName) {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucketName).build()
            );

            if (!exists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(bucketName).build()
                );
                log.info("Created bucket: {}", bucketName);
            }
        } catch (Exception e) {
            log.error("Error ensuring bucket exists: {}", bucketName, e);
            throw new RuntimeException("无法创建存储桶: " + bucketName, e);
        }
    }

    /**
     * 上传文件
     */
    public String uploadFile(String bucketName, String objectName, MultipartFile file) {
        try {
            ensureBucketExists(bucketName);

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            log.info("File uploaded successfully: {}/{}", bucketName, objectName);
            return objectName;
        } catch (Exception e) {
            log.error("Error uploading file: {}/{}", bucketName, objectName, e);
            throw new RuntimeException("文件上传失败", e);
        }
    }

    /**
     * 上传文件流
     */
    public String uploadFile(String bucketName, String objectName, InputStream inputStream,
                             long size, String contentType) {
        try {
            ensureBucketExists(bucketName);

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(inputStream, size, -1)
                            .contentType(contentType)
                            .build()
            );

            log.info("File stream uploaded successfully: {}/{}", bucketName, objectName);
            return objectName;
        } catch (Exception e) {
            log.error("Error uploading file stream: {}/{}", bucketName, objectName, e);
            throw new RuntimeException("文件流上传失败", e);
        }
    }

    /**
     * 生成预签名上传URL
     */
    public String generatePresignedUploadUrl(String bucketName, String objectName, int expiresInMinutes) {
        try {
            ensureBucketExists(bucketName);

            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.PUT)
                            .bucket(bucketName)
                            .object(objectName)
                            .expiry(expiresInMinutes, TimeUnit.MINUTES)
                            .build()
            );
        } catch (Exception e) {
            log.error("Error generating presigned upload URL: {}/{}", bucketName, objectName, e);
            throw new RuntimeException("生成上传URL失败", e);
        }
    }

    /**
     * 生成预签名下载URL
     */
    public String generatePresignedDownloadUrl(String bucketName, String objectName, int expiresInMinutes) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(objectName)
                            .expiry(expiresInMinutes, TimeUnit.MINUTES)
                            .build()
            );
        } catch (Exception e) {
            log.error("Error generating presigned download URL: {}/{}", bucketName, objectName, e);
            throw new RuntimeException("生成下载URL失败", e);
        }
    }

    /**
     * 获取文件流
     */
    public InputStream getFileStream(String bucketName, String objectName) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
        } catch (Exception e) {
            log.error("Error getting file stream: {}/{}", bucketName, objectName, e);
            throw new RuntimeException("获取文件流失败", e);
        }
    }

    /**
     * 删除文件
     */
    public void deleteFile(String bucketName, String objectName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
            log.info("File deleted successfully: {}/{}", bucketName, objectName);
        } catch (Exception e) {
            log.error("Error deleting file: {}/{}", bucketName, objectName, e);
            throw new RuntimeException("文件删除失败", e);
        }
    }

    /**
     * 检查文件是否存在
     */
    public boolean fileExists(String bucketName, String objectName) {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 生成分片上传的预签名URL（简化版本）
     * 注意：这是一个简化实现，生成多个独立的上传URL
     */
    public String generatePresignedUploadPartUrl(String bucketName, String objectName,
                                                 int partNumber, int expiresInMinutes) {
        try {
            ensureBucketExists(bucketName);

            // 为每个分片生成唯一的对象名
            String partObjectName = objectName + ".part" + partNumber;

            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.PUT)
                            .bucket(bucketName)
                            .object(partObjectName)
                            .expiry(expiresInMinutes, TimeUnit.MINUTES)
                            .build()
            );
        } catch (Exception e) {
            log.error("Error generating presigned upload part URL: {}/{}, partNumber: {}",
                    bucketName, objectName, partNumber, e);
            throw new RuntimeException("生成分片上传URL失败", e);
        }
    }

    /**
     * 合并分片文件（简化版本）
     * 注意：这是一个简化实现，实际使用 ComposeSource 来合并
     */
    public String mergeChunks(String bucketName, String objectName, int totalChunks) {
        try {
            // 使用 ComposeSource 来合并分片
            List<ComposeSource> sources = new java.util.ArrayList<>();

            for (int i = 1; i <= totalChunks; i++) {
                String partObjectName = objectName + ".part" + i;
                sources.add(
                        ComposeSource.builder()
                                .bucket(bucketName)
                                .object(partObjectName)
                                .build()
                );
            }

            // 合并所有分片到最终文件
            minioClient.composeObject(
                    ComposeObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .sources(sources)
                            .build()
            );

            // 删除临时分片文件
            for (int i = 1; i <= totalChunks; i++) {
                String partObjectName = objectName + ".part" + i;
                try {
                    deleteFile(bucketName, partObjectName);
                } catch (Exception e) {
                    log.warn("Failed to delete part file: {}", partObjectName, e);
                }
            }

            log.info("Merged {} chunks successfully: {}/{}", totalChunks, bucketName, objectName);
            return objectName;
        } catch (Exception e) {
            log.error("Error merging chunks: {}/{}", bucketName, objectName, e);
            throw new RuntimeException("合并分片失败", e);
        }
    }

    /**
     * 复制文件（用于秒传）
     */
    public void copyFile(String sourceBucket, String sourceObject,
                         String targetBucket, String targetObject) {
        try {
            ensureBucketExists(targetBucket);

            minioClient.copyObject(
                    CopyObjectArgs.builder()
                            .bucket(targetBucket)
                            .object(targetObject)
                            .source(CopySource.builder()
                                    .bucket(sourceBucket)
                                    .object(sourceObject)
                                    .build())
                            .build()
            );

            log.info("File copied successfully: {}/{} -> {}/{}",
                    sourceBucket, sourceObject, targetBucket, targetObject);
        } catch (Exception e) {
            log.error("Error copying file: {}/{} -> {}/{}",
                    sourceBucket, sourceObject, targetBucket, targetObject, e);
            throw new RuntimeException("文件复制失败", e);
        }
    }

    /**
     * 根据文件类型获取对应的存储桶名称
     */
    public String getBucketNameByFileType(String fileType) {
        if (fileType == null) {
            return minioConfig.getBucketName();
        }

        switch (fileType.toLowerCase()) {
            case "image":
                return minioConfig.getImageBucket();
            case "video":
                return minioConfig.getVideoBucket();
            case "audio":
                return minioConfig.getAudioBucket();
            case "document":
                return minioConfig.getFileBucket();
            default:
                return minioConfig.getBucketName();
        }
    }
}
