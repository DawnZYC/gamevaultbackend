package com.sg.nusiss.gamevaultbackend.controller.developer;

import com.sg.nusiss.gamevaultbackend.entity.developer.DevGameAsset;
import com.sg.nusiss.gamevaultbackend.repository.developer.DevGameAssetRepository;
import com.sg.nusiss.gamevaultbackend.service.developer.DevGameStatisticsApplicationService;
import com.sg.nusiss.gamevaultbackend.controller.common.AuthenticatedControllerBase;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;

@RestController
@RequestMapping("/api/developer/devgameasset")
@RequiredArgsConstructor
public class DevGameAssetDownloadController extends AuthenticatedControllerBase {

    private final DevGameAssetRepository devGameAssetRepository;
    private final DevGameStatisticsApplicationService devGameStatisticsApplicationService;


    @GetMapping("/download/{assetId}")
    public ResponseEntity<Resource> downloadAsset(@PathVariable String assetId) {
        // 1️⃣ 从数据库查出 asset 信息
        DevGameAsset asset = devGameAssetRepository.findById(assetId)
                .orElseThrow(() -> new IllegalArgumentException("Asset not found"));

        // 2️⃣ 调用统计逻辑（只统计 zip 文件下载）
        if ("zip".equalsIgnoreCase(asset.getAssetType())) {
            devGameStatisticsApplicationService.recordGameDownload(asset.getDevGameId());
        }

        // 2️⃣ 根据文件路径加载文件
        File file = new File(asset.getStoragePath());
        if (!file.exists()) {
            throw new IllegalArgumentException("File not found: " + asset.getStoragePath());
        }

        // 3️⃣ 返回文件流
        Resource resource = new FileSystemResource(file);
        
        // 修复文件名编码问题 - 使用URL编码处理特殊字符
        String encodedFileName = java.net.URLEncoder.encode(asset.getFileName(), java.nio.charset.StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20"); // 将+号替换为%20，符合URL编码标准
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + encodedFileName + "\"")
                .contentType(MediaType.parseMediaType(asset.getMimeType()))
                .body(resource);
    }
}
