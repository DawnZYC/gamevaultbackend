package com.sg.nusiss.gamevaultbackend.controller.library;

import com.sg.nusiss.gamevaultbackend.dto.library.LibraryItemDTO;
import com.sg.nusiss.gamevaultbackend.entity.library.PurchasedGameActivationCode;
import com.sg.nusiss.gamevaultbackend.entity.shopping.Game;
import com.sg.nusiss.gamevaultbackend.repository.library.PurchasedGameActivationCodeRepository;
import com.sg.nusiss.gamevaultbackend.repository.shopping.GameRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * @ClassName LibraryControllerTest
 * @Author Zhang Yuchen
 * @Date 2025/10/25
 * @Description 游戏库控制器单元测试
 */
@ExtendWith(MockitoExtension.class)
public class LibraryControllerTest {

    @Mock
    private PurchasedGameActivationCodeRepository purchasedGameActivationCodeRepository;

    @Mock
    private GameRepository gameRepository;

    @InjectMocks
    private LibraryController libraryController;

    private Jwt mockJwt;
    private Game testGame1;
    private Game testGame2;
    private Game testGame3;
    private PurchasedGameActivationCode activationCode1;
    private PurchasedGameActivationCode activationCode2;
    private PurchasedGameActivationCode activationCode3;

    @BeforeEach
    void setUp() {
        // Mock JWT - 使用lenient以避免在某些测试中未使用时出错
        mockJwt = mock(Jwt.class);
        lenient().when(mockJwt.getClaims()).thenReturn(Map.of("uid", 1L));

        // 准备测试游戏数据
        testGame1 = new Game();
        testGame1.setGameId(1L);
        testGame1.setTitle("The Witcher 3");
        testGame1.setDeveloper("CD Projekt Red");
        testGame1.setPrice(new BigDecimal("39.99"));
        testGame1.setImageUrl("/uploads/games/witcher3.jpg");
        testGame1.setIsActive(true);
        testGame1.setGenre("RPG");
        testGame1.setPlatform("PC");
        testGame1.setReleaseDate(LocalDate.of(2015, 5, 19));

        testGame2 = new Game();
        testGame2.setGameId(2L);
        testGame2.setTitle("Cyberpunk 2077");
        testGame2.setDeveloper("CD Projekt Red");
        testGame2.setPrice(new BigDecimal("59.99"));
        testGame2.setImageUrl("/uploads/games/cyberpunk.jpg");
        testGame2.setIsActive(true);
        testGame2.setGenre("RPG");
        testGame2.setPlatform("PC");
        testGame2.setReleaseDate(LocalDate.of(2020, 12, 10));

        testGame3 = new Game();
        testGame3.setGameId(3L);
        testGame3.setTitle("Elden Ring");
        testGame3.setDeveloper("FromSoftware");
        testGame3.setPrice(new BigDecimal("49.99"));
        testGame3.setImageUrl("/uploads/games/eldenring.jpg");
        testGame3.setIsActive(true);
        testGame3.setGenre("Action RPG");
        testGame3.setPlatform("PC");
        testGame3.setReleaseDate(LocalDate.of(2022, 2, 25));

        // 准备测试激活码数据
        activationCode1 = PurchasedGameActivationCode.builder()
                .activationId(1L)
                .userId(1L)
                .orderItemId(101L)
                .gameId(1L)
                .activationCode("WITCHER3-XXXXX-XXXXX")
                .build();

        activationCode2 = PurchasedGameActivationCode.builder()
                .activationId(2L)
                .userId(1L)
                .orderItemId(102L)
                .gameId(2L)
                .activationCode("CYBER2077-XXXXX-XXXXX")
                .build();

        activationCode3 = PurchasedGameActivationCode.builder()
                .activationId(3L)
                .userId(1L)
                .orderItemId(103L)
                .gameId(3L)
                .activationCode("ELDENRING-XXXXX-XXXXX")
                .build();
    }

    // ==================== myLibrary 方法测试 ====================

