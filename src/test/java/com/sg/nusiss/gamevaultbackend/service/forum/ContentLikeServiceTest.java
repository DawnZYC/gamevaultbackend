package com.sg.nusiss.gamevaultbackend.service.forum;

import com.sg.nusiss.gamevaultbackend.constant.forum.ForumRelationType;
import com.sg.nusiss.gamevaultbackend.entity.forum.UserContentRelation;
import com.sg.nusiss.gamevaultbackend.mapper.forum.ForumContentLikeMapper;
import com.sg.nusiss.gamevaultbackend.mapper.forum.ForumMetricMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ForumContentLikeService 单元测试
 *
 * 测试策略：
 * 1. Mock 所有依赖（ForumContentLikeMapper 和 ForumMetricMapper）
 * 2. 测试业务逻辑和参数验证
 * 3. 不涉及真实数据库操作
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ContentLikeServiceTest")
class ContentLikeServiceTest {

    @Mock
    private ForumContentLikeMapper contentLikeMapper;

    @Mock
    private ForumMetricMapper metricMapper;

    @InjectMocks
    private ForumContentLikeService service;

    private Long testUserId;
    private Long testContentId;

    @BeforeEach
    void setUp() {
        testUserId = 1L;
        testContentId = 100L;
    }

    // ================================================================
    // likeContent() 方法测试
    // ================================================================

    @Nested
    @DisplayName("likeContent() 测试")
    class LikeContentTests {

        @Test
        @DisplayName("点赞成功 - 返回 true")
        void likeContent_Success_ReturnsTrue() {
            // Given: 用户未点赞过该内容
            when(contentLikeMapper.existsByUserAndContentAndType(
                    testUserId, testContentId, ForumRelationType.LIKE))
                    .thenReturn(false);
            when(contentLikeMapper.insert(any(UserContentRelation.class)))
                    .thenReturn(1);

            // When: 执行点赞
            boolean result = service.likeContent(testContentId, testUserId);

            // Then: 验证结果和调用
            assertTrue(result);
            verify(contentLikeMapper).existsByUserAndContentAndType(
                    testUserId, testContentId, ForumRelationType.LIKE);
            verify(contentLikeMapper).insert(any(UserContentRelation.class));

            // 验证插入的对象内容
            ArgumentCaptor<UserContentRelation> captor =
                    ArgumentCaptor.forClass(UserContentRelation.class);
            verify(contentLikeMapper).insert(captor.capture());
            UserContentRelation captured = captor.getValue();
            assertEquals(testUserId, captured.getUserId());
            assertEquals(testContentId, captured.getContentId());
            assertNotNull(captured.getCreatedDate());
        }

        @Test
        @DisplayName("重复点赞 - 返回 false")
        void likeContent_AlreadyLiked_ReturnsFalse() {
            // Given: 用户已经点赞过
            when(contentLikeMapper.existsByUserAndContentAndType(
                    testUserId, testContentId, ForumRelationType.LIKE))
                    .thenReturn(true);

            // When: 尝试再次点赞
            boolean result = service.likeContent(testContentId, testUserId);

            // Then: 返回 false，不执行插入
            assertFalse(result);
            verify(contentLikeMapper).existsByUserAndContentAndType(
                    testUserId, testContentId, ForumRelationType.LIKE);
            verify(contentLikeMapper, never()).insert(any());
        }

        @Test
        @DisplayName("contentId 为 null - 抛出异常")
        void likeContent_NullContentId_ThrowsException() {
            // When & Then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> service.likeContent(null, testUserId)
            );
            assertEquals("内容ID和用户ID不能为空", exception.getMessage());
            verify(contentLikeMapper, never()).existsByUserAndContentAndType(any(), any(), any());
        }

