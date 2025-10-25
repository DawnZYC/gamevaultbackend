package com.sg.nusiss.gamevaultbackend.service.store;

import com.sg.nusiss.gamevaultbackend.entity.library.PurchasedGameActivationCode;
import com.sg.nusiss.gamevaultbackend.entity.library.UnusedGameActivationCode;
import com.sg.nusiss.gamevaultbackend.repository.library.PurchasedGameActivationCodeRepository;
import com.sg.nusiss.gamevaultbackend.repository.library.UnusedGameActivationCodeRepository;
import com.sg.nusiss.gamevaultbackend.service.shopping.GameActivationCodeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * @ClassName GameActivationCodeServiceTest
 * @Author AI Assistant
 * @Date 2025/01/27
 * @Description GameActivationCodeService单元测试类，覆盖所有方法
 */
@ExtendWith(MockitoExtension.class)
class GameActivationCodeServiceTest {

    @Mock
    private UnusedGameActivationCodeRepository unusedRepo;

    @Mock
    private PurchasedGameActivationCodeRepository purchasedRepo;

    @InjectMocks
    private GameActivationCodeService activationCodeService;

    private UnusedGameActivationCode testUnusedCode;
    private PurchasedGameActivationCode testPurchasedCode;

    @BeforeEach
    void setUp() {
        // 设置TARGET_STOCK值
        ReflectionTestUtils.setField(activationCodeService, "TARGET_STOCK", 30);

        testUnusedCode = new UnusedGameActivationCode();
        testUnusedCode.setActivationId(1L);
        testUnusedCode.setGameId(1L);
        testUnusedCode.setActivationCode("UNUSED-CODE-123");

        testPurchasedCode = new PurchasedGameActivationCode();
        testPurchasedCode.setActivationId(1L);
        testPurchasedCode.setUserId(1L);
        testPurchasedCode.setOrderItemId(1L);
        testPurchasedCode.setGameId(1L);
        testPurchasedCode.setActivationCode("PURCHASED-CODE-123");
    }

    // ==================== generateInitialCodes 方法测试 ====================

    @Test
    void testGenerateInitialCodes_StockBelowTarget_GeneratesCodes() {
        // Given
        Long gameId = 1L;
        when(unusedRepo.countByGameId(gameId)).thenReturn(5L); // 少于目标库存
        when(unusedRepo.saveAll(anyList())).thenReturn(Arrays.asList(testUnusedCode));

        // When
        activationCodeService.generateInitialCodes(gameId);

        // Then
        verify(unusedRepo, times(1)).countByGameId(gameId);
        verify(unusedRepo, times(1)).saveAll(anyList());
    }

    @Test
    void testGenerateInitialCodes_StockAtTarget_NoGeneration() {
        // Given
        Long gameId = 1L;
        when(unusedRepo.countByGameId(gameId)).thenReturn(30L); // 等于目标库存

        // When
        activationCodeService.generateInitialCodes(gameId);

        // Then
        verify(unusedRepo, times(1)).countByGameId(gameId);
        verify(unusedRepo, never()).saveAll(anyList());
    }

    @Test
    void testGenerateInitialCodes_StockAboveTarget_NoGeneration() {
        // Given
        Long gameId = 1L;
        when(unusedRepo.countByGameId(gameId)).thenReturn(35L); // 超过目标库存

        // When
        activationCodeService.generateInitialCodes(gameId);

        // Then
        verify(unusedRepo, times(1)).countByGameId(gameId);
        verify(unusedRepo, never()).saveAll(anyList());
    }

    @Test
    void testGenerateInitialCodes_ZeroStock_GeneratesFullTarget() {
        // Given
        Long gameId = 1L;
        when(unusedRepo.countByGameId(gameId)).thenReturn(0L); // 零库存
        when(unusedRepo.saveAll(anyList())).thenReturn(Arrays.asList());

        // When
        activationCodeService.generateInitialCodes(gameId);

        // Then
        verify(unusedRepo, times(1)).countByGameId(gameId);
        verify(unusedRepo, times(1)).saveAll(anyList());
    }

