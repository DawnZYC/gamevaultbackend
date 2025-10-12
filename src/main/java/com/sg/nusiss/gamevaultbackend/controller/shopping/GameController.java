package com.sg.nusiss.gamevaultbackend.controller.shopping;

import com.sg.nusiss.gamevaultbackend.dto.shopping.GameDTO;
import com.sg.nusiss.gamevaultbackend.service.shopping.GameService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

/**
 * ✅ 游戏控制器
 * 提供游戏的浏览、搜索、创建（仅管理员可用）
 */
@RestController
@RequestMapping("/api/games")
@CrossOrigin(origins = "*") // ✅ 可选：前端访问时允许跨域
public class GameController {

    private final GameService service;

    public GameController(GameService service) {
        this.service = service;
    }

    /** 🎮 获取游戏列表（支持搜索、按类型、按平台） */
    @GetMapping
    public List<GameDTO> list(@RequestParam Optional<String> genre,
                              @RequestParam Optional<String> platform,
                              @RequestParam(name = "q") Optional<String> q) {
        if (q.isPresent()) return service.searchByTitle(q.get());
        if (genre.isPresent()) return service.findByGenre(genre.get());
        if (platform.isPresent()) return service.findByPlatform(platform.get());
        return service.findAll();
    }

    /** 🔍 获取单个游戏详情 */
    @GetMapping("/{id}")
    public GameDTO get(@PathVariable Long id) {
        return service.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found"));
    }

    /** 🧩 创建新游戏（⚠️ 仅管理员使用） */
    @PostMapping
    public ResponseEntity<GameDTO> create(@RequestBody GameDTO game) {
        GameDTO saved = service.save(game);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .header("Location", "/api/games/" + saved.getGameId())
                .body(saved);
    }

    /** 🔄 更新游戏信息（⚠️ 仅管理员使用） */
    @PutMapping("/{id}")
    public ResponseEntity<GameDTO> update(@PathVariable Long id, @RequestBody GameDTO game) {
        GameDTO updated = service.updateGame(id, game);
        return ResponseEntity.ok(updated);
    }
}