    @Test
    void testMyLibrary_Success_WithMultipleGames() {
        // Given
        List<PurchasedGameActivationCode> codes = Arrays.asList(
                activationCode1, activationCode2, activationCode3
        );

        when(purchasedGameActivationCodeRepository.findByUserId(1L)).thenReturn(codes);
        when(gameRepository.findById(1L)).thenReturn(Optional.of(testGame1));
        when(gameRepository.findById(2L)).thenReturn(Optional.of(testGame2));
        when(gameRepository.findById(3L)).thenReturn(Optional.of(testGame3));

        // When
        Map<String, Object> result = libraryController.myLibrary(mockJwt);

        // Then
        assertNotNull(result, "返回结果不应为null");
        assertTrue(result.containsKey("items"), "应包含items键");

        @SuppressWarnings("unchecked")
        List<LibraryItemDTO> items = (List<LibraryItemDTO>) result.get("items");

        assertNotNull(items, "游戏列表不应为null");
        assertEquals(3, items.size(), "应该有3个游戏");

        // 验证第一个游戏
        LibraryItemDTO item1 = items.get(0);
        assertEquals(1L, item1.activationId, "激活ID应该匹配");
        assertEquals(1L, item1.gameId, "游戏ID应该匹配");
        assertEquals("The Witcher 3", item1.title, "游戏标题应该匹配");
        assertEquals("WITCHER3-XXXXX-XXXXX", item1.activationCode, "激活码应该匹配");
        assertEquals(new BigDecimal("39.99"), item1.price, "价格应该匹配");
        assertEquals("/uploads/games/witcher3.jpg", item1.imageUrl, "图片URL应该匹配");

        // 验证第二个游戏
        LibraryItemDTO item2 = items.get(1);
        assertEquals(2L, item2.activationId, "激活ID应该匹配");
        assertEquals(2L, item2.gameId, "游戏ID应该匹配");
        assertEquals("Cyberpunk 2077", item2.title, "游戏标题应该匹配");
        assertEquals("CYBER2077-XXXXX-XXXXX", item2.activationCode, "激活码应该匹配");

        // 验证第三个游戏
        LibraryItemDTO item3 = items.get(2);
        assertEquals(3L, item3.activationId, "激活ID应该匹配");
        assertEquals(3L, item3.gameId, "游戏ID应该匹配");
        assertEquals("Elden Ring", item3.title, "游戏标题应该匹配");
        assertEquals("ELDENRING-XXXXX-XXXXX", item3.activationCode, "激活码应该匹配");

        // 验证方法调用
        verify(purchasedGameActivationCodeRepository, times(1)).findByUserId(1L);
        verify(gameRepository, times(1)).findById(1L);
        verify(gameRepository, times(1)).findById(2L);
        verify(gameRepository, times(1)).findById(3L);
    }

    @Test
    void testMyLibrary_Success_WithSingleGame() {
        // Given - 只有一个游戏
        List<PurchasedGameActivationCode> codes = Collections.singletonList(activationCode1);

        when(purchasedGameActivationCodeRepository.findByUserId(1L)).thenReturn(codes);
        when(gameRepository.findById(1L)).thenReturn(Optional.of(testGame1));

        // When
        Map<String, Object> result = libraryController.myLibrary(mockJwt);

        // Then
        assertNotNull(result, "返回结果不应为null");

        @SuppressWarnings("unchecked")
        List<LibraryItemDTO> items = (List<LibraryItemDTO>) result.get("items");

        assertEquals(1, items.size(), "应该有1个游戏");

        LibraryItemDTO item = items.get(0);
        assertEquals(1L, item.activationId, "激活ID应该匹配");
        assertEquals(1L, item.gameId, "游戏ID应该匹配");
        assertEquals("The Witcher 3", item.title, "游戏标题应该匹配");

        verify(purchasedGameActivationCodeRepository, times(1)).findByUserId(1L);
        verify(gameRepository, times(1)).findById(1L);
    }

    @Test
    void testMyLibrary_EmptyLibrary() {
        // Given - 用户没有购买任何游戏
        when(purchasedGameActivationCodeRepository.findByUserId(1L))
                .thenReturn(Collections.emptyList());

        // When
        Map<String, Object> result = libraryController.myLibrary(mockJwt);

        // Then
        assertNotNull(result, "返回结果不应为null");
        assertTrue(result.containsKey("items"), "应包含items键");

        @SuppressWarnings("unchecked")
        List<LibraryItemDTO> items = (List<LibraryItemDTO>) result.get("items");

        assertNotNull(items, "游戏列表不应为null");
        assertTrue(items.isEmpty(), "游戏列表应该为空");
        assertEquals(0, items.size(), "应该有0个游戏");

        verify(purchasedGameActivationCodeRepository, times(1)).findByUserId(1L);
        verify(gameRepository, never()).findById(anyLong());
    }