        @Test
        @DisplayName("userId 为 null - 抛出异常")
        void likeContent_NullUserId_ThrowsException() {
            // When & Then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> service.likeContent(testContentId, null)
            );
            assertEquals("内容ID和用户ID不能为空", exception.getMessage());
            verify(contentLikeMapper, never()).existsByUserAndContentAndType(any(), any(), any());
        }

        @Test
        @DisplayName("两个参数都为 null - 抛出异常")
        void likeContent_BothNull_ThrowsException() {
            // When & Then
            assertThrows(IllegalArgumentException.class,
                    () -> service.likeContent(null, null));
            verify(contentLikeMapper, never()).existsByUserAndContentAndType(any(), any(), any());
        }

        @Test
        @DisplayName("插入失败 - 返回 false")
        void likeContent_InsertFails_ReturnsFalse() {
            // Given: 插入返回 0（失败）
            when(contentLikeMapper.existsByUserAndContentAndType(
                    testUserId, testContentId, ForumRelationType.LIKE))
                    .thenReturn(false);
            when(contentLikeMapper.insert(any(UserContentRelation.class)))
                    .thenReturn(0);

            // When
            boolean result = service.likeContent(testContentId, testUserId);

            // Then
            assertFalse(result);
        }
    }

    // ================================================================
    // unlikeContent() 方法测试
    // ================================================================

    @Nested
    @DisplayName("unlikeContent() 测试")
    class UnlikeContentTests {

        @Test
        @DisplayName("取消点赞成功 - 返回 true")
        void unlikeContent_Success_ReturnsTrue() {
            // Given: 用户已点赞
            when(contentLikeMapper.existsByUserAndContentAndType(
                    testUserId, testContentId, ForumRelationType.LIKE))
                    .thenReturn(true);
            when(contentLikeMapper.deleteByUserAndContentAndType(
                    testUserId, testContentId, ForumRelationType.LIKE))
                    .thenReturn(1);

            // When
            boolean result = service.unlikeContent(testContentId, testUserId);

            // Then
            assertTrue(result);
            verify(contentLikeMapper).existsByUserAndContentAndType(
                    testUserId, testContentId, ForumRelationType.LIKE);
            verify(contentLikeMapper).deleteByUserAndContentAndType(
                    testUserId, testContentId, ForumRelationType.LIKE);
        }

        @Test
        @DisplayName("未点赞时取消 - 返回 false")
        void unlikeContent_NotLiked_ReturnsFalse() {
            // Given: 用户未点赞
            when(contentLikeMapper.existsByUserAndContentAndType(
                    testUserId, testContentId, ForumRelationType.LIKE))
                    .thenReturn(false);

            // When
            boolean result = service.unlikeContent(testContentId, testUserId);

            // Then
            assertFalse(result);
            verify(contentLikeMapper, never()).deleteByUserAndContentAndType(any(), any(), any());
        }

        @Test
        @DisplayName("contentId 为 null - 抛出异常")
        void unlikeContent_NullContentId_ThrowsException() {
            // When & Then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> service.unlikeContent(null, testUserId)
            );
            assertEquals("内容ID和用户ID不能为空", exception.getMessage());
        }

        @Test
        @DisplayName("userId 为 null - 抛出异常")
        void unlikeContent_NullUserId_ThrowsException() {
            // When & Then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> service.unlikeContent(testContentId, null)
            );
            assertEquals("内容ID和用户ID不能为空", exception.getMessage());
        }

        @Test
        @DisplayName("删除失败 - 返回 false")
        void unlikeContent_DeleteFails_ReturnsFalse() {
            // Given
            when(contentLikeMapper.existsByUserAndContentAndType(
                    testUserId, testContentId, ForumRelationType.LIKE))
                    .thenReturn(true);
            when(contentLikeMapper.deleteByUserAndContentAndType(
                    testUserId, testContentId, ForumRelationType.LIKE))
                    .thenReturn(0);

            // When
            boolean result = service.unlikeContent(testContentId, testUserId);

            // Then
            assertFalse(result);
        }
    }

    // ================================================================
    // toggleLike() 方法测试
    // ================================================================

    @Nested
    @DisplayName("toggleLike() 测试")
    class ToggleLikeTests {

        @Test
        @DisplayName("未点赞状态 -> 点赞 - 返回 true")
        void toggleLike_NotLiked_LikesAndReturnsTrue() {
            // Given: 用户未点赞
            when(contentLikeMapper.existsByUserAndContentAndType(
                    testUserId, testContentId, ForumRelationType.LIKE))
                    .thenReturn(false);
            when(contentLikeMapper.insert(any(UserContentRelation.class)))
                    .thenReturn(1);

            // When
            boolean result = service.toggleLike(testContentId, testUserId);

            // Then: 返回 true 表示点赞了
            assertTrue(result);
            verify(contentLikeMapper).insert(any(UserContentRelation.class));
            verify(contentLikeMapper, never()).deleteByUserAndContentAndType(any(), any(), any());
        }

        @Test
        @DisplayName("已点赞状态 -> 取消点赞 - 返回 false")
        void toggleLike_AlreadyLiked_UnlikesAndReturnsFalse() {
            // Given: 用户已点赞
            when(contentLikeMapper.existsByUserAndContentAndType(
                    testUserId, testContentId, ForumRelationType.LIKE))
                    .thenReturn(true);
            when(contentLikeMapper.deleteByUserAndContentAndType(
                    testUserId, testContentId, ForumRelationType.LIKE))
                    .thenReturn(1);

            // When
            boolean result = service.toggleLike(testContentId, testUserId);

            // Then: 返回 false 表示取消点赞了
            assertFalse(result);
            verify(contentLikeMapper).deleteByUserAndContentAndType(
                    testUserId, testContentId, ForumRelationType.LIKE);
            verify(contentLikeMapper, never()).insert(any());
        }

        @Test
        @DisplayName("多次切换 - 状态正确切换")
        void toggleLike_MultipleTimes_TogglesCorrectly() {
            // 第一次调用：未点赞 -> 点赞
            when(contentLikeMapper.existsByUserAndContentAndType(
                    testUserId, testContentId, ForumRelationType.LIKE))
                    .thenReturn(false);
            when(contentLikeMapper.insert(any())).thenReturn(1);

            boolean result1 = service.toggleLike(testContentId, testUserId);
            assertTrue(result1);
            verify(contentLikeMapper).insert(any());

            // 重置 Mock 为第二次调用准备
            reset(contentLikeMapper);

            // 第二次调用：已点赞 -> 取消
            when(contentLikeMapper.existsByUserAndContentAndType(
                    testUserId, testContentId, ForumRelationType.LIKE))
                    .thenReturn(true);
            when(contentLikeMapper.deleteByUserAndContentAndType(
                    testUserId, testContentId, ForumRelationType.LIKE))
                    .thenReturn(1);

            boolean result2 = service.toggleLike(testContentId, testUserId);
            assertFalse(result2);
            verify(contentLikeMapper).deleteByUserAndContentAndType(
                    testUserId, testContentId, ForumRelationType.LIKE);
        }
    }

    // ================================================================
    // isLiked() 方法测试
    // ================================================================

    @Nested
    @DisplayName("isLiked() 测试")
    class IsLikedTests {

        @Test
        @DisplayName("已点赞 - 返回 true")
        void isLiked_UserHasLiked_ReturnsTrue() {
            // Given
            when(contentLikeMapper.existsByUserAndContentAndType(
                    testUserId, testContentId, ForumRelationType.LIKE))
                    .thenReturn(true);

            // When
            boolean result = service.isLiked(testContentId, testUserId);

            // Then
            assertTrue(result);
        }

        @Test
        @DisplayName("未点赞 - 返回 false")
        void isLiked_UserHasNotLiked_ReturnsFalse() {
            // Given
            when(contentLikeMapper.existsByUserAndContentAndType(
                    testUserId, testContentId, ForumRelationType.LIKE))
                    .thenReturn(false);

            // When
            boolean result = service.isLiked(testContentId, testUserId);

            // Then
            assertFalse(result);
        }

        @Test
        @DisplayName("contentId 为 null - 返回 false")
        void isLiked_NullContentId_ReturnsFalse() {
            // When
            boolean result = service.isLiked(null, testUserId);

            // Then
            assertFalse(result);
            verify(contentLikeMapper, never()).existsByUserAndContentAndType(any(), any(), any());
        }

        @Test
        @DisplayName("userId 为 null - 返回 false")
        void isLiked_NullUserId_ReturnsFalse() {
            // When
            boolean result = service.isLiked(testContentId, null);

            // Then
            assertFalse(result);
            verify(contentLikeMapper, never()).existsByUserAndContentAndType(any(), any(), any());
        }

        @Test
        @DisplayName("两个参数都为 null - 返回 false")
        void isLiked_BothNull_ReturnsFalse() {
            // When
            boolean result = service.isLiked(null, null);

            // Then
            assertFalse(result);
        }
    }

    // ================================================================
    // getLikeCount() 方法测试
    // ================================================================

    @Nested
    @DisplayName("getLikeCount() 测试")
    class GetLikeCountTests {

        @Test
        @DisplayName("正常查询 - 返回点赞数")
        void getLikeCount_ValidContentId_ReturnsCount() {
            // Given
            when(contentLikeMapper.countByContentAndType(
                    testContentId, ForumRelationType.LIKE))
                    .thenReturn(5);

            // When
            int count = service.getLikeCount(testContentId);

            // Then
            assertEquals(5, count);
        }

        @Test
        @DisplayName("内容无点赞 - 返回 0")
        void getLikeCount_NoLikes_ReturnsZero() {
            // Given
            when(contentLikeMapper.countByContentAndType(
                    testContentId, ForumRelationType.LIKE))
                    .thenReturn(0);

            // When
            int count = service.getLikeCount(testContentId);

            // Then
            assertEquals(0, count);
        }

        @Test
        @DisplayName("contentId 为 null - 返回 0")
        void getLikeCount_NullContentId_ReturnsZero() {
            // When
            int count = service.getLikeCount(null);

            // Then
            assertEquals(0, count);
            verify(contentLikeMapper, never()).countByContentAndType(any(), any());
        }
    }

    // ================================================================
    // getLikedUserIds() 方法测试
    // ================================================================

    @Nested
    @DisplayName("getLikedUserIds() 测试")
    class GetLikedUserIdsTests {

        @Test
        @DisplayName("正常查询 - 返回用户ID列表")
        void getLikedUserIds_ValidContentId_ReturnsUserIds() {
            // Given
            List<Long> userIds = Arrays.asList(1L, 2L, 3L);
            when(contentLikeMapper.findUserIdsByContentAndType(
                    testContentId, ForumRelationType.LIKE))
                    .thenReturn(userIds);

            // When
            List<Long> result = service.getLikedUserIds(testContentId);

            // Then
            assertEquals(3, result.size());
            assertEquals(userIds, result);
        }

        @Test
        @DisplayName("无点赞用户 - 返回空列表")
        void getLikedUserIds_NoLikes_ReturnsEmptyList() {
            // Given
            when(contentLikeMapper.findUserIdsByContentAndType(
                    testContentId, ForumRelationType.LIKE))
                    .thenReturn(Collections.emptyList());

            // When
            List<Long> result = service.getLikedUserIds(testContentId);

            // Then
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("contentId 为 null - 抛出异常")
        void getLikedUserIds_NullContentId_ThrowsException() {
            // When & Then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> service.getLikedUserIds(null)
            );
            assertEquals("内容ID不能为空", exception.getMessage());
        }
    }

    // ================================================================
    // getUserLikedContentIds() 方法测试
    // ================================================================

    @Nested
    @DisplayName("getUserLikedContentIds() 测试")
    class GetUserLikedContentIdsTests {

        @Test
        @DisplayName("正常查询 - 返回内容ID列表")
        void getUserLikedContentIds_ValidUserId_ReturnsContentIds() {
            // Given
            List<Long> contentIds = Arrays.asList(100L, 101L, 102L);
            when(contentLikeMapper.findContentIdsByUserAndType(
                    testUserId, ForumRelationType.LIKE))
                    .thenReturn(contentIds);

            // When
            List<Long> result = service.getUserLikedContentIds(testUserId);

            // Then
            assertEquals(3, result.size());
            assertEquals(contentIds, result);
        }

        @Test
        @DisplayName("用户无点赞 - 返回空列表")
        void getUserLikedContentIds_NoLikes_ReturnsEmptyList() {
            // Given
            when(contentLikeMapper.findContentIdsByUserAndType(
                    testUserId, ForumRelationType.LIKE))
                    .thenReturn(Collections.emptyList());

            // When
            List<Long> result = service.getUserLikedContentIds(testUserId);

            // Then
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("userId 为 null - 抛出异常")
        void getUserLikedContentIds_NullUserId_ThrowsException() {
            // When & Then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> service.getUserLikedContentIds(null)
            );
            assertEquals("用户ID不能为空", exception.getMessage());
        }
    }

    // ================================================================
    // batchCheckLikeStatus() 方法测试
    // ================================================================

    @Nested
    @DisplayName("batchCheckLikeStatus() 测试")
    class BatchCheckLikeStatusTests {

        @Test
        @DisplayName("正常批量查询 - 返回正确的状态Map")
        void batchCheckLikeStatus_ValidInput_ReturnsCorrectMap() {
            // Given
            List<Long> contentIds = Arrays.asList(100L, 101L, 102L, 103L);
            List<Long> likedIds = Arrays.asList(100L, 102L);

            when(contentLikeMapper.findLikedContentIdsByUserAndType(
                    testUserId, contentIds, ForumRelationType.LIKE))
                    .thenReturn(likedIds);

            // When
            Map<Long, Boolean> result = service.batchCheckLikeStatus(testUserId, contentIds);

            // Then
            assertEquals(4, result.size());
            assertTrue(result.get(100L));
            assertFalse(result.get(101L));
            assertTrue(result.get(102L));
            assertFalse(result.get(103L));
        }

        @Test
        @DisplayName("全部未点赞 - 返回全false的Map")
        void batchCheckLikeStatus_NoLikes_ReturnsAllFalse() {
            // Given
            List<Long> contentIds = Arrays.asList(100L, 101L);
            when(contentLikeMapper.findLikedContentIdsByUserAndType(
                    testUserId, contentIds, ForumRelationType.LIKE))
                    .thenReturn(Collections.emptyList());

            // When
            Map<Long, Boolean> result = service.batchCheckLikeStatus(testUserId, contentIds);

            // Then
            assertEquals(2, result.size());
            assertFalse(result.get(100L));
            assertFalse(result.get(101L));
        }

        @Test
        @DisplayName("全部已点赞 - 返回全true的Map")
        void batchCheckLikeStatus_AllLiked_ReturnsAllTrue() {
            // Given
            List<Long> contentIds = Arrays.asList(100L, 101L);
            when(contentLikeMapper.findLikedContentIdsByUserAndType(
                    testUserId, contentIds, ForumRelationType.LIKE))
                    .thenReturn(contentIds);

            // When
            Map<Long, Boolean> result = service.batchCheckLikeStatus(testUserId, contentIds);

            // Then
            assertEquals(2, result.size());
            assertTrue(result.get(100L));
            assertTrue(result.get(101L));
        }

        @Test
        @DisplayName("userId 为 null - 返回空Map")
        void batchCheckLikeStatus_NullUserId_ReturnsEmptyMap() {
            // When
            Map<Long, Boolean> result = service.batchCheckLikeStatus(
                    null, Arrays.asList(100L, 101L));

            // Then
            assertTrue(result.isEmpty());
            verify(contentLikeMapper, never()).findLikedContentIdsByUserAndType(any(), any(), any());
        }

        @Test
        @DisplayName("contentIds 为 null - 返回空Map")
        void batchCheckLikeStatus_NullContentIds_ReturnsEmptyMap() {
            // When
            Map<Long, Boolean> result = service.batchCheckLikeStatus(testUserId, null);

            // Then
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("contentIds 为空列表 - 返回空Map")
        void batchCheckLikeStatus_EmptyContentIds_ReturnsEmptyMap() {
            // When
            Map<Long, Boolean> result = service.batchCheckLikeStatus(
                    testUserId, Collections.emptyList());

            // Then
            assertTrue(result.isEmpty());
        }
    }

    // ================================================================
    // batchGetLikeCounts() 方法测试
    // ================================================================

    @Nested
    @DisplayName("batchGetLikeCounts() 测试")
    class BatchGetLikeCountsTests {

        @Test
        @DisplayName("正常批量查询 - 返回点赞数Map")
        void batchGetLikeCounts_ValidInput_ReturnsCountMap() {
            // Given
            List<Long> contentIds = Arrays.asList(100L, 101L, 102L);
            Map<Long, Integer> expectedCounts = new HashMap<>();
            expectedCounts.put(100L, 5);
            expectedCounts.put(101L, 10);
            expectedCounts.put(102L, 3);

            when(metricMapper.getBatchMetrics(contentIds, "like_count"))
                    .thenReturn(expectedCounts);

            // When
            Map<Long, Integer> result = service.batchGetLikeCounts(contentIds);

            // Then
            assertEquals(3, result.size());
            assertEquals(5, result.get(100L));
            assertEquals(10, result.get(101L));
            assertEquals(3, result.get(102L));
        }

        @Test
        @DisplayName("contentIds 为 null - 返回空Map")
        void batchGetLikeCounts_NullContentIds_ReturnsEmptyMap() {
            // When
            Map<Long, Integer> result = service.batchGetLikeCounts(null);

            // Then
            assertTrue(result.isEmpty());
            verify(metricMapper, never()).getBatchMetrics(any(), any());
        }

        @Test
        @DisplayName("contentIds 为空列表 - 返回空Map")
        void batchGetLikeCounts_EmptyContentIds_ReturnsEmptyMap() {
            // When
            Map<Long, Integer> result = service.batchGetLikeCounts(Collections.emptyList());

            // Then
            assertTrue(result.isEmpty());
        }
    }

    // ================================================================
    // getUserRecentLikes() 方法测试
    // ================================================================

    @Nested
    @DisplayName("getUserRecentLikes() 测试")
    class GetUserRecentLikesTests {

        @Test
        @DisplayName("正常查询 - 返回最近点赞列表")
        void getUserRecentLikes_ValidInput_ReturnsRecentLikes() {
            // Given
            List<UserContentRelation> relations = Arrays.asList(
                    new UserContentRelation(testUserId, 100L, ForumRelationType.LIKE),
                    new UserContentRelation(testUserId, 101L, ForumRelationType.LIKE)
            );
            when(contentLikeMapper.findRecentByUserAndType(
                    testUserId, ForumRelationType.LIKE, 10))
                    .thenReturn(relations);

            // When
            List<UserContentRelation> result = service.getUserRecentLikes(testUserId, 10);

            // Then
            assertEquals(2, result.size());
            assertEquals(relations, result);
        }

        @Test
        @DisplayName("userId 为 null - 抛出异常")
        void getUserRecentLikes_NullUserId_ThrowsException() {
            // When & Then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> service.getUserRecentLikes(null, 10)
            );
            assertEquals("用户ID不能为空", exception.getMessage());
        }

        @Test
        @DisplayName("limit 为 0 - 正常调用")
        void getUserRecentLikes_ZeroLimit_CallsMapper() {
            // Given
            when(contentLikeMapper.findRecentByUserAndType(
                    testUserId, ForumRelationType.LIKE, 0))
                    .thenReturn(Collections.emptyList());

            // When
            List<UserContentRelation> result = service.getUserRecentLikes(testUserId, 0);

            // Then
            assertTrue(result.isEmpty());
            verify(contentLikeMapper).findRecentByUserAndType(testUserId, ForumRelationType.LIKE, 0);
        }
    }

    // ================================================================
    // getContentRecentLikes() 方法测试
    // ================================================================

    @Nested
    @DisplayName("getContentRecentLikes() 测试")
    class GetContentRecentLikesTests {

        @Test
        @DisplayName("正常查询 - 返回最近点赞列表")
        void getContentRecentLikes_ValidInput_ReturnsRecentLikes() {
            // Given
            List<UserContentRelation> relations = Arrays.asList(
                    new UserContentRelation(1L, testContentId, ForumRelationType.LIKE),
                    new UserContentRelation(2L, testContentId, ForumRelationType.LIKE)
            );
            when(contentLikeMapper.findRecentByContentAndType(
                    testContentId, ForumRelationType.LIKE, 10))
                    .thenReturn(relations);

            // When
            List<UserContentRelation> result = service.getContentRecentLikes(testContentId, 10);

            // Then
            assertEquals(2, result.size());
            assertEquals(relations, result);
        }

        @Test
        @DisplayName("contentId 为 null - 抛出异常")
        void getContentRecentLikes_NullContentId_ThrowsException() {
            // When & Then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> service.getContentRecentLikes(null, 10)
            );
            assertEquals("内容ID不能为空", exception.getMessage());
        }
    }

    // ================================================================
    // syncLikeCount() 方法测试
    // ================================================================

    @Nested
    @DisplayName("syncLikeCount() 测试")
    class SyncLikeCountTests {

        @Test
        @DisplayName("正常同步 - 调用正确的方法")
        void syncLikeCount_ValidContentId_CallsCorrectMethods() {
            // Given
            when(contentLikeMapper.countByContentAndType(
                    testContentId, ForumRelationType.LIKE))
                    .thenReturn(5);

            // When
            service.syncLikeCount(testContentId);

            // Then
            verify(contentLikeMapper).countByContentAndType(testContentId, ForumRelationType.LIKE);
            verify(metricMapper).setMetricValue(testContentId, "like_count", 5);
        }

        @Test
        @DisplayName("点赞数为 0 - 正常同步")
        void syncLikeCount_ZeroCount_SyncsCorrectly() {
            // Given
            when(contentLikeMapper.countByContentAndType(
                    testContentId, ForumRelationType.LIKE))
                    .thenReturn(0);

            // When
            service.syncLikeCount(testContentId);

            // Then
            verify(metricMapper).setMetricValue(testContentId, "like_count", 0);
        }

        @Test
        @DisplayName("contentId 为 null - 抛出异常")
        void syncLikeCount_NullContentId_ThrowsException() {
            // When & Then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> service.syncLikeCount(null)
            );
            assertEquals("内容ID不能为空", exception.getMessage());
            verify(contentLikeMapper, never()).countByContentAndType(any(), any());
        }
    }

    // ================================================================
    // batchSyncLikeCounts() 方法测试
    // ================================================================

    @Nested
    @DisplayName("batchSyncLikeCounts() 测试")
    class BatchSyncLikeCountsTests {

        @Test
        @DisplayName("批量同步 - 遍历所有ID")
        void batchSyncLikeCounts_ValidList_SyncsAll() {
            // Given
            List<Long> contentIds = Arrays.asList(100L, 101L, 102L);
            when(contentLikeMapper.countByContentAndType(anyLong(), eq(ForumRelationType.LIKE)))
                    .thenReturn(5, 3, 8);

            // When
            service.batchSyncLikeCounts(contentIds);

            // Then
            verify(contentLikeMapper, times(3)).countByContentAndType(anyLong(), eq(ForumRelationType.LIKE));
            verify(metricMapper).setMetricValue(100L, "like_count", 5);
            verify(metricMapper).setMetricValue(101L, "like_count", 3);
            verify(metricMapper).setMetricValue(102L, "like_count", 8);
        }

        @Test
        @DisplayName("contentIds 为 null - 不执行任何操作")
        void batchSyncLikeCounts_NullList_DoesNothing() {
            // When
            service.batchSyncLikeCounts(null);

            // Then
            verify(contentLikeMapper, never()).countByContentAndType(any(), any());
            verify(metricMapper, never()).setMetricValue(any(), any(), anyInt());
        }

        @Test
        @DisplayName("contentIds 为空列表 - 不执行任何操作")
        void batchSyncLikeCounts_EmptyList_DoesNothing() {
            // When
            service.batchSyncLikeCounts(Collections.emptyList());

            // Then
            verify(contentLikeMapper, never()).countByContentAndType(any(), any());
            verify(metricMapper, never()).setMetricValue(any(), any(), anyInt());
        }
    }

    // ================================================================
    // getTopLikedContents() 方法测试
    // ================================================================

    @Nested
    @DisplayName("getTopLikedContents() 测试")
    class GetTopLikedContentsTests {

        @Test
        @DisplayName("正常查询 - 返回热门内容列表")
        void getTopLikedContents_ValidLimit_ReturnsTopContents() {
            // Given
            List<Long> topContentIds = Arrays.asList(100L, 101L, 102L);
            when(metricMapper.findTopContentsByMetric("like_count", 3))
                    .thenReturn(topContentIds);

            // When
            List<Long> result = service.getTopLikedContents(3);

            // Then
            assertEquals(3, result.size());
            assertEquals(topContentIds, result);
        }

        @Test
        @DisplayName("limit 为 0 - 返回空列表")
        void getTopLikedContents_ZeroLimit_ReturnsEmptyList() {
            // Given
            when(metricMapper.findTopContentsByMetric("like_count", 0))
                    .thenReturn(Collections.emptyList());

            // When
            List<Long> result = service.getTopLikedContents(0);

            // Then
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("大limit值 - 正常调用")
        void getTopLikedContents_LargeLimit_CallsMapper() {
            // Given
            when(metricMapper.findTopContentsByMetric("like_count", 100))
                    .thenReturn(Collections.emptyList());

            // When
            List<Long> result = service.getTopLikedContents(100);

            // Then
            verify(metricMapper).findTopContentsByMetric("like_count", 100);
        }
    }
}