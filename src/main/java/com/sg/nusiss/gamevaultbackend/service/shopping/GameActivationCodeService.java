package com.sg.nusiss.gamevaultbackend.service.shopping;

import com.sg.nusiss.gamevaultbackend.entity.library.PurchasedGameActivationCode;
import com.sg.nusiss.gamevaultbackend.entity.library.UnusedGameActivationCode;
import com.sg.nusiss.gamevaultbackend.repository.library.PurchasedGameActivationCodeRepository;
import com.sg.nusiss.gamevaultbackend.repository.library.UnusedGameActivationCodeRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class GameActivationCodeService {

    private final UnusedGameActivationCodeRepository unusedRepo;
    private final PurchasedGameActivationCodeRepository purchasedRepo;

    /** 默认目标库存量，可在 application.yml 中修改 */
    @Value("${activation.stock.target:30}")
    private int TARGET_STOCK;

    public GameActivationCodeService(UnusedGameActivationCodeRepository unusedRepo,
                                     PurchasedGameActivationCodeRepository purchasedRepo) {
        this.unusedRepo = unusedRepo;
        this.purchasedRepo = purchasedRepo;
    }

    /** 游戏上架时生成初始激活码（保证库存为目标值） */
    public void generateInitialCodes(Long gameId) {
        long existing = unusedRepo.countByGameId(gameId);
        if (existing < TARGET_STOCK) {
            generateCodes(gameId, (int) (TARGET_STOCK - existing));
        }
    }

    /** 手动补充激活码到目标库存 */
    public void replenishToTarget(Long gameId) {
        long existing = unusedRepo.countByGameId(gameId);
        if (existing < TARGET_STOCK) {
            int toGenerate = (int) (TARGET_STOCK - existing);
            generateCodes(gameId, toGenerate);
            System.out.printf("Game %d 库存不足(%d)，已补充 %d 条激活码%n",
                    gameId, existing, toGenerate);
        }
    }

    /** 内部生成逻辑 */
    private void generateCodes(Long gameId, int count) {
        List<UnusedGameActivationCode> codes = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            UnusedGameActivationCode code = new UnusedGameActivationCode();
            code.setGameId(gameId);
            code.setActivationCode(UUID.randomUUID().toString());
            codes.add(code);
        }
        unusedRepo.saveAll(codes);
    }

    /** 分配激活码并在交易后补足库存 */
    @Transactional
    public PurchasedGameActivationCode assignCodeToOrderItem(Long userId, Long orderItemId, Long gameId) {
        // step1: 从库存取一条
        UnusedGameActivationCode unused = unusedRepo.findFirstByGameId(gameId)
                .orElseThrow(() -> new IllegalStateException("该游戏没有可用激活码，请联系管理员补充库存"));

        // step2: 记录已购码
        PurchasedGameActivationCode purchased = PurchasedGameActivationCode.of(
                userId, orderItemId, gameId, unused.getActivationCode()
        );
        purchasedRepo.save(purchased);

        // step3: 删除已使用库存码
        unusedRepo.delete(unused);

        // step4: 自动补足库存到目标值
        replenishToTarget(gameId);

        return purchased;
    }

    /** 管理端统计库存情况 */
    public Map<String, Long> getStockStats(Long gameId) {
        long unused = unusedRepo.countByGameId(gameId);
        long purchased = purchasedRepo.findByUserId(gameId).size();
        return Map.of("unused", unused, "purchased", purchased);
    }
}