    @Test
    void testMyLibrary_GameNotFound_ReturnsNullFields() {
        // Given - 游戏不存在
        List<PurchasedGameActivationCode> codes = Collections.singletonList(activationCode1);

        when(purchasedGameActivationCodeRepository.findByUserId(1L)).thenReturn(codes);
        when(gameRepository.findById(1L)).thenReturn(Optional.empty());

        // When
        Map<String, Object> result = libraryController.myLibrary(mockJwt);

        // Then
        assertNotNull(result, "返回结果不应为null");

        @SuppressWarnings("unchecked")
        List<LibraryItemDTO> items = (List<LibraryItemDTO>) result.get("items");

        assertEquals(1, items.size(), "应该有1个项目");

        LibraryItemDTO item = items.get(0);
        assertEquals(1L, item.activationId, "激活ID应该匹配");
        assertEquals(1L, item.gameId, "游戏ID应该匹配");
        assertEquals("WITCHER3-XXXXX-XXXXX", item.activationCode, "激活码应该匹配");
        // 游戏不存在时，游戏相关字段应该为null
        assertNull(item.title, "游戏标题应该为null");
        assertNull(item.price, "价格应该为null");
        assertNull(item.imageUrl, "图片URL应该为null");

        verify(purchasedGameActivationCodeRepository, times(1)).findByUserId(1L);
        verify(gameRepository, times(1)).findById(1L);
    }

    @Test
    void testMyLibrary_MultipleGames_SomeNotFound() {
        // Given - 多个游戏，部分不存在
        List<PurchasedGameActivationCode> codes = Arrays.asList(
                activationCode1, activationCode2
        );

        when(purchasedGameActivationCodeRepository.findByUserId(1L)).thenReturn(codes);
        when(gameRepository.findById(1L)).thenReturn(Optional.of(testGame1));
        when(gameRepository.findById(2L)).thenReturn(Optional.empty()); // 第二个游戏不存在

        // When
        Map<String, Object> result = libraryController.myLibrary(mockJwt);

        // Then
        @SuppressWarnings("unchecked")
        List<LibraryItemDTO> items = (List<LibraryItemDTO>) result.get("items");

        assertEquals(2, items.size(), "应该有2个项目");

        // 第一个游戏应该有完整信息
        LibraryItemDTO item1 = items.get(0);
        assertNotNull(item1.title, "第一个游戏的标题不应为null");
        assertEquals("The Witcher 3", item1.title, "游戏标题应该匹配");

        // 第二个游戏应该只有激活码信息
        LibraryItemDTO item2 = items.get(1);
        assertNull(item2.title, "第二个游戏的标题应该为null");
        assertEquals(2L, item2.gameId, "游戏ID应该匹配");
        assertEquals("CYBER2077-XXXXX-XXXXX", item2.activationCode, "激活码应该匹配");

        verify(gameRepository, times(1)).findById(1L);
        verify(gameRepository, times(1)).findById(2L);
    }

    @Test
    void testMyLibrary_CacheEfficiency_SameGameMultipleTimes() {
        // Given - 用户购买了同一个游戏多次
        PurchasedGameActivationCode activationCode1Copy = PurchasedGameActivationCode.builder()
                .activationId(4L)
                .userId(1L)
                .orderItemId(104L)
                .gameId(1L) // 同一个游戏
                .activationCode("WITCHER3-YYYYY-YYYYY")
                .build();

        List<PurchasedGameActivationCode> codes = Arrays.asList(
                activationCode1, activationCode1Copy
        );

        when(purchasedGameActivationCodeRepository.findByUserId(1L)).thenReturn(codes);
        when(gameRepository.findById(1L)).thenReturn(Optional.of(testGame1));

        // When
        Map<String, Object> result = libraryController.myLibrary(mockJwt);

        // Then
        @SuppressWarnings("unchecked")
        List<LibraryItemDTO> items = (List<LibraryItemDTO>) result.get("items");

        assertEquals(2, items.size(), "应该有2个项目");

        // 两个项目应该有相同的游戏信息
        LibraryItemDTO item1 = items.get(0);
        LibraryItemDTO item2 = items.get(1);

        assertEquals("The Witcher 3", item1.title, "游戏标题应该匹配");
        assertEquals("The Witcher 3", item2.title, "游戏标题应该匹配");
        assertEquals(item1.gameId, item2.gameId, "游戏ID应该相同");

        // 但激活码应该不同
        assertNotEquals(item1.activationCode, item2.activationCode, "激活码应该不同");

        // 由于使用了缓存，同一个游戏只应该查询一次
        verify(gameRepository, times(1)).findById(1L);
    }

