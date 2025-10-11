package com.sg.nusiss.gamevaultbackend.controller.shopping;

import com.sg.nusiss.gamevaultbackend.entity.library.PurchasedGameActivationCode;
import com.sg.nusiss.gamevaultbackend.repository.library.PurchasedGameActivationCodeRepository;
import com.sg.nusiss.gamevaultbackend.service.shopping.GameActivationCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ActivationCodeController {

    private final GameActivationCodeService activationService;
    private final PurchasedGameActivationCodeRepository purchasedRepo;

    // ---------------------------- 管理员功能 ----------------------------

    /** 查看指定游戏的激活码库存情况 */
    @GetMapping("/admin/games/{gameId}/activation-codes/stats")
    public ResponseEntity<Map<String, Long>> getStockStats(@PathVariable Long gameId) {
        return ResponseEntity.ok(activationService.getStockStats(gameId));
    }

    /** 手动补充激活码（补足到目标库存） */
    @PostMapping("/admin/games/{gameId}/activation-codes/replenish")
    public ResponseEntity<String> replenish(@PathVariable Long gameId) {
        activationService.replenishToTarget(gameId);
        return ResponseEntity.ok("已补足激活码库存至目标数量");
    }

    /** 初始化激活码（新游戏上架后） */
    @PostMapping("/admin/games/{gameId}/activation-codes/init")
    public ResponseEntity<String> initCodes(@PathVariable Long gameId) {
        activationService.generateInitialCodes(gameId);
        return ResponseEntity.ok("已初始化激活码库存至目标数量");
    }

    // ---------------------------- 用户功能 ----------------------------

    /** 🧾 查询当前用户所有已购激活码 */
    @GetMapping("/user/activation-codes")
    public ResponseEntity<List<PurchasedGameActivationCode>> getUserCodes(@AuthenticationPrincipal Jwt jwt) {
        Long userId = ((Number) jwt.getClaims().get("uid")).longValue();
        return ResponseEntity.ok(purchasedRepo.findByUserId(userId));
    }

    /** 🎮 查询当前用户指定游戏的激活码 */
    @GetMapping("/user/activation-codes/{gameId}")
    public ResponseEntity<List<PurchasedGameActivationCode>> getUserCodesByGame(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long gameId) {

        Long userId = ((Number) jwt.getClaims().get("uid")).longValue();

        List<PurchasedGameActivationCode> list = purchasedRepo.findByUserId(userId)
                .stream()
                .filter(code -> code.getGameId().equals(gameId))
                .toList();

        return ResponseEntity.ok(list);
    }
}
