package com.sg.nusiss.gamevaultbackend.controller.developer;

import com.sg.nusiss.gamevaultbackend.dto.developer.DevGameResponse;
import com.sg.nusiss.gamevaultbackend.dto.developer.DevGameSummaryResponse;
import com.sg.nusiss.gamevaultbackend.entity.developer.DeveloperProfile;
import com.sg.nusiss.gamevaultbackend.repository.developer.DeveloperProfileRepository;
import com.sg.nusiss.gamevaultbackend.service.developer.DevGameQueryApplicationService;
import com.sg.nusiss.gamevaultbackend.controller.common.AuthenticatedControllerBase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/developer/devgame")
@RequiredArgsConstructor
public class DevGameQueryController extends AuthenticatedControllerBase {

    private final DevGameQueryApplicationService devGameQueryApplicationService;
    private final DeveloperProfileRepository developerProfileRepository;

    @GetMapping("/my")
    public ResponseEntity<List<DevGameSummaryResponse>> getMyGames(@AuthenticationPrincipal Jwt jwt) {
        String userId = extractUserId(jwt);

        DeveloperProfile developerProfile = developerProfileRepository.findByUserId(userId)
                .orElseGet(() -> {
                    DeveloperProfile newProfile = new DeveloperProfile();
                    newProfile.setId(UUID.randomUUID().toString());
                    newProfile.setUserId(userId);
                    newProfile.setProjectCount(0);
                    developerProfileRepository.save(newProfile);
                    return newProfile;
                });

        List<DevGameSummaryResponse> games =
                devGameQueryApplicationService.listDevGamesWithCover(developerProfile.getId());
        return ResponseEntity.ok(games);
    }

    @GetMapping("/{gameId}")
    public ResponseEntity<DevGameResponse> getGame(@AuthenticationPrincipal Jwt jwt,
                                                   @PathVariable String gameId) {
        extractUserId(jwt); // 保证必须登录
        DevGameResponse game = devGameQueryApplicationService.queryDevGameDetails(gameId);
        return game != null
                ? ResponseEntity.ok(game)
                : ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }
}
