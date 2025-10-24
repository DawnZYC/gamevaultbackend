package com.sg.nusiss.gamevaultbackend.service.developer;

import com.sg.nusiss.gamevaultbackend.dto.developer.DevGameResponse;
import com.sg.nusiss.gamevaultbackend.dto.developer.DevGameUploadRequest;
import com.sg.nusiss.gamevaultbackend.dto.developer.OperationResult;
import com.sg.nusiss.gamevaultbackend.entity.developer.DevGame;
import com.sg.nusiss.gamevaultbackend.entity.developer.DevGameAsset;
import com.sg.nusiss.gamevaultbackend.entity.developer.DeveloperProfile;
import com.sg.nusiss.gamevaultbackend.repository.developer.DevGameAssetRepository;
import com.sg.nusiss.gamevaultbackend.repository.developer.DevGameRepository;
import com.sg.nusiss.gamevaultbackend.repository.developer.DevGameStatisticsRepository;
import com.sg.nusiss.gamevaultbackend.repository.developer.DeveloperProfileRepository;
import com.sg.nusiss.gamevaultbackend.util.developer.AssetUrlBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DevGameApplicationService {
    @Value("${app.asset-storage-path}")
    private String assetStoragePath;
    private final DevGameRepository devGameRepository;
    private final DevGameAssetRepository devGameAssetRepository;
    private final DeveloperProfileRepository developerProfileRepository;
    private final DevGameStatisticsRepository devGameStatisticsRepository;
    private final AssetUrlBuilder assetUrlBuilder;

    @Transactional
    public DevGameResponse uploadGame(DevGameUploadRequest request) {
        DeveloperProfile developer = developerProfileRepository.findById(request.getDeveloperId())
                .orElseThrow(() -> new IllegalArgumentException("Developer profile not found"));

        String gameId = UUID.randomUUID().toString();

        DevGame game = new DevGame(
                gameId,
                request.getDeveloperId(),
                request.getName(),
                request.getDescription(),
                request.getReleaseDate(),
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        devGameRepository.insert(game);

        // Save assets and get their IDs
        String imageAssetId = saveAsset(developer.getUserId(), request.getName(), gameId, request.getImage(), "image");
        String videoAssetId = null;
        String zipAssetId = saveAsset(developer.getUserId(), request.getName(), gameId, request.getZip(), "zip");

        // Handle optional video file
        if (request.getVideo() != null && !request.getVideo().isEmpty()) {
            videoAssetId = saveAsset(developer.getUserId(), request.getName(), gameId, request.getVideo(), "video");
        }

        // Build download URLs
        String imageUrl = assetUrlBuilder.buildDownloadUrl(imageAssetId);
        String videoUrl = videoAssetId != null ? assetUrlBuilder.buildDownloadUrl(videoAssetId) : null;
        String zipUrl = assetUrlBuilder.buildDownloadUrl(zipAssetId);

        developerProfileRepository.syncProjectCount(request.getDeveloperId());

        return new DevGameResponse(gameId, request.getName(), request.getDescription(),
                imageUrl, videoUrl, zipUrl);
    }

    private static final long MAX_FILE_SIZE_BYTES = 200L * 1024 * 1024; // 200 MB

    private String saveAsset(String userId, String gameName, String gameId, MultipartFile file, String assetType) {
        try {
            // 验证文件不为空
            if (file == null || file.isEmpty()) {
                throw new IllegalArgumentException("File is null or empty for asset type: " + assetType);
            }
            
            if (file.getSize() > MAX_FILE_SIZE_BYTES) {
                throw new IllegalArgumentException("File exceeds maximum allowed size (200MB)");
            }

            String contentType = file.getContentType();
            if (assetType.equalsIgnoreCase("image")) {
                if (contentType == null || !contentType.startsWith("image/")) {
                    throw new IllegalArgumentException("Invalid image file type — only image files are allowed!");
                }
            } else if (assetType.equalsIgnoreCase("video")) {
                if (contentType == null || !contentType.startsWith("video/")) {
                    throw new IllegalArgumentException("Invalid video file type — only video files are allowed!");
                }
            } else if (assetType.equalsIgnoreCase("zip")) {
                if (contentType == null ||
                        !(contentType.equals("application/zip") ||
                                contentType.equals("application/x-zip-compressed"))) {
                    throw new IllegalArgumentException("Invalid ZIP file type — only .zip files are allowed!");
                }
            } else {
                throw new IllegalArgumentException("Unknown asset type: " + assetType);
            }

            String safeUserId = sanitizePathSegment(userId);

            // 确保使用绝对路径
            String basePath;
            if (assetStoragePath.startsWith("/") || assetStoragePath.contains(":")) {
                // 已经是绝对路径
                basePath = assetStoragePath.endsWith("/")
                        ? assetStoragePath
                        : assetStoragePath + "/";
            } else {
                // 相对路径，转换为绝对路径
                String projectRoot = System.getProperty("user.dir");
                basePath = projectRoot + "/" + assetStoragePath;
                basePath = basePath.endsWith("/") ? basePath : basePath + "/";
            }
            basePath = basePath + safeUserId + "/" + gameId + "/";

            Path folder = Paths.get(basePath);
            Files.createDirectories(folder);

            String fileName = file.getOriginalFilename();
            if (fileName == null || fileName.trim().isEmpty()) {
                throw new IllegalArgumentException("File name is null or empty");
            }
            
            String storagePath = basePath + fileName;
            File dest = new File(storagePath);
            
            // 确保父目录存在
            dest.getParentFile().mkdirs();
            
            // 保存文件
            file.transferTo(dest);
            
            // 验证文件是否真的保存成功
            if (!dest.exists() || dest.length() == 0) {
                throw new RuntimeException("File was not saved successfully: " + storagePath);
            }

            String assetId = UUID.randomUUID().toString();
            DevGameAsset asset = new DevGameAsset(
                    assetId,
                    gameId,
                    assetType,
                    fileName,
                    storagePath,
                    file.getSize(),
                    file.getContentType(),
                    LocalDateTime.now()
            );
            
            // 保存到数据库
            devGameAssetRepository.insert(asset);

            // Return asset ID instead of relative path
            return assetId;

        } catch (IOException e) {
            throw new RuntimeException("Failed to save asset: " + assetType, e);
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error saving asset: " + assetType, e);
        }
    }



    @Transactional
    public OperationResult deleteGame(String userId, String gameId) {
        try {
            // 1️⃣ 查询当前用户对应的 DeveloperProfile
            var developerProfileOpt = developerProfileRepository.findByUserId(userId);
            if (developerProfileOpt.isEmpty()) {
                return OperationResult.failure("Developer profile not found for userId: " + userId);
            }
            var developerProfile = developerProfileOpt.get();

            // 2️⃣ 查询游戏是否存在且属于该开发者
            var gameOpt = devGameRepository.findById(gameId);
            if (gameOpt.isEmpty()) {
                return OperationResult.failure("Game not found: " + gameId);
            }

            var game = gameOpt.get();
            if (!game.getDeveloperProfileId().equals(developerProfile.getId())) {
                return OperationResult.failure("Unauthorized: this game does not belong to current developer.");
            }

            // 3️⃣ 删除游戏相关资源
            devGameAssetRepository.deleteByGameId(gameId);
            devGameStatisticsRepository.deleteByGameId(gameId);

            // 4️⃣ 删除主游戏记录
            devGameRepository.deleteById(gameId);

            // 5️⃣ 同步开发者项目数
            developerProfileRepository.syncProjectCount(developerProfile.getId());

            return OperationResult.success("Game deleted successfully: " + gameId);

        } catch (Exception e) {
            e.printStackTrace();
            return OperationResult.failure("Failed to delete game: " + e.getMessage());
        }
    }


    @Transactional
    public DevGameResponse updateGame(
            String userId,
            String gameId,
            String name,
            String description,
            String releaseDate,
            MultipartFile image,
            MultipartFile video,
            MultipartFile zip
    ) {
        DevGame game = devGameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("Game not found: " + gameId));

        DeveloperProfile profile = developerProfileRepository
                .findById(game.getDeveloperProfileId())
                .orElseThrow(() -> new IllegalArgumentException("Developer profile not found for this game"));

        // Verify that the user has permission to update this game
        if (!profile.getUserId().equals(userId)) {
            throw new IllegalArgumentException("You don't have permission to update this game");
        }

        game.setName(name);
        game.setDescription(description);
        if (releaseDate != null && !releaseDate.isBlank()) {
            try {
                LocalDateTime parsedDate = OffsetDateTime.parse(releaseDate, DateTimeFormatter.ISO_DATE_TIME)
                        .toLocalDateTime();
                game.setReleaseDate(parsedDate);
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid releaseDate format: " + releaseDate, e);
            }
        }
        devGameRepository.update(game);

        String imageUrl = null;
        String videoUrl = null;
        String zipUrl = null;

        if (image != null && !image.isEmpty()) {
            String assetId = saveAsset(userId, game.getName(), gameId, image, "image");
            imageUrl = assetUrlBuilder.buildDownloadUrl(assetId);
        }
        if (video != null && !video.isEmpty()) {
            String assetId = saveAsset(userId, game.getName(), gameId, video, "video");
            videoUrl = assetUrlBuilder.buildDownloadUrl(assetId);
        }
        if (zip != null && !zip.isEmpty()) {
            String assetId = saveAsset(userId, game.getName(), gameId, zip, "zip");
            zipUrl = assetUrlBuilder.buildDownloadUrl(assetId);
        }

        return new DevGameResponse(
                game.getId(),
                game.getName(),
                game.getDescription(),
                imageUrl,
                videoUrl,
                zipUrl
        );
    }

    private String sanitizePathSegment(String input) {
        return input.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

}
