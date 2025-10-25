package com.sg.nusiss.gamevaultbackend.service.store;

import com.sg.nusiss.gamevaultbackend.dto.shopping.GameDTO;
import com.sg.nusiss.gamevaultbackend.entity.shopping.Game;
import com.sg.nusiss.gamevaultbackend.repository.library.UnusedGameActivationCodeRepository;
import com.sg.nusiss.gamevaultbackend.repository.shopping.GameRepository;
import com.sg.nusiss.gamevaultbackend.service.shopping.GameActivationCodeService;
import com.sg.nusiss.gamevaultbackend.service.shopping.GameService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * @ClassName GameServiceTest
 * @Author WangChang
 * @Date 2025/10/27
 * * @Description GameService单元测试类，覆盖所有方法
 */
@ExtendWith(MockitoExtension.class)
class GameServiceTest {

    @Mock
    private GameRepository gameRepository;

    @Mock
    private GameActivationCodeService activationCodeService;

    @Mock
    private UnusedGameActivationCodeRepository unusedRepo;

    @InjectMocks
    private GameService gameService;

    private Game testGame;
    private GameDTO testGameDTO;

    @BeforeEach
    void setUp() {
        // 设置TARGET_STOCK值
        ReflectionTestUtils.setField(gameService, "TARGET_STOCK", 30);

        testGame = new Game();
        testGame.setGameId(1L);
        testGame.setTitle("测试游戏");
        testGame.setDeveloper("测试开发商");
        testGame.setDescription("测试游戏描述");
        testGame.setPrice(new BigDecimal("99.99"));
        testGame.setDiscountPrice(new BigDecimal("79.99"));
        testGame.setGenre("动作");
        testGame.setPlatform("PC");
        testGame.setReleaseDate(LocalDate.of(2024, 1, 1));
        testGame.setIsActive(true);
        testGame.setImageUrl("http://example.com/image.jpg");

        testGameDTO = new GameDTO();
        testGameDTO.setGameId(1L);
        testGameDTO.setTitle("测试游戏");
        testGameDTO.setDeveloper("测试开发商");
        testGameDTO.setDescription("测试游戏描述");
        testGameDTO.setPrice(new BigDecimal("99.99"));
        testGameDTO.setDiscountPrice(new BigDecimal("79.99"));
        testGameDTO.setGenre("动作");
        testGameDTO.setPlatform("PC");
        testGameDTO.setReleaseDate(LocalDate.of(2024, 1, 1));
        testGameDTO.setIsActive(true);
        testGameDTO.setImageUrl("http://example.com/image.jpg");
    }

    // ==================== findAll 方法测试 ====================

    @Test
    void testFindAll_Success() {
        // Given
        List<Game> games = Arrays.asList(testGame, createGame(2L, "游戏2"));
        when(gameRepository.findAll()).thenReturn(games);

        // When
        List<GameDTO> result = gameService.findAll();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("测试游戏", result.get(0).getTitle());
        assertEquals("游戏2", result.get(1).getTitle());

        verify(gameRepository, times(1)).findAll();
    }

    @Test
    void testFindAll_EmptyList() {
        // Given
        when(gameRepository.findAll()).thenReturn(Arrays.asList());

        // When
        List<GameDTO> result = gameService.findAll();

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(gameRepository, times(1)).findAll();
    }

    // ==================== findById 方法测试 ====================

    @Test
    void testFindById_Success() {
        // Given
        Long gameId = 1L;
        when(gameRepository.findById(gameId)).thenReturn(Optional.of(testGame));

        // When
        Optional<GameDTO> result = gameService.findById(gameId);

        // Then
        assertTrue(result.isPresent());
        assertEquals("测试游戏", result.get().getTitle());
        assertEquals(gameId, result.get().getGameId());

        verify(gameRepository, times(1)).findById(gameId);
    }