    @Test
    void testGenerateInitialCodes_PartialStock_GeneratesRemaining() {
        // Given
        Long gameId = 1L;
        when(unusedRepo.countByGameId(gameId)).thenReturn(20L); // 部分库存
        when(unusedRepo.saveAll(anyList())).thenReturn(Arrays.asList());

        // When
        activationCodeService.generateInitialCodes(gameId);

        // Then
        verify(unusedRepo, times(1)).countByGameId(gameId);
        verify(unusedRepo, times(1)).saveAll(anyList());
    }

    // ==================== replenishToTarget 方法测试 ====================

    @Test
    void testReplenishToTarget_StockBelowTarget_Replenishes() {
        // Given
        Long gameId = 1L;
        when(unusedRepo.countByGameId(gameId)).thenReturn(10L); // 少于目标库存
        when(unusedRepo.saveAll(anyList())).thenReturn(Arrays.asList());

        // When
        activationCodeService.replenishToTarget(gameId);

        // Then
        verify(unusedRepo, times(1)).countByGameId(gameId);
        verify(unusedRepo, times(1)).saveAll(anyList());
    }

    @Test
    void testReplenishToTarget_StockAtTarget_NoReplenishment() {
        // Given
        Long gameId = 1L;
        when(unusedRepo.countByGameId(gameId)).thenReturn(30L); // 等于目标库存

        // When
        activationCodeService.replenishToTarget(gameId);

        // Then
        verify(unusedRepo, times(1)).countByGameId(gameId);
        verify(unusedRepo, never()).saveAll(anyList());
    }

    @Test
    void testReplenishToTarget_StockAboveTarget_NoReplenishment() {
        // Given
        Long gameId = 1L;
        when(unusedRepo.countByGameId(gameId)).thenReturn(35L); // 超过目标库存

        // When
        activationCodeService.replenishToTarget(gameId);

        // Then
        verify(unusedRepo, times(1)).countByGameId(gameId);
        verify(unusedRepo, never()).saveAll(anyList());
    }

    @Test
    void testReplenishToTarget_ZeroStock_ReplenishesToTarget() {
        // Given
        Long gameId = 1L;
        when(unusedRepo.countByGameId(gameId)).thenReturn(0L); // 零库存
        when(unusedRepo.saveAll(anyList())).thenReturn(Arrays.asList());

        // When
        activationCodeService.replenishToTarget(gameId);

        // Then
        verify(unusedRepo, times(1)).countByGameId(gameId);
        verify(unusedRepo, times(1)).saveAll(anyList());
    }

    // ==================== assignCodeToOrderItem 方法测试 ====================

    @Test
    void testAssignCodeToOrderItem_Success() {
        // Given
        Long userId = 1L;
        Long orderItemId = 1L;
        Long gameId = 1L;

        when(unusedRepo.findFirstByGameId(gameId)).thenReturn(Optional.of(testUnusedCode));
        when(purchasedRepo.save(any(PurchasedGameActivationCode.class))).thenReturn(testPurchasedCode);
        when(unusedRepo.countByGameId(gameId)).thenReturn(29L); // 分配后库存不足
        when(unusedRepo.saveAll(anyList())).thenReturn(Arrays.asList());

        // When
        PurchasedGameActivationCode result = activationCodeService.assignCodeToOrderItem(userId, orderItemId, gameId);

        // Then
        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertEquals(orderItemId, result.getOrderItemId());
        assertEquals(gameId, result.getGameId());

        verify(unusedRepo, times(1)).findFirstByGameId(gameId);
        verify(purchasedRepo, times(1)).save(any(PurchasedGameActivationCode.class));
        verify(unusedRepo, times(1)).delete(testUnusedCode);
        verify(unusedRepo, times(1)).countByGameId(gameId);
        verify(unusedRepo, times(1)).saveAll(anyList());
    }

    @Test
    void testAssignCodeToOrderItem_NoAvailableCode_ThrowsException() {
        // Given
        Long userId = 1L;
        Long orderItemId = 1L;
        Long gameId = 1L;

        when(unusedRepo.findFirstByGameId(gameId)).thenReturn(Optional.empty());

        // When & Then
        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> activationCodeService.assignCodeToOrderItem(userId, orderItemId, gameId));
        assertTrue(exception.getMessage().contains("该游戏没有可用激活码"));

