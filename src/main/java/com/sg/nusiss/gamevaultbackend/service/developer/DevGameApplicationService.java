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

        String imageUrl = saveAsset(developer.getUserId(), request.getName(), gameId, request.getImage(), "image");
        String videoUrl = saveAsset(developer.getUserId(), request.getName(), gameId, request.getVideo(), "video");
        String zipUrl   = saveAsset(developer.getUserId(), request.getName(), gameId, request.getZip(), "zip");


        developerProfileRepository.syncProjectCount(request.getDeveloperId());

        return new DevGameResponse(gameId, request.getName(), request.getDescription(),
                imageUrl, videoUrl, zipUrl);
    }

    private static final long MAX_FILE_SIZE_BYTES = 200L * 1024 * 1024; // 200 MB

    private String saveAsset(String userId, String gameName, String gameId, MultipartFile file, String assetType) {
        try {
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

            String basePath = assetStoragePath.endsWith("/")
                    ? assetStoragePath
                    : assetStoragePath + "/";
            basePath = basePath + safeUserId + "/" + gameId + "/";

            Path folder = Paths.get(basePath);
            Files.createDirectories(folder);

            String fileName = file.getOriginalFilename();
            String storagePath = basePath + fileName;
            File dest = new File(storagePath);
            file.transferTo(dest);

            DevGameAsset asset = new DevGameAsset(
                    UUID.randomUUID().toString(),
                    gameId,
                    assetType,
                    fileName,
                    storagePath,
                    file.getSize(),
                    file.getContentType(),
                    LocalDateTime.now()
            );
            devGameAssetRepository.insert(asset);

            return "/assets/" + safeUserId + "/" + gameId + "/" + fileName;

        } catch (IOException e) {
            throw new RuntimeException("Failed to save asset: " + assetType, e);
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
            String gameId,
            String name,
            String description,
            String releaseDate,
            String date, MultipartFile image,
            MultipartFile video,
            MultipartFile zip
    ) {
        DevGame game = devGameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("Game not found: " + gameId));

        DeveloperProfile profile = developerProfileRepository
                .findById(game.getDeveloperProfileId())
                .orElseThrow(() -> new IllegalArgumentException("Developer profile not found for this game"));

        String userId = profile.getUserId();

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
            imageUrl = saveAsset(userId, game.getName(), gameId, image, "image");
        }
        if (video != null && !video.isEmpty()) {
            videoUrl = saveAsset(userId, game.getName(), gameId, video, "video");
        }
        if (zip != null && !zip.isEmpty()) {
            zipUrl = saveAsset(userId, game.getName(), gameId, zip, "zip");
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
