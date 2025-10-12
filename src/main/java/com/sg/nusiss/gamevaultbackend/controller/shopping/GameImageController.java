package com.sg.nusiss.gamevaultbackend.controller.shopping;

import com.sg.nusiss.gamevaultbackend.service.FileUploadService;
import com.sg.nusiss.gamevaultbackend.service.shopping.GameService;
import com.sg.nusiss.gamevaultbackend.dto.shopping.GameDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 游戏图片上传控制器
 * 提供游戏图片的上传、删除功能
 */
@RestController
@RequestMapping("/api/games")
@CrossOrigin(origins = "*")
public class GameImageController {

    private final FileUploadService fileUploadService;
    private final GameService gameService;

    public GameImageController(FileUploadService fileUploadService, GameService gameService) {
        this.fileUploadService = fileUploadService;
        this.gameService = gameService;
    }

    /**
     * 上传游戏图片
     * @param gameId 游戏ID
     * @param file 图片文件
     * @return 上传结果
     */
    @PostMapping("/{gameId}/image")
    public ResponseEntity<Map<String, Object>> uploadGameImage(
            @PathVariable Long gameId,
            @RequestParam("file") MultipartFile file) {
        
        try {
            // 检查游戏是否存在
            if (!gameService.findById(gameId).isPresent()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found");
            }

            // 上传图片
            String imageUrl = fileUploadService.uploadGameImage(file, gameId);
            
            // 更新游戏信息
            GameDTO gameUpdate = new GameDTO();
            gameUpdate.setImageUrl(imageUrl);
            gameService.updateGame(gameId, gameUpdate);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("imageUrl", imageUrl);
            response.put("message", "图片上传成功");
            
            return ResponseEntity.ok(response);
            
        } catch (IOException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "图片上传失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    /**
     * 删除游戏图片
     * @param gameId 游戏ID
     * @return 删除结果
     */
    @DeleteMapping("/{gameId}/image")
    public ResponseEntity<Map<String, Object>> deleteGameImage(@PathVariable Long gameId) {
        try {
            // 检查游戏是否存在
            GameDTO game = gameService.findById(gameId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found"));

            // 删除图片文件
            boolean deleted = fileUploadService.deleteGameImage(game.getImageUrl());
            
            Map<String, Object> response = new HashMap<>();
            if (deleted) {
                response.put("success", true);
                response.put("message", "图片删除成功");
            } else {
                response.put("success", false);
                response.put("message", "图片删除失败");
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "删除图片时发生错误: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