        verify(unusedRepo, times(1)).findFirstByGameId(gameId);
        verify(purchasedRepo, never()).save(any(PurchasedGameActivationCode.class));
        verify(unusedRepo, never()).delete(any(UnusedGameActivationCode.class));
    }

    @Test
    void testAssignCodeToOrderItem_StockSufficient_NoReplenishment() {
        // Given
        Long userId = 1L;
        Long orderItemId = 1L;
        Long gameId = 1L;

        when(unusedRepo.findFirstByGameId(gameId)).thenReturn(Optional.of(testUnusedCode));
        when(purchasedRepo.save(any(PurchasedGameActivationCode.class))).thenReturn(testPurchasedCode);
        when(unusedRepo.countByGameId(gameId)).thenReturn(30L); // 分配后库存仍然充足

        // When
        PurchasedGameActivationCode result = activationCodeService.assignCodeToOrderItem(userId, orderItemId, gameId);

        // Then
        assertNotNull(result);
        verify(unusedRepo, times(1)).findFirstByGameId(gameId);
        verify(purchasedRepo, times(1)).save(any(PurchasedGameActivationCode.class));
        verify(unusedRepo, times(1)).delete(testUnusedCode);
        verify(unusedRepo, times(1)).countByGameId(gameId);
        verify(unusedRepo, never()).saveAll(anyList()); // 不需要补充库存
    }

    @Test
    void testAssignCodeToOrderItem_VerifyCodeTransfer() {
        // Given
        Long userId = 1L;
        Long orderItemId = 1L;
        Long gameId = 1L;

        when(unusedRepo.findFirstByGameId(gameId)).thenReturn(Optional.of(testUnusedCode));
        when(purchasedRepo.save(any(PurchasedGameActivationCode.class))).thenAnswer(invocation -> {
            PurchasedGameActivationCode code = invocation.getArgument(0);
            code.setActivationId(1L);
            return code;
        });
        when(unusedRepo.countByGameId(gameId)).thenReturn(29L);
        when(unusedRepo.saveAll(anyList())).thenReturn(Arrays.asList());

        // When
        PurchasedGameActivationCode result = activationCodeService.assignCodeToOrderItem(userId, orderItemId, gameId);

        // Then
        assertNotNull(result);
        assertEquals("UNUSED-CODE-123", result.getActivationCode());
        assertEquals(userId, result.getUserId());
        assertEquals(orderItemId, result.getOrderItemId());
        assertEquals(gameId, result.getGameId());
    }

    // ==================== getStockStats 方法测试 ====================

    @Test
    void testGetStockStats_Success() {
        // Given
        Long gameId = 1L;
        when(unusedRepo.countByGameId(gameId)).thenReturn(25L);
        when(purchasedRepo.findByUserId(gameId)).thenReturn(Arrays.asList(testPurchasedCode, testPurchasedCode));

        // When
        Map<String, Long> result = activationCodeService.getStockStats(gameId);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(25L, result.get("unused"));
        assertEquals(2L, result.get("purchased"));

        verify(unusedRepo, times(1)).countByGameId(gameId);
        verify(purchasedRepo, times(1)).findByUserId(gameId);
    }

    @Test
    void testGetStockStats_ZeroStock() {
        // Given
        Long gameId = 1L;
        when(unusedRepo.countByGameId(gameId)).thenReturn(0L);
        when(purchasedRepo.findByUserId(gameId)).thenReturn(Arrays.asList());

        // When
        Map<String, Long> result = activationCodeService.getStockStats(gameId);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(0L, result.get("unused"));
        assertEquals(0L, result.get("purchased"));

        verify(unusedRepo, times(1)).countByGameId(gameId);
        verify(purchasedRepo, times(1)).findByUserId(gameId);
    }

    @Test
    void testGetStockStats_HighStock() {
        // Given
        Long gameId = 1L;
        when(unusedRepo.countByGameId(gameId)).thenReturn(100L);
        when(purchasedRepo.findByUserId(gameId)).thenReturn(Arrays.asList(testPurchasedCode));

        // When
        Map<String, Long> result = activationCodeService.getStockStats(gameId);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(100L, result.get("unused"));
        assertEquals(1L, result.get("purchased"));

        verify(unusedRepo, times(1)).countByGameId(gameId);
        verify(purchasedRepo, times(1)).findByUserId(gameId);
    }

    // ==================== 内部方法测试（通过公共方法间接测试） ====================

    @Test
    void testGenerateCodes_VerifyCodeGeneration() {
        // Given
        Long gameId = 1L;
        when(unusedRepo.countByGameId(gameId)).thenReturn(0L);
        when(unusedRepo.saveAll(anyList())).thenAnswer(invocation -> {
            List<UnusedGameActivationCode> codes = invocation.getArgument(0);
            return codes;
        });

        // When
        activationCodeService.generateInitialCodes(gameId);

        // Then
        verify(unusedRepo, times(1)).saveAll(anyList());
    }

    @Test
    void testGenerateCodes_VerifyCodeUniqueness() {
        // Given
        Long gameId = 1L;
        when(unusedRepo.countByGameId(gameId)).thenReturn(0L);
        when(unusedRepo.saveAll(anyList())).thenAnswer(invocation -> {
            List<UnusedGameActivationCode> codes = invocation.getArgument(0);
            // 验证所有激活码都是唯一的
            Set<String> codeSet = new HashSet<>();
            for (UnusedGameActivationCode code : codes) {
                assertNotNull(code.getActivationCode());
                assertFalse(codeSet.contains(code.getActivationCode()));
                codeSet.add(code.getActivationCode());
            }
            return codes;
        });

        // When
        activationCodeService.generateInitialCodes(gameId);

        // Then
        verify(unusedRepo, times(1)).saveAll(anyList());
    }

    @Test
    void testGenerateCodes_VerifyGameIdAssignment() {
        // Given
        Long gameId = 1L;
        when(unusedRepo.countByGameId(gameId)).thenReturn(0L);
        when(unusedRepo.saveAll(anyList())).thenAnswer(invocation -> {
            List<UnusedGameActivationCode> codes = invocation.getArgument(0);
            // 验证所有激活码都分配了正确的游戏ID
            for (UnusedGameActivationCode code : codes) {
                assertEquals(gameId, code.getGameId());
            }
            return codes;
        });

        // When
        activationCodeService.generateInitialCodes(gameId);

        // Then
        verify(unusedRepo, times(1)).saveAll(anyList());
    }

    // ==================== 集成测试场景 ====================

    @Test
    void testCompleteActivationCodeLifecycle() {
        // 1. 游戏上架时生成初始激活码
        Long gameId = 1L;
        when(unusedRepo.countByGameId(gameId)).thenReturn(0L);
        when(unusedRepo.saveAll(anyList())).thenReturn(Arrays.asList());

        activationCodeService.generateInitialCodes(gameId);
        verify(unusedRepo, times(1)).saveAll(anyList());

        // 2. 用户购买游戏，分配激活码
        when(unusedRepo.findFirstByGameId(gameId)).thenReturn(Optional.of(testUnusedCode));
        when(purchasedRepo.save(any(PurchasedGameActivationCode.class))).thenReturn(testPurchasedCode);
        when(unusedRepo.countByGameId(gameId)).thenReturn(29L);
        when(unusedRepo.saveAll(anyList())).thenReturn(Arrays.asList());

        PurchasedGameActivationCode assignedCode = activationCodeService.assignCodeToOrderItem(1L, 1L, gameId);
        assertNotNull(assignedCode);

        // 3. 检查库存统计
        when(purchasedRepo.findByUserId(gameId)).thenReturn(Arrays.asList(assignedCode));

        Map<String, Long> stats = activationCodeService.getStockStats(gameId);
        assertNotNull(stats);
        assertEquals(29L, stats.get("unused"));
        assertEquals(1L, stats.get("purchased"));

        // 验证所有方法都被调用
        verify(unusedRepo, atLeast(1)).countByGameId(gameId);
        verify(unusedRepo, atLeast(1)).saveAll(anyList());
        verify(unusedRepo, times(1)).findFirstByGameId(gameId);
        verify(purchasedRepo, times(1)).save(any(PurchasedGameActivationCode.class));
        verify(unusedRepo, times(1)).delete(any(UnusedGameActivationCode.class));
    }

    @Test
    void testStockManagementScenarios() {
        Long gameId = 1L;

        // 场景1：库存充足，不需要补充
        when(unusedRepo.countByGameId(gameId)).thenReturn(35L);
        activationCodeService.replenishToTarget(gameId);
        verify(unusedRepo, never()).saveAll(anyList());

        // 场景2：库存不足，需要补充
        when(unusedRepo.countByGameId(gameId)).thenReturn(5L);
        when(unusedRepo.saveAll(anyList())).thenReturn(Arrays.asList());
        activationCodeService.replenishToTarget(gameId);
        verify(unusedRepo, times(1)).saveAll(anyList());

        // 场景3：零库存，需要生成完整目标数量
        when(unusedRepo.countByGameId(gameId)).thenReturn(0L);
        activationCodeService.generateInitialCodes(gameId);
        verify(unusedRepo, times(2)).saveAll(anyList()); // 之前调用过一次
    }

    @Test
    void testMultipleGameStockManagement() {
        Long gameId1 = 1L;
        Long gameId2 = 2L;

        // 为两个不同的游戏管理库存
        when(unusedRepo.countByGameId(gameId1)).thenReturn(10L);
        when(unusedRepo.countByGameId(gameId2)).thenReturn(25L);
        when(unusedRepo.saveAll(anyList())).thenReturn(Arrays.asList());

        activationCodeService.replenishToTarget(gameId1);
        activationCodeService.replenishToTarget(gameId2);

        verify(unusedRepo, times(1)).countByGameId(gameId1);
        verify(unusedRepo, times(1)).countByGameId(gameId2);
        verify(unusedRepo, times(2)).saveAll(anyList());
    }

    @Test
    void testActivationCodeAssignmentWithReplenishment() {
        Long gameId = 1L;
        Long userId = 1L;
        Long orderItemId = 1L;

        // 模拟库存刚好在目标值，分配一个后需要补充
        when(unusedRepo.findFirstByGameId(gameId)).thenReturn(Optional.of(testUnusedCode));
        when(purchasedRepo.save(any(PurchasedGameActivationCode.class))).thenReturn(testPurchasedCode);
        when(unusedRepo.countByGameId(gameId)).thenReturn(29L); // 分配后库存不足
        when(unusedRepo.saveAll(anyList())).thenReturn(Arrays.asList());

        PurchasedGameActivationCode result = activationCodeService.assignCodeToOrderItem(userId, orderItemId, gameId);

        assertNotNull(result);
        verify(unusedRepo, times(1)).findFirstByGameId(gameId);
        verify(purchasedRepo, times(1)).save(any(PurchasedGameActivationCode.class));
        verify(unusedRepo, times(1)).delete(testUnusedCode);
        verify(unusedRepo, times(1)).countByGameId(gameId);
        verify(unusedRepo, times(1)).saveAll(anyList()); // 应该补充库存
    }

    @Test
    void testStockStatsWithMultiplePurchases() {
        Long gameId = 1L;

        // 模拟多次购买
        PurchasedGameActivationCode code1 = new PurchasedGameActivationCode();
        code1.setActivationId(1L);
        code1.setUserId(1L);
        code1.setOrderItemId(1L);
        code1.setGameId(gameId);
        code1.setActivationCode("CODE-1");

        PurchasedGameActivationCode code2 = new PurchasedGameActivationCode();
        code2.setActivationId(2L);
        code2.setUserId(2L);
        code2.setOrderItemId(2L);
        code2.setGameId(gameId);
        code2.setActivationCode("CODE-2");

        when(unusedRepo.countByGameId(gameId)).thenReturn(28L);
        when(purchasedRepo.findByUserId(gameId)).thenReturn(Arrays.asList(code1, code2));

        Map<String, Long> stats = activationCodeService.getStockStats(gameId);

        assertNotNull(stats);
        assertEquals(28L, stats.get("unused"));
        assertEquals(2L, stats.get("purchased"));

        verify(unusedRepo, times(1)).countByGameId(gameId);
        verify(purchasedRepo, times(1)).findByUserId(gameId);
    }
}
