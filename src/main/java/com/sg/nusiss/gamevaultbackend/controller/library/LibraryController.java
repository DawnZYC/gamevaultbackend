package com.sg.nusiss.gamevaultbackend.controller.library;

import com.sg.nusiss.gamevaultbackend.dto.library.LibraryItemDTO;
import com.sg.nusiss.gamevaultbackend.entity.library.PurchasedGameActivationCode;
import com.sg.nusiss.gamevaultbackend.entity.shopping.Game;
import com.sg.nusiss.gamevaultbackend.repository.library.PurchasedGameActivationCodeRepository;
import com.sg.nusiss.gamevaultbackend.repository.shopping.GameRepository;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class LibraryController {
    private final PurchasedGameActivationCodeRepository pacRepo;
    private final GameRepository gameRepo;

    public LibraryController(PurchasedGameActivationCodeRepository pacRepo, GameRepository gameRepo) {
        this.pacRepo = pacRepo; this.gameRepo = gameRepo;
    }

    @GetMapping("/library")
    public Map<String, Object> myLibrary(@AuthenticationPrincipal Jwt jwt) {
        Long uid = ((Number) jwt.getClaims().get("uid")).longValue();
        List<PurchasedGameActivationCode> codes = pacRepo.findByUserId(uid);
        Map<Long, Game> gameCache = new HashMap<>();
        List<LibraryItemDTO> items = codes.stream().map(c -> {
            Game g = gameCache.computeIfAbsent(c.getGameId(), id -> gameRepo.findById(id).orElse(null));
            LibraryItemDTO dto = new LibraryItemDTO();
            dto.activationId = c.getActivationId();
            dto.gameId = c.getGameId();
            dto.activationCode = c.getActivationCode();
            if (g != null) { 
                dto.title = g.getTitle(); 
                dto.price = g.getPrice(); 
                dto.imageUrl = g.getImageUrl();
            }
            return dto;
        }).collect(Collectors.toList());
        return Map.of("items", items);
    }
}