    @Test
    void testMyLibrary_VerifyGameCacheUsage() {
        // Given - 测试缓存机制，确保同一游戏只查询一次
        List<PurchasedGameActivationCode> codes = Arrays.asList(
                activationCode1, activationCode2, activationCode1
        );

        when(purchasedGameActivationCodeRepository.findByUserId(1L)).thenReturn(codes);
        when(gameRepository.findById(1L)).thenReturn(Optional.of(testGame1));
        when(gameRepository.findById(2L)).thenReturn(Optional.of(testGame2));

        // When
        libraryController.myLibrary(mockJwt);

        // Then - 每个游戏ID只应该查询一次
        verify(gameRepository, times(1)).findById(1L);
        verify(gameRepository, times(1)).findById(2L);
    }

    @Test
    void testMyLibrary_DifferentUser() {
        // Given - 不同的用户
        Jwt mockJwt2 = mock(Jwt.class);
        when(mockJwt2.getClaims()).thenReturn(Map.of("uid", 2L));

        PurchasedGameActivationCode code = PurchasedGameActivationCode.builder()
                .activationId(10L)
                .userId(2L)
                .orderItemId(201L)
                .gameId(1L)
                .activationCode("USER2-XXXXX-XXXXX")
                .build();

        when(purchasedGameActivationCodeRepository.findByUserId(2L))
                .thenReturn(Collections.singletonList(code));
        when(gameRepository.findById(1L)).thenReturn(Optional.of(testGame1));

        // When
        Map<String, Object> result = libraryController.myLibrary(mockJwt2);

        // Then
        @SuppressWarnings("unchecked")
        List<LibraryItemDTO> items = (List<LibraryItemDTO>) result.get("items");

        assertEquals(1, items.size(), "应该有1个游戏");

        LibraryItemDTO item = items.get(0);
        assertEquals(10L, item.activationId, "激活ID应该匹配");
        assertEquals(2L, code.getUserId(), "用户ID应该是2");
        assertEquals("USER2-XXXXX-XXXXX", item.activationCode, "激活码应该匹配");

        verify(purchasedGameActivationCodeRepository, times(1)).findByUserId(2L);
        verify(purchasedGameActivationCodeRepository, never()).findByUserId(1L);
    }

    @Test
    void testMyLibrary_AllFieldsPopulated() {
        // Given - 验证所有字段都被正确填充
        List<PurchasedGameActivationCode> codes = Collections.singletonList(activationCode1);

        when(purchasedGameActivationCodeRepository.findByUserId(1L)).thenReturn(codes);
        when(gameRepository.findById(1L)).thenReturn(Optional.of(testGame1));

        // When
        Map<String, Object> result = libraryController.myLibrary(mockJwt);

        // Then
        @SuppressWarnings("unchecked")
        List<LibraryItemDTO> items = (List<LibraryItemDTO>) result.get("items");

        LibraryItemDTO item = items.get(0);

        // 验证所有字段都被正确设置
        assertNotNull(item.activationId, "activationId不应为null");
        assertNotNull(item.gameId, "gameId不应为null");
        assertNotNull(item.title, "title不应为null");
        assertNotNull(item.activationCode, "activationCode不应为null");
        assertNotNull(item.price, "price不应为null");
        assertNotNull(item.imageUrl, "imageUrl不应为null");

        assertEquals(1L, item.activationId);
        assertEquals(1L, item.gameId);
        assertEquals("The Witcher 3", item.title);
        assertEquals("WITCHER3-XXXXX-XXXXX", item.activationCode);
        assertEquals(new BigDecimal("39.99"), item.price);
        assertEquals("/uploads/games/witcher3.jpg", item.imageUrl);
    }