    @Test
    void testFindById_NotFound() {
        // Given
        Long gameId = 999L;
        when(gameRepository.findById(gameId)).thenReturn(Optional.empty());

        // When
        Optional<GameDTO> result = gameService.findById(gameId);

        // Then
        assertFalse(result.isPresent());

        verify(gameRepository, times(1)).findById(gameId);
    }

    @Test
    void testFindById_NullId() {
        // Given
        Long gameId = null;
        when(gameRepository.findById(gameId)).thenReturn(Optional.empty());

        // When
        Optional<GameDTO> result = gameService.findById(gameId);

        // Then
        assertFalse(result.isPresent());

        verify(gameRepository, times(1)).findById(gameId);
    }

    // ==================== searchByTitle 方法测试 ====================

    @Test
    void testSearchByTitle_Success() {
        // Given
        String query = "测试";
        List<Game> games = Arrays.asList(testGame);
        when(gameRepository.findByTitleContainingIgnoreCase(query)).thenReturn(games);

        // When
        List<GameDTO> result = gameService.searchByTitle(query);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("测试游戏", result.get(0).getTitle());

        verify(gameRepository, times(1)).findByTitleContainingIgnoreCase(query);
    }

    @Test
    void testSearchByTitle_NoResults() {
        // Given
        String query = "不存在的游戏";
        when(gameRepository.findByTitleContainingIgnoreCase(query)).thenReturn(Arrays.asList());

        // When
        List<GameDTO> result = gameService.searchByTitle(query);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(gameRepository, times(1)).findByTitleContainingIgnoreCase(query);
    }

    @Test
    void testSearchByTitle_EmptyQuery() {
        // Given
        String query = "";
        when(gameRepository.findByTitleContainingIgnoreCase(query)).thenReturn(Arrays.asList());

        // When
        List<GameDTO> result = gameService.searchByTitle(query);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(gameRepository, times(1)).findByTitleContainingIgnoreCase(query);
    }

    @Test
    void testSearchByTitle_NullQuery() {
        // Given
        String query = null;
        when(gameRepository.findByTitleContainingIgnoreCase(query)).thenReturn(Arrays.asList());

        // When
        List<GameDTO> result = gameService.searchByTitle(query);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(gameRepository, times(1)).findByTitleContainingIgnoreCase(query);
    }

    @Test
    void testSearchByTitle_CaseInsensitive() {
        // Given
        String query = "TEST";
        List<Game> games = Arrays.asList(testGame);
        when(gameRepository.findByTitleContainingIgnoreCase(query)).thenReturn(games);

        // When
        List<GameDTO> result = gameService.searchByTitle(query);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());

