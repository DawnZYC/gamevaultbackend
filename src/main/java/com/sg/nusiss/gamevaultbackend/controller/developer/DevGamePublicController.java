package com.sg.nusiss.gamevaultbackend.controller.developer;

import com.sg.nusiss.gamevaultbackend.dto.developer.DevGameListResponse;
import com.sg.nusiss.gamevaultbackend.dto.developer.DevGameResponse;
import com.sg.nusiss.gamevaultbackend.dto.developer.HotGameResponse;
import com.sg.nusiss.gamevaultbackend.service.developer.DevGameQueryApplicationService;
import com.sg.nusiss.gamevaultbackend.service.developer.DevGameStatisticsApplicationService;
import com.sg.nusiss.gamevaultbackend.service.developer.DevGameStatisticsQueryService;
import com.sg.nusiss.gamevaultbackend.controller.common.AuthenticatedControllerBase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/developer/devgame/public")
@RequiredArgsConstructor
public class DevGamePublicController extends AuthenticatedControllerBase {

    private final DevGameQueryApplicationService devGameQueryApplicationService;
    private final DevGameStatisticsApplicationService devGameStatisticsApplicationService;
    private final DevGameStatisticsQueryService devGameStatisticsQueryService;

    /**
     * 🔒 GameHub 公共游戏列表（分页）— 需开发者登录后才能访问
     */
    @GetMapping("/all")
    public ResponseEntity<DevGameListResponse> listAllGames(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int pageSize
    ) {
        String userId = extractUserId(jwt);
        System.out.println("👤 [listAllGames] Access by developer uid = " + userId);

        DevGameListResponse result = devGameQueryApplicationService.listAllGames(page, pageSize);
        return ResponseEntity.ok(result);
    }

    /**
     * 🔒 获取某个游戏详情（统计 + 浏览计数）
     */
    @GetMapping("/{gameId}")
    public ResponseEntity<DevGameResponse> getPublicGameDetail(@AuthenticationPrincipal Jwt jwt,
                                                               @PathVariable String gameId) {
        String userId = extractUserId(jwt);
        System.out.println("👤 [getPublicGameDetail] Access by developer uid = " + userId);

        DevGameResponse game = devGameQueryApplicationService.queryDevGameDetails(gameId);
        if (game != null) {
            devGameStatisticsApplicationService.recordGameView(gameId);
            return ResponseEntity.ok(game);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * 🔒 获取热门游戏榜单
     */
    @GetMapping("/hot")
    public ResponseEntity<List<HotGameResponse>> getHotGames(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "6") int limit) {
        String userId = extractUserId(jwt);
        System.out.println("👤 [getHotGames] Access by developer uid = " + userId);

        List<HotGameResponse> result = devGameStatisticsQueryService.getHotGames(limit);
        return ResponseEntity.ok(result);
    }
}