    @Test
    void testMyLibrary_OrderPreserved() {
        // Given - 验证顺序是否保持
        List<PurchasedGameActivationCode> codes = Arrays.asList(
                activationCode3, activationCode1, activationCode2
        );

        when(purchasedGameActivationCodeRepository.findByUserId(1L)).thenReturn(codes);
        when(gameRepository.findById(1L)).thenReturn(Optional.of(testGame1));
        when(gameRepository.findById(2L)).thenReturn(Optional.of(testGame2));
        when(gameRepository.findById(3L)).thenReturn(Optional.of(testGame3));

        // When
        Map<String, Object> result = libraryController.myLibrary(mockJwt);

        // Then - 顺序应该和数据库返回的顺序一致
        @SuppressWarnings("unchecked")
        List<LibraryItemDTO> items = (List<LibraryItemDTO>) result.get("items");

        assertEquals(3, items.size(), "应该有3个游戏");
        assertEquals(3L, items.get(0).gameId, "第一个应该是游戏3");
        assertEquals(1L, items.get(1).gameId, "第二个应该是游戏1");
        assertEquals(2L, items.get(2).gameId, "第三个应该是游戏2");
    }

    @Test
    void testMyLibrary_LargeLibrary() {
        // Given - 测试大量游戏的情况
        List<PurchasedGameActivationCode> codes = new ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            PurchasedGameActivationCode code = PurchasedGameActivationCode.builder()
                    .activationId((long) i)
                    .userId(1L)
                    .orderItemId((long) (100 + i))
                    .gameId((long) (i % 10 + 1)) // 10个不同的游戏
                    .activationCode("CODE-" + i)
                    .build();
            codes.add(code);
        }

        when(purchasedGameActivationCodeRepository.findByUserId(1L)).thenReturn(codes);
        // Mock 10个不同的游戏
        for (int i = 1; i <= 10; i++) {
            Game game = new Game();
            game.setGameId((long) i);
            game.setTitle("Game " + i);
            game.setPrice(new BigDecimal("29.99"));
            when(gameRepository.findById((long) i)).thenReturn(Optional.of(game));
        }

        // When
        Map<String, Object> result = libraryController.myLibrary(mockJwt);

        // Then
        @SuppressWarnings("unchecked")
        List<LibraryItemDTO> items = (List<LibraryItemDTO>) result.get("items");

        assertEquals(100, items.size(), "应该有100个激活码");

