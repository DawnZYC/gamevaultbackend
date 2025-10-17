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
 * @Description 游戏库控制器单元测试 - 测试用户游戏库功能
 * @Date 2025/10/17
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
}

