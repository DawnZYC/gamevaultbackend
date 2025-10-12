package com.sg.nusiss.gamevaultbackend.service;

import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.UUID;

@Service
public class FileUploadService {

    @Value("${app.upload.path:uploads}")
    private String uploadPath;

    // 允许的图片格式
    private static final String[] ALLOWED_EXTENSIONS = {"jpg", "jpeg", "png", "gif", "webp"};
    
    // 最大文件大小 (5MB)
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    /**
     * 上传头像文件
     * @param file 上传的文件
     * @param userId 用户ID
     * @return 文件访问路径
     * @throws IOException 文件操作异常
     */
    public String uploadAvatar(MultipartFile file, Long userId) throws IOException {
        // 验证文件
        validateFile(file);
        
        // 创建上传目录
        Path uploadDir = Paths.get(uploadPath, "avatars");
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }
        
        // 生成唯一文件名
        String originalFilename = file.getOriginalFilename();
        String extension = FilenameUtils.getExtension(originalFilename);
        String filename = String.format("user_%d_avatar_%s.%s", 
            userId, UUID.randomUUID().toString().substring(0, 8), extension);
        
        // 保存文件
        Path filePath = uploadDir.resolve(filename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        
        // 返回相对路径，用于数据库存储
        return "/uploads/avatars/" + filename;
    }

    /**
     * 验证上传的文件
     * @param file 上传的文件
     * @throws IllegalArgumentException 文件验证失败
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }
        
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("文件大小不能超过5MB");
        }
        
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new IllegalArgumentException("文件名不能为空");
        }
        
        String extension = FilenameUtils.getExtension(originalFilename).toLowerCase();
        if (!Arrays.asList(ALLOWED_EXTENSIONS).contains(extension)) {
            throw new IllegalArgumentException("只支持以下图片格式: " + String.join(", ", ALLOWED_EXTENSIONS));
        }
        
        // 验证文件内容类型
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("文件必须是图片格式");
        }
    }

    /**
     * 上传游戏图片文件
     * @param file 上传的文件
     * @param gameId 游戏ID
     * @return 文件访问路径
     * @throws IOException 文件操作异常
     */
    public String uploadGameImage(MultipartFile file, Long gameId) throws IOException {
        // 验证文件
        validateFile(file);
        
        // 创建上传目录
        Path uploadDir = Paths.get(uploadPath, "games");
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }
        
        // 生成唯一文件名
        String originalFilename = file.getOriginalFilename();
        String extension = FilenameUtils.getExtension(originalFilename);
        String filename = String.format("game_%d_%s.%s", 
            gameId, UUID.randomUUID().toString().substring(0, 8), extension);
        
        // 保存文件
        Path filePath = uploadDir.resolve(filename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        
        // 返回相对路径，用于数据库存储
        return "/uploads/games/" + filename;
    }

    /**
     * 删除头像文件
     * @param avatarUrl 头像URL
     * @return 是否删除成功
     */
    public boolean deleteAvatar(String avatarUrl) {
        if (avatarUrl == null || avatarUrl.isEmpty()) {
            return true;
        }
        
        try {
            // 将相对路径转换为绝对路径
            String filename = avatarUrl.replace("/uploads/avatars/", "");
            Path filePath = Paths.get(uploadPath, "avatars", filename);
            
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                return true;
            }
        } catch (IOException e) {
            System.err.println("删除头像文件失败: " + e.getMessage());
        }
        
        return false;
    }

    /**
     * 删除游戏图片文件
     * @param imageUrl 游戏图片URL
     * @return 是否删除成功
     */
    public boolean deleteGameImage(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return true;
        }
        
        try {
            // 将相对路径转换为绝对路径
            String filename = imageUrl.replace("/uploads/games/", "");
            Path filePath = Paths.get(uploadPath, "games", filename);
            
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                return true;
            }
        } catch (IOException e) {
            System.err.println("删除游戏图片文件失败: " + e.getMessage());
        }
        
        return false;
    }
}