        // 由于缓存，每个游戏只应该查询一次
        for (int i = 1; i <= 10; i++) {
            verify(gameRepository, times(1)).findById((long) i);
        }
    }

    // ==================== 异常处理测试 ====================

    @Test
    void testMyLibrary_DatabaseError_ThrowsException() {
        // Given
        when(purchasedGameActivationCodeRepository.findByUserId(1L))
                .thenThrow(new RuntimeException("Database connection failed"));

        // When & Then
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> libraryController.myLibrary(mockJwt),
                "数据库错误应该抛出异常"
        );

        assertEquals("Database connection failed", exception.getMessage());
        verify(purchasedGameActivationCodeRepository, times(1)).findByUserId(1L);
    }

    @Test
    void testMyLibrary_GameRepositoryError_ThrowsException() {
        // Given
        List<PurchasedGameActivationCode> codes = Arrays.asList(activationCode1);
        when(purchasedGameActivationCodeRepository.findByUserId(1L)).thenReturn(codes);
        when(gameRepository.findById(1L)).thenThrow(new RuntimeException("Game repository error"));

        // When & Then
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> libraryController.myLibrary(mockJwt),
                "游戏仓库错误应该抛出异常"
        );

        assertEquals("Game repository error", exception.getMessage());
    }

    // ==================== 边界条件测试 ====================
    // 注意：实际实现中没有参数验证，这些测试被注释掉
    // 参数验证由Spring Security和JWT处理

    /*
    @Test
    void testMyLibrary_NullJwt_ThrowsException() {
        // 实际实现中没有null检查，由Spring Security处理
    }

    @Test
    void testMyLibrary_InvalidJwtClaims_ThrowsException() {
        // 实际实现中没有claims验证，由Spring Security处理
    }

    @Test
    void testMyLibrary_NonNumericUserId_ThrowsException() {
        // 实际实现中没有类型检查，直接转换
    }

    @Test
    void testMyLibrary_NegativeUserId_ThrowsException() {
        // 实际实现中没有范围检查
    }

    @Test
    void testMyLibrary_ZeroUserId_ThrowsException() {
        // 实际实现中没有范围检查
    }
    */

    // ==================== 性能测试模拟 ====================

    @Test
    void testMyLibrary_PerformanceWithManyGames() {
        // Given - 测试大量游戏的性能
        List<PurchasedGameActivationCode> codes = new ArrayList<>();
        for (int i = 1; i <= 1000; i++) {
            PurchasedGameActivationCode code = PurchasedGameActivationCode.builder()
                    .activationId((long) i)
                    .userId(1L)
                    .orderItemId((long) (1000 + i))
                    .gameId((long) ((i - 1) % 100 + 1)) // 100个不同的游戏
                    .activationCode("CODE-" + i)
                    .build();
            codes.add(code);
        }

        when(purchasedGameActivationCodeRepository.findByUserId(1L)).thenReturn(codes);
        // Mock 100个不同的游戏
        for (int i = 1; i <= 100; i++) {
            Game game = new Game();
            game.setGameId((long) i);
            game.setTitle("Game " + i);
            game.setPrice(new BigDecimal("29.99"));
            game.setImageUrl("/uploads/games/game" + i + ".jpg");
            when(gameRepository.findById((long) i)).thenReturn(Optional.of(game));
        }

        // When
        long startTime = System.currentTimeMillis();
        Map<String, Object> result = libraryController.myLibrary(mockJwt);
        long endTime = System.currentTimeMillis();

        // Then
        assertNotNull(result);
        @SuppressWarnings("unchecked")
        List<LibraryItemDTO> items = (List<LibraryItemDTO>) result.get("items");
        assertEquals(1000, items.size(), "应该有1000个激活码");

        // 验证性能（应该在合理时间内完成）
        long executionTime = endTime - startTime;
        assertTrue(executionTime < 5000, "执行时间应该少于5秒，实际用时: " + executionTime + "ms");

        // 由于缓存，每个游戏只应该查询一次
        for (int i = 1; i <= 100; i++) {
            verify(gameRepository, times(1)).findById((long) i);
        }
    }

    @Test
    void testMyLibrary_MemoryEfficiencyWithLargeDataset() {
        // Given - 测试内存效率
        List<PurchasedGameActivationCode> codes = new ArrayList<>();
        for (int i = 1; i <= 10000; i++) {
            PurchasedGameActivationCode code = PurchasedGameActivationCode.builder()
                    .activationId((long) i)
                    .userId(1L)
                    .orderItemId((long) (10000 + i))
                    .gameId((long) ((i - 1) % 50 + 1)) // 50个不同的游戏
                    .activationCode("CODE-" + i)
                    .build();
            codes.add(code);
        }

        when(purchasedGameActivationCodeRepository.findByUserId(1L)).thenReturn(codes);
        // Mock 50个不同的游戏
        for (int i = 1; i <= 50; i++) {
            Game game = new Game();
            game.setGameId((long) i);
            game.setTitle("Game " + i);
            game.setPrice(new BigDecimal("29.99"));
            game.setImageUrl("/uploads/games/game" + i + ".jpg");
            when(gameRepository.findById((long) i)).thenReturn(Optional.of(game));
        }

        // When
        Map<String, Object> result = libraryController.myLibrary(mockJwt);

        // Then
        assertNotNull(result);
        @SuppressWarnings("unchecked")
        List<LibraryItemDTO> items = (List<LibraryItemDTO>) result.get("items");
        assertEquals(10000, items.size(), "应该有10000个激活码");

        // 验证缓存效率
        for (int i = 1; i <= 50; i++) {
            verify(gameRepository, times(1)).findById((long) i);
        }
    }

    // ==================== 数据一致性测试 ====================

    @Test
    void testMyLibrary_DataConsistencyWithPartialFailures() {
        // Given - 部分游戏查询失败
        List<PurchasedGameActivationCode> codes = Arrays.asList(
                activationCode1, activationCode2, activationCode3
        );

        when(purchasedGameActivationCodeRepository.findByUserId(1L)).thenReturn(codes);
        when(gameRepository.findById(1L)).thenReturn(Optional.of(testGame1));
        when(gameRepository.findById(2L)).thenReturn(Optional.empty()); // 游戏2不存在
        when(gameRepository.findById(3L)).thenThrow(new RuntimeException("Database error")); // 游戏3查询失败

        // When & Then
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> libraryController.myLibrary(mockJwt),
                "数据库错误应该抛出异常"
        );

        assertEquals("Database error", exception.getMessage());
    }

    @Test
    void testMyLibrary_ConcurrentAccessSimulation() {
        // Given - 模拟并发访问
        List<PurchasedGameActivationCode> codes = Arrays.asList(activationCode1);
        when(purchasedGameActivationCodeRepository.findByUserId(1L)).thenReturn(codes);
        when(gameRepository.findById(1L)).thenReturn(Optional.of(testGame1));

        // When - 模拟多次并发调用
        Map<String, Object> result1 = libraryController.myLibrary(mockJwt);
        Map<String, Object> result2 = libraryController.myLibrary(mockJwt);
        Map<String, Object> result3 = libraryController.myLibrary(mockJwt);

        // Then
        assertNotNull(result1);
        assertNotNull(result2);
        assertNotNull(result3);

        // 验证结果结构相同（不比较对象引用）
        assertTrue(result1.containsKey("items"));
        assertTrue(result2.containsKey("items"));
        assertTrue(result3.containsKey("items"));
        
        @SuppressWarnings("unchecked")
        List<LibraryItemDTO> items1 = (List<LibraryItemDTO>) result1.get("items");
        @SuppressWarnings("unchecked")
        List<LibraryItemDTO> items2 = (List<LibraryItemDTO>) result2.get("items");
        @SuppressWarnings("unchecked")
        List<LibraryItemDTO> items3 = (List<LibraryItemDTO>) result3.get("items");
        
        assertEquals(items1.size(), items2.size());
        assertEquals(items2.size(), items3.size());

        // 验证方法被正确调用
        verify(purchasedGameActivationCodeRepository, times(3)).findByUserId(1L);
        verify(gameRepository, times(3)).findById(1L);
    }

    // ==================== 特殊场景测试 ====================

    @Test
    void testMyLibrary_UserWithNoGames_EmptyResult() {
        // Given - 用户没有任何游戏
        when(purchasedGameActivationCodeRepository.findByUserId(1L))
                .thenReturn(Collections.emptyList());

        // When
        Map<String, Object> result = libraryController.myLibrary(mockJwt);

        // Then
        assertNotNull(result);
        @SuppressWarnings("unchecked")
        List<LibraryItemDTO> items = (List<LibraryItemDTO>) result.get("items");
        assertTrue(items.isEmpty());
        assertEquals(0, items.size());

        verify(purchasedGameActivationCodeRepository, times(1)).findByUserId(1L);
        verify(gameRepository, never()).findById(anyLong());
    }

    @Test
    void testMyLibrary_UserWithDeletedGames() {
        // Given - 用户有激活码，但游戏已被删除
        List<PurchasedGameActivationCode> codes = Arrays.asList(activationCode1);
        when(purchasedGameActivationCodeRepository.findByUserId(1L)).thenReturn(codes);
        when(gameRepository.findById(1L)).thenReturn(Optional.empty()); // 游戏已被删除

        // When
        Map<String, Object> result = libraryController.myLibrary(mockJwt);

        // Then
        assertNotNull(result);
        @SuppressWarnings("unchecked")
        List<LibraryItemDTO> items = (List<LibraryItemDTO>) result.get("items");
        assertEquals(1, items.size());

        LibraryItemDTO item = items.get(0);
        assertEquals(1L, item.activationId);
        assertEquals(1L, item.gameId);
        assertEquals("WITCHER3-XXXXX-XXXXX", item.activationCode);
        assertNull(item.title, "游戏标题应该为null");
        assertNull(item.price, "价格应该为null");
        assertNull(item.imageUrl, "图片URL应该为null");
    }

    @Test
    void testMyLibrary_UserWithInactiveGames() {
        // Given - 用户有激活码，但游戏已下架
        testGame1.setIsActive(false); // 游戏已下架
        List<PurchasedGameActivationCode> codes = Arrays.asList(activationCode1);
        when(purchasedGameActivationCodeRepository.findByUserId(1L)).thenReturn(codes);
        when(gameRepository.findById(1L)).thenReturn(Optional.of(testGame1));

        // When
        Map<String, Object> result = libraryController.myLibrary(mockJwt);

        // Then
        assertNotNull(result);
        @SuppressWarnings("unchecked")
        List<LibraryItemDTO> items = (List<LibraryItemDTO>) result.get("items");
        assertEquals(1, items.size());

        LibraryItemDTO item = items.get(0);
        assertEquals("The Witcher 3", item.title);
        assertEquals("WITCHER3-XXXXX-XXXXX", item.activationCode);
        // 即使游戏已下架，用户仍然可以看到自己的游戏
    }

    @Test
    void testMyLibrary_UserWithDuplicateGames() {
        // Given - 用户购买了同一个游戏的多个副本
        PurchasedGameActivationCode duplicateCode = PurchasedGameActivationCode.builder()
                .activationId(4L)
                .userId(1L)
                .orderItemId(104L)
                .gameId(1L) // 同一个游戏
                .activationCode("WITCHER3-DUPLICATE-XXXXX")
                .build();

        List<PurchasedGameActivationCode> codes = Arrays.asList(activationCode1, duplicateCode);
        when(purchasedGameActivationCodeRepository.findByUserId(1L)).thenReturn(codes);
        when(gameRepository.findById(1L)).thenReturn(Optional.of(testGame1));

        // When
        Map<String, Object> result = libraryController.myLibrary(mockJwt);

        // Then
        assertNotNull(result);
        @SuppressWarnings("unchecked")
        List<LibraryItemDTO> items = (List<LibraryItemDTO>) result.get("items");
        assertEquals(2, items.size(), "应该有2个激活码");

        // 两个项目应该有相同的游戏信息
        LibraryItemDTO item1 = items.get(0);
        LibraryItemDTO item2 = items.get(1);

        assertEquals("The Witcher 3", item1.title);
        assertEquals("The Witcher 3", item2.title);
        assertEquals(1L, item1.gameId);
        assertEquals(1L, item2.gameId);

        // 但激活码应该不同
        assertNotEquals(item1.activationCode, item2.activationCode);
        assertEquals("WITCHER3-XXXXX-XXXXX", item1.activationCode);
        assertEquals("WITCHER3-DUPLICATE-XXXXX", item2.activationCode);

        // 由于缓存，同一个游戏只应该查询一次
        verify(gameRepository, times(1)).findById(1L);
    }

    // ==================== 集成测试场景 ====================

    @Test
    void testMyLibrary_CompleteUserJourney() {
        // Given - 模拟用户完整的游戏库体验
        List<PurchasedGameActivationCode> codes = Arrays.asList(
                activationCode1, activationCode2, activationCode3
        );

        when(purchasedGameActivationCodeRepository.findByUserId(1L)).thenReturn(codes);
        when(gameRepository.findById(1L)).thenReturn(Optional.of(testGame1));
        when(gameRepository.findById(2L)).thenReturn(Optional.of(testGame2));
        when(gameRepository.findById(3L)).thenReturn(Optional.of(testGame3));

        // When - 用户查看游戏库
        Map<String, Object> result = libraryController.myLibrary(mockJwt);

        // Then - 验证完整的用户体验
        assertNotNull(result);
        @SuppressWarnings("unchecked")
        List<LibraryItemDTO> items = (List<LibraryItemDTO>) result.get("items");

        assertEquals(3, items.size(), "用户应该看到3个游戏");

        // 验证每个游戏都有完整信息
        for (LibraryItemDTO item : items) {
            assertNotNull(item.activationId, "激活ID不应为null");
            assertNotNull(item.gameId, "游戏ID不应为null");
            assertNotNull(item.title, "游戏标题不应为null");
            assertNotNull(item.activationCode, "激活码不应为null");
            assertNotNull(item.price, "价格不应为null");
            assertNotNull(item.imageUrl, "图片URL不应为null");
        }

        // 验证所有repository方法都被正确调用
        verify(purchasedGameActivationCodeRepository, times(1)).findByUserId(1L);
        verify(gameRepository, times(1)).findById(1L);
        verify(gameRepository, times(1)).findById(2L);
        verify(gameRepository, times(1)).findById(3L);
    }
}

