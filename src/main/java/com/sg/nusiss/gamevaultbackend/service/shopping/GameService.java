package com.sg.nusiss.gamevaultbackend.service.shopping;

import com.sg.nusiss.gamevaultbackend.entity.shopping.Game;
import com.sg.nusiss.gamevaultbackend.dto.shopping.GameDTO;
import com.sg.nusiss.gamevaultbackend.repository.shopping.GameRepository;
import com.sg.nusiss.gamevaultbackend.repository.library.UnusedGameActivationCodeRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class GameService {

    private final GameRepository repo;
    private final GameActivationCodeService activationCodeService;
    private final UnusedGameActivationCodeRepository unusedRepo;

    /** 默认目标库存量，可在 application.yml 中配置 */
    @Value("${activation.stock.target:30}")
    private int TARGET_STOCK;

    public GameService(GameRepository repo,
                       GameActivationCodeService activationCodeService,
                       UnusedGameActivationCodeRepository unusedRepo) {
        this.repo = repo;
        this.activationCodeService = activationCodeService;
        this.unusedRepo = unusedRepo;
    }

    public List<GameDTO> findAll() {
        return repo.findAll().stream()
                .map(this::convertToDTO)
                .toList();
    }

    public Optional<GameDTO> findById(Long id) {
        return repo.findById(id).map(this::convertToDTO);
    }

    public List<GameDTO> searchByTitle(String q) {
        return repo.findByTitleContainingIgnoreCase(q).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<GameDTO> findByGenre(String genre) {
        return repo.findByGenre(genre).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<GameDTO> findByPlatform(String platform) {
        return repo.findByPlatform(platform).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<GameDTO> findTopDiscountedGames(int limit) {
        return repo.findTopDiscountedGames(limit).stream()
                .map(this::convertToDTO)
                .toList();
    }

    /**
     * 保存游戏并初始化激活码库存。
     * 如果该游戏当前库存 < 目标数量，则自动补足到目标数量。
     */
    @Transactional
    public GameDTO save(Game game) {
        Game saved = repo.save(game);

        long existingCodes = unusedRepo.countByGameId(saved.getGameId());
        if (existingCodes < TARGET_STOCK) {
            activationCodeService.generateInitialCodes(saved.getGameId());
        }

        return convertToDTO(saved);
    }

    // --- DTO 转换 ---
    private GameDTO convertToDTO(Game game) {
        GameDTO dto = new GameDTO();
        dto.setGameId(game.getGameId());
        dto.setTitle(game.getTitle());
        dto.setDeveloper(game.getDeveloper());
        dto.setDescription(game.getDescription());
        dto.setPrice(game.getPrice());
        dto.setDiscountPrice(game.getDiscountPrice());
        dto.setGenre(game.getGenre());
        dto.setPlatform(game.getPlatform());
        dto.setReleaseDate(game.getReleaseDate());
        dto.setIsActive(game.getIsActive());
        dto.setImageUrl(game.getImageUrl());
        return dto;
    }
}