        verify(gameRepository, times(1)).findByTitleContainingIgnoreCase(query);
    }

    // ==================== findByGenre 方法测试 ====================

    @Test
    void testFindByGenre_Success() {
        // Given
        String genre = "动作";
        List<Game> games = Arrays.asList(testGame);
        when(gameRepository.findByGenre(genre)).thenReturn(games);

        // When
        List<GameDTO> result = gameService.findByGenre(genre);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("动作", result.get(0).getGenre());

        verify(gameRepository, times(1)).findByGenre(genre);
    }

    @Test
    void testFindByGenre_NoResults() {
        // Given
        String genre = "不存在的类型";
        when(gameRepository.findByGenre(genre)).thenReturn(Arrays.asList());

        // When
        List<GameDTO> result = gameService.findByGenre(genre);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(gameRepository, times(1)).findByGenre(genre);
    }

    @Test
    void testFindByGenre_EmptyGenre() {
        // Given
        String genre = "";
        when(gameRepository.findByGenre(genre)).thenReturn(Arrays.asList());

        // When
        List<GameDTO> result = gameService.findByGenre(genre);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(gameRepository, times(1)).findByGenre(genre);
    }

    @Test
    void testFindByGenre_NullGenre() {
        // Given
        String genre = null;
        when(gameRepository.findByGenre(genre)).thenReturn(Arrays.asList());

        // When
        List<GameDTO> result = gameService.findByGenre(genre);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(gameRepository, times(1)).findByGenre(genre);
    }

    // ==================== findByPlatform 方法测试 ====================

    @Test
    void testFindByPlatform_Success() {
        // Given
        String platform = "PC";
        List<Game> games = Arrays.asList(testGame);
        when(gameRepository.findByPlatform(platform)).thenReturn(games);

        // When
        List<GameDTO> result = gameService.findByPlatform(platform);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("PC", result.get(0).getPlatform());

        verify(gameRepository, times(1)).findByPlatform(platform);
    }

    @Test
    void testFindByPlatform_NoResults() {
        // Given
        String platform = "不存在的平台";
        when(gameRepository.findByPlatform(platform)).thenReturn(Arrays.asList());

        // When
        List<GameDTO> result = gameService.findByPlatform(platform);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(gameRepository, times(1)).findByPlatform(platform);
    }

    @Test
    void testFindByPlatform_EmptyPlatform() {
        // Given
        String platform = "";
        when(gameRepository.findByPlatform(platform)).thenReturn(Arrays.asList());

        // When
        List<GameDTO> result = gameService.findByPlatform(platform);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(gameRepository, times(1)).findByPlatform(platform);
    }

    @Test
    void testFindByPlatform_NullPlatform() {
        // Given
        String platform = null;
        when(gameRepository.findByPlatform(platform)).thenReturn(Arrays.asList());

        // When
        List<GameDTO> result = gameService.findByPlatform(platform);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(gameRepository, times(1)).findByPlatform(platform);
    }

    // ==================== findTopDiscountedGames 方法测试 ====================

    @Test
    void testFindTopDiscountedGames_Success() {
        // Given
        int limit = 5;
        List<Game> games = Arrays.asList(testGame, createGame(2L, "游戏2"));
        when(gameRepository.findTopDiscountedGames(limit)).thenReturn(games);

        // When
        List<GameDTO> result = gameService.findTopDiscountedGames(limit);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());

        verify(gameRepository, times(1)).findTopDiscountedGames(limit);
    }

    @Test
    void testFindTopDiscountedGames_NoResults() {
        // Given
        int limit = 5;
        when(gameRepository.findTopDiscountedGames(limit)).thenReturn(Arrays.asList());

        // When
        List<GameDTO> result = gameService.findTopDiscountedGames(limit);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(gameRepository, times(1)).findTopDiscountedGames(limit);
    }

    @Test
    void testFindTopDiscountedGames_ZeroLimit() {
        // Given
        int limit = 0;
        when(gameRepository.findTopDiscountedGames(limit)).thenReturn(Arrays.asList());

        // When
        List<GameDTO> result = gameService.findTopDiscountedGames(limit);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(gameRepository, times(1)).findTopDiscountedGames(limit);
    }

    @Test
    void testFindTopDiscountedGames_NegativeLimit() {
        // Given
        int limit = -1;
        when(gameRepository.findTopDiscountedGames(limit)).thenReturn(Arrays.asList());

        // When
        List<GameDTO> result = gameService.findTopDiscountedGames(limit);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(gameRepository, times(1)).findTopDiscountedGames(limit);
    }

    // ==================== save(Game) 方法测试 ====================

    @Test
    void testSaveGame_Success_WithStockGeneration() {
        // Given
        when(gameRepository.save(testGame)).thenReturn(testGame);
        when(unusedRepo.countByGameId(testGame.getGameId())).thenReturn(5L); // 少于目标库存
        doNothing().when(activationCodeService).generateInitialCodes(testGame.getGameId());

        // When
        GameDTO result = gameService.save(testGame);

        // Then
        assertNotNull(result);
        assertEquals("测试游戏", result.getTitle());
        assertEquals(testGame.getGameId(), result.getGameId());

        verify(gameRepository, times(1)).save(testGame);
        verify(unusedRepo, times(1)).countByGameId(testGame.getGameId());
        verify(activationCodeService, times(1)).generateInitialCodes(testGame.getGameId());
    }

    @Test
    void testSaveGame_Success_WithoutStockGeneration() {
        // Given
        when(gameRepository.save(testGame)).thenReturn(testGame);
        when(unusedRepo.countByGameId(testGame.getGameId())).thenReturn(35L); // 超过目标库存

        // When
        GameDTO result = gameService.save(testGame);

        // Then
        assertNotNull(result);
        assertEquals("测试游戏", result.getTitle());

        verify(gameRepository, times(1)).save(testGame);
        verify(unusedRepo, times(1)).countByGameId(testGame.getGameId());
        verify(activationCodeService, never()).generateInitialCodes(anyLong());
    }

    @Test
    void testSaveGame_WithNullFields() {
        // Given
        Game gameWithNulls = new Game();
        gameWithNulls.setGameId(2L);
        gameWithNulls.setTitle("部分信息游戏");
        // 其他字段为null

        when(gameRepository.save(gameWithNulls)).thenReturn(gameWithNulls);
        when(unusedRepo.countByGameId(gameWithNulls.getGameId())).thenReturn(0L);
        doNothing().when(activationCodeService).generateInitialCodes(gameWithNulls.getGameId());

        // When
        GameDTO result = gameService.save(gameWithNulls);

        // Then
        assertNotNull(result);
        assertEquals("部分信息游戏", result.getTitle());
        assertNull(result.getDeveloper());
        assertNull(result.getDescription());

        verify(gameRepository, times(1)).save(gameWithNulls);
    }

    // ==================== save(GameDTO) 方法测试 ====================

    @Test
    void testSaveGameDTO_Success() {
        // Given
        when(gameRepository.save(any(Game.class))).thenReturn(testGame);
        when(unusedRepo.countByGameId(testGame.getGameId())).thenReturn(5L);
        doNothing().when(activationCodeService).generateInitialCodes(testGame.getGameId());

        // When
        GameDTO result = gameService.save(testGameDTO);

        // Then
        assertNotNull(result);
        assertEquals("测试游戏", result.getTitle());

        verify(gameRepository, times(1)).save(any(Game.class));
        verify(unusedRepo, times(1)).countByGameId(testGame.getGameId());
        verify(activationCodeService, times(1)).generateInitialCodes(testGame.getGameId());
    }

    @Test
    void testSaveGameDTO_WithNullFields() {
        // Given
        GameDTO dtoWithNulls = new GameDTO();
        dtoWithNulls.setGameId(2L);
        dtoWithNulls.setTitle("部分信息游戏");
        // 其他字段为null

        when(gameRepository.save(any(Game.class))).thenAnswer(invocation -> {
            Game game = invocation.getArgument(0);
            return game;
        });
        when(unusedRepo.countByGameId(2L)).thenReturn(0L);
        doNothing().when(activationCodeService).generateInitialCodes(2L);

        // When
        GameDTO result = gameService.save(dtoWithNulls);

        // Then
        assertNotNull(result);
        assertEquals("部分信息游戏", result.getTitle());
        assertNull(result.getDeveloper());

        verify(gameRepository, times(1)).save(any(Game.class));
    }

    // ==================== updateGame 方法测试 ====================

    @Test
    void testUpdateGame_Success() {
        // Given
        Long gameId = 1L;
        GameDTO updateDTO = new GameDTO();
        updateDTO.setTitle("更新后的游戏");
        updateDTO.setDeveloper("新开发商");
        updateDTO.setPrice(new BigDecimal("149.99"));

        when(gameRepository.findById(gameId)).thenReturn(Optional.of(testGame));
        when(gameRepository.save(any(Game.class))).thenReturn(testGame);

        // When
        GameDTO result = gameService.updateGame(gameId, updateDTO);

        // Then
        assertNotNull(result);
        verify(gameRepository, times(1)).findById(gameId);
        verify(gameRepository, times(1)).save(any(Game.class));
    }

    @Test
    void testUpdateGame_GameNotFound_ThrowsException() {
        // Given
        Long gameId = 999L;
        GameDTO updateDTO = new GameDTO();
        updateDTO.setTitle("更新后的游戏");

        when(gameRepository.findById(gameId)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> gameService.updateGame(gameId, updateDTO));
        assertTrue(exception.getMessage().contains("Game not found with id: " + gameId));

        verify(gameRepository, times(1)).findById(gameId);
        verify(gameRepository, never()).save(any(Game.class));
    }

    @Test
    void testUpdateGame_PartialUpdate() {
        // Given
        Long gameId = 1L;
        GameDTO updateDTO = new GameDTO();
        updateDTO.setTitle("只更新标题");
        // 其他字段为null，不应被更新

        when(gameRepository.findById(gameId)).thenReturn(Optional.of(testGame));
        when(gameRepository.save(any(Game.class))).thenReturn(testGame);

        // When
        GameDTO result = gameService.updateGame(gameId, updateDTO);

        // Then
        assertNotNull(result);
        verify(gameRepository, times(1)).findById(gameId);
        verify(gameRepository, times(1)).save(any(Game.class));
    }

    @Test
    void testUpdateGame_AllFieldsUpdate() {
        // Given
        Long gameId = 1L;
        GameDTO updateDTO = new GameDTO();
        updateDTO.setTitle("完全更新");
        updateDTO.setDeveloper("新开发商");
        updateDTO.setDescription("新描述");
        updateDTO.setPrice(new BigDecimal("199.99"));
        updateDTO.setDiscountPrice(new BigDecimal("149.99"));
        updateDTO.setGenre("RPG");
        updateDTO.setPlatform("PS5");
        updateDTO.setReleaseDate(LocalDate.of(2025, 1, 1));
        updateDTO.setIsActive(false);
        updateDTO.setImageUrl("http://newimage.com/image.jpg");

        when(gameRepository.findById(gameId)).thenReturn(Optional.of(testGame));
        when(gameRepository.save(any(Game.class))).thenReturn(testGame);

        // When
        GameDTO result = gameService.updateGame(gameId, updateDTO);

        // Then
        assertNotNull(result);
        verify(gameRepository, times(1)).findById(gameId);
        verify(gameRepository, times(1)).save(any(Game.class));
    }

    // ==================== deleteGame 方法测试 ====================

    @Test
    void testDeleteGame_Success() {
        // Given
        Long gameId = 1L;
        when(gameRepository.existsById(gameId)).thenReturn(true);
        doNothing().when(gameRepository).deleteById(gameId);

        // When
        gameService.deleteGame(gameId);

        // Then
        verify(gameRepository, times(1)).existsById(gameId);
        verify(gameRepository, times(1)).deleteById(gameId);
    }

    @Test
    void testDeleteGame_GameNotFound_ThrowsException() {
        // Given
        Long gameId = 999L;
        when(gameRepository.existsById(gameId)).thenReturn(false);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> gameService.deleteGame(gameId));
        assertTrue(exception.getMessage().contains("Game not found with id: " + gameId));

        verify(gameRepository, times(1)).existsById(gameId);
        verify(gameRepository, never()).deleteById(anyLong());
    }

    @Test
    void testDeleteGame_NullId_ThrowsException() {
        // Given
        Long gameId = null;
        when(gameRepository.existsById(gameId)).thenReturn(false);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> gameService.deleteGame(gameId));
        assertTrue(exception.getMessage().contains("Game not found with id: null"));

        verify(gameRepository, times(1)).existsById(gameId);
        verify(gameRepository, never()).deleteById(anyLong());
    }

    // ==================== 集成测试场景 ====================

    @Test
    void testCompleteGameLifecycle() {
        // 1. 创建游戏
        when(gameRepository.save(any(Game.class))).thenReturn(testGame);
        when(unusedRepo.countByGameId(testGame.getGameId())).thenReturn(0L);
        doNothing().when(activationCodeService).generateInitialCodes(testGame.getGameId());

        GameDTO created = gameService.save(testGame);
        assertNotNull(created);

        // 2. 查找游戏
        when(gameRepository.findById(testGame.getGameId())).thenReturn(Optional.of(testGame));
        Optional<GameDTO> found = gameService.findById(testGame.getGameId());
        assertTrue(found.isPresent());

        // 3. 更新游戏
        GameDTO updateDTO = new GameDTO();
        updateDTO.setTitle("更新后的游戏");
        when(gameRepository.findById(testGame.getGameId())).thenReturn(Optional.of(testGame));
        when(gameRepository.save(any(Game.class))).thenReturn(testGame);

        GameDTO updated = gameService.updateGame(testGame.getGameId(), updateDTO);
        assertNotNull(updated);

        // 4. 删除游戏
        when(gameRepository.existsById(testGame.getGameId())).thenReturn(true);
        doNothing().when(gameRepository).deleteById(testGame.getGameId());

        assertDoesNotThrow(() -> gameService.deleteGame(testGame.getGameId()));

        // 验证所有方法都被调用
        verify(gameRepository, times(2)).save(any(Game.class)); // save() 和 updateGame() 各调用一次
        verify(gameRepository, times(2)).findById(testGame.getGameId()); // 直接调用和updateGame内部调用
        verify(gameRepository, times(1)).existsById(testGame.getGameId());
        verify(gameRepository, times(1)).deleteById(testGame.getGameId());
    }

    @Test
    void testSearchAndFilterCombination() {
        // Given
        String searchQuery = "测试";
        String genre = "动作";
        String platform = "PC";
        int limit = 3;

        List<Game> searchResults = Arrays.asList(testGame);
        List<Game> genreResults = Arrays.asList(testGame);
        List<Game> platformResults = Arrays.asList(testGame);
        List<Game> discountResults = Arrays.asList(testGame);

        when(gameRepository.findByTitleContainingIgnoreCase(searchQuery)).thenReturn(searchResults);
        when(gameRepository.findByGenre(genre)).thenReturn(genreResults);
        when(gameRepository.findByPlatform(platform)).thenReturn(platformResults);
        when(gameRepository.findTopDiscountedGames(limit)).thenReturn(discountResults);

        // When
        List<GameDTO> searchResult = gameService.searchByTitle(searchQuery);
        List<GameDTO> genreResult = gameService.findByGenre(genre);
        List<GameDTO> platformResult = gameService.findByPlatform(platform);
        List<GameDTO> discountResult = gameService.findTopDiscountedGames(limit);

        // Then
        assertNotNull(searchResult);
        assertNotNull(genreResult);
        assertNotNull(platformResult);
        assertNotNull(discountResult);

        verify(gameRepository, times(1)).findByTitleContainingIgnoreCase(searchQuery);
        verify(gameRepository, times(1)).findByGenre(genre);
        verify(gameRepository, times(1)).findByPlatform(platform);
        verify(gameRepository, times(1)).findTopDiscountedGames(limit);
    }

    // ==================== 辅助方法 ====================

    private Game createGame(Long id, String title) {
        Game game = new Game();
        game.setGameId(id);
        game.setTitle(title);
        game.setDeveloper("开发商" + id);
        game.setDescription("描述" + id);
        game.setPrice(new BigDecimal("99.99"));
        game.setDiscountPrice(new BigDecimal("79.99"));
        game.setGenre("动作");
        game.setPlatform("PC");
        game.setReleaseDate(LocalDate.of(2024, 1, 1));
        game.setIsActive(true);
        game.setImageUrl("http://example.com/image" + id + ".jpg");
        return game;
    }
}
