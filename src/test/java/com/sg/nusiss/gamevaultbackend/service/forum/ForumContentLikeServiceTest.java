package com.sg.nusiss.gamevaultbackend.service.forum;

import com.sg.nusiss.gamevaultbackend.constant.forum.ForumRelationType;
import com.sg.nusiss.gamevaultbackend.entity.forum.UserContentRelation;
import com.sg.nusiss.gamevaultbackend.mapper.forum.ForumContentLikeMapper;
import com.sg.nusiss.gamevaultbackend.mapper.forum.ForumMetricMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * @ClassName ForumContentLikeServiceTest
 * @Author Hou Zheyu
 * @Date 2025/10/25
 * @Description ForumContentLikeService单元测试类，覆盖所有方法
 */
@ExtendWith(MockitoExtension.class)
class ForumContentLikeServiceTest {

    @Mock
    private ForumContentLikeMapper contentLikeMapper;

    @Mock
    private ForumMetricMapper metricMapper;

    @InjectMocks
    private ForumContentLikeService forumContentLikeService;

    private Long testContentId;
    private Long testUserId;
    private UserContentRelation testRelation;

    @BeforeEach
    void setUp() {
        testContentId = 1L;
        testUserId = 1L;

        testRelation = new UserContentRelation();
        testRelation.setUserId(testUserId);
        testRelation.setContentId(testContentId);
        testRelation.setRelationType(ForumRelationType.LIKE);
        testRelation.setCreatedDate(LocalDateTime.now());
    }

    // ==================== likeContent 方法测试 ====================

    @Test
    void testLikeContent_Success() {
        // Given
        when(contentLikeMapper.existsByUserAndContentAndType(testUserId, testContentId, ForumRelationType.LIKE))
            .thenReturn(false);
        when(contentLikeMapper.insert(any(UserContentRelation.class))).thenReturn(1);

        // When
        boolean result = forumContentLikeService.likeContent(testContentId, testUserId);

        // Then
        assertTrue(result);
        verify(contentLikeMapper, times(1)).existsByUserAndContentAndType(testUserId, testContentId, ForumRelationType.LIKE);
        verify(contentLikeMapper, times(1)).insert(any(UserContentRelation.class));
    }

    @Test
    void testLikeContent_AlreadyLiked_ReturnsFalse() {
        // Given
        when(contentLikeMapper.existsByUserAndContentAndType(testUserId, testContentId, ForumRelationType.LIKE))
            .thenReturn(true);

        // When
        boolean result = forumContentLikeService.likeContent(testContentId, testUserId);

        // Then
        assertFalse(result);
        verify(contentLikeMapper, times(1)).existsByUserAndContentAndType(testUserId, testContentId, ForumRelationType.LIKE);
        verify(contentLikeMapper, never()).insert(any(UserContentRelation.class));
    }

    @Test
    void testLikeContent_NullContentId_ThrowsException() {
        // Given
        Long contentId = null;

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> forumContentLikeService.likeContent(contentId, testUserId));
        assertEquals("内容ID和用户ID不能为空", exception.getMessage());

        verify(contentLikeMapper, never()).existsByUserAndContentAndType(anyLong(), anyLong(), any());
    }

    @Test
    void testLikeContent_NullUserId_ThrowsException() {
        // Given
        Long userId = null;

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> forumContentLikeService.likeContent(testContentId, userId));
        assertEquals("内容ID和用户ID不能为空", exception.getMessage());

        verify(contentLikeMapper, never()).existsByUserAndContentAndType(anyLong(), anyLong(), any());
    }

    @Test
    void testLikeContent_InsertFails_ReturnsFalse() {
        // Given
        when(contentLikeMapper.existsByUserAndContentAndType(testUserId, testContentId, ForumRelationType.LIKE))
            .thenReturn(false);
        when(contentLikeMapper.insert(any(UserContentRelation.class))).thenReturn(0);

        // When
        boolean result = forumContentLikeService.likeContent(testContentId, testUserId);

        // Then
        assertFalse(result);
        verify(contentLikeMapper, times(1)).existsByUserAndContentAndType(testUserId, testContentId, ForumRelationType.LIKE);
        verify(contentLikeMapper, times(1)).insert(any(UserContentRelation.class));
    }

    // ==================== unlikeContent 方法测试 ====================

    @Test
    void testUnlikeContent_Success() {
        // Given
        when(contentLikeMapper.existsByUserAndContentAndType(testUserId, testContentId, ForumRelationType.LIKE))
            .thenReturn(true);
        when(contentLikeMapper.deleteByUserAndContentAndType(testUserId, testContentId, ForumRelationType.LIKE))
            .thenReturn(1);

        // When
        boolean result = forumContentLikeService.unlikeContent(testContentId, testUserId);

        // Then
        assertTrue(result);
        verify(contentLikeMapper, times(1)).existsByUserAndContentAndType(testUserId, testContentId, ForumRelationType.LIKE);
        verify(contentLikeMapper, times(1)).deleteByUserAndContentAndType(testUserId, testContentId, ForumRelationType.LIKE);
    }

    @Test
    void testUnlikeContent_NotLiked_ReturnsFalse() {
        // Given
        when(contentLikeMapper.existsByUserAndContentAndType(testUserId, testContentId, ForumRelationType.LIKE))
            .thenReturn(false);

        // When
        boolean result = forumContentLikeService.unlikeContent(testContentId, testUserId);

        // Then
        assertFalse(result);
        verify(contentLikeMapper, times(1)).existsByUserAndContentAndType(testUserId, testContentId, ForumRelationType.LIKE);
        verify(contentLikeMapper, never()).deleteByUserAndContentAndType(anyLong(), anyLong(), any());
    }

    @Test
    void testUnlikeContent_NullContentId_ThrowsException() {
        // Given
        Long contentId = null;

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> forumContentLikeService.unlikeContent(contentId, testUserId));
        assertEquals("内容ID和用户ID不能为空", exception.getMessage());

        verify(contentLikeMapper, never()).existsByUserAndContentAndType(anyLong(), anyLong(), any());
    }

    @Test
    void testUnlikeContent_NullUserId_ThrowsException() {
        // Given
        Long userId = null;

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> forumContentLikeService.unlikeContent(testContentId, userId));
        assertEquals("内容ID和用户ID不能为空", exception.getMessage());

        verify(contentLikeMapper, never()).existsByUserAndContentAndType(anyLong(), anyLong(), any());
    }

    @Test
    void testUnlikeContent_DeleteFails_ReturnsFalse() {
        // Given
        when(contentLikeMapper.existsByUserAndContentAndType(testUserId, testContentId, ForumRelationType.LIKE))
            .thenReturn(true);
        when(contentLikeMapper.deleteByUserAndContentAndType(testUserId, testContentId, ForumRelationType.LIKE))
            .thenReturn(0);

        // When
        boolean result = forumContentLikeService.unlikeContent(testContentId, testUserId);

        // Then
        assertFalse(result);
        verify(contentLikeMapper, times(1)).existsByUserAndContentAndType(testUserId, testContentId, ForumRelationType.LIKE);
        verify(contentLikeMapper, times(1)).deleteByUserAndContentAndType(testUserId, testContentId, ForumRelationType.LIKE);
    }

    // ==================== toggleLike 方法测试 ====================

    @Test
    void testToggleLike_FromNotLikedToLiked_ReturnsTrue() {
        // Given
        when(contentLikeMapper.existsByUserAndContentAndType(testUserId, testContentId, ForumRelationType.LIKE))
            .thenReturn(false);
        when(contentLikeMapper.insert(any(UserContentRelation.class))).thenReturn(1);

        // When
        boolean result = forumContentLikeService.toggleLike(testContentId, testUserId);

        // Then
        assertTrue(result);
        // toggleLike会调用existsByUserAndContentAndType一次，然后调用likeContent，likeContent又会调用一次existsByUserAndContentAndType
        verify(contentLikeMapper, times(2)).existsByUserAndContentAndType(testUserId, testContentId, ForumRelationType.LIKE);
        verify(contentLikeMapper, times(1)).insert(any(UserContentRelation.class));
        verify(contentLikeMapper, never()).deleteByUserAndContentAndType(anyLong(), anyLong(), any());
    }

    @Test
    void testToggleLike_FromLikedToNotLiked_ReturnsFalse() {
        // Given
        when(contentLikeMapper.existsByUserAndContentAndType(testUserId, testContentId, ForumRelationType.LIKE))
            .thenReturn(true);
        when(contentLikeMapper.deleteByUserAndContentAndType(testUserId, testContentId, ForumRelationType.LIKE))
            .thenReturn(1);

        // When
        boolean result = forumContentLikeService.toggleLike(testContentId, testUserId);

        // Then
        assertFalse(result);
        // toggleLike会调用existsByUserAndContentAndType一次，然后调用unlikeContent，unlikeContent又会调用一次existsByUserAndContentAndType
        verify(contentLikeMapper, times(2)).existsByUserAndContentAndType(testUserId, testContentId, ForumRelationType.LIKE);
        verify(contentLikeMapper, times(1)).deleteByUserAndContentAndType(testUserId, testContentId, ForumRelationType.LIKE);
        verify(contentLikeMapper, never()).insert(any(UserContentRelation.class));
    }

    // ==================== isLiked 方法测试 ====================

    @Test
    void testIsLiked_UserLiked_ReturnsTrue() {
        // Given
        when(contentLikeMapper.existsByUserAndContentAndType(testUserId, testContentId, ForumRelationType.LIKE))
            .thenReturn(true);

        // When
        boolean result = forumContentLikeService.isLiked(testContentId, testUserId);

        // Then
        assertTrue(result);
        verify(contentLikeMapper, times(1)).existsByUserAndContentAndType(testUserId, testContentId, ForumRelationType.LIKE);
    }

    @Test
    void testIsLiked_UserNotLiked_ReturnsFalse() {
        // Given
        when(contentLikeMapper.existsByUserAndContentAndType(testUserId, testContentId, ForumRelationType.LIKE))
            .thenReturn(false);

        // When
        boolean result = forumContentLikeService.isLiked(testContentId, testUserId);

        // Then
        assertFalse(result);
        verify(contentLikeMapper, times(1)).existsByUserAndContentAndType(testUserId, testContentId, ForumRelationType.LIKE);
    }

    @Test
    void testIsLiked_NullContentId_ReturnsFalse() {
        // Given
        Long contentId = null;

        // When
        boolean result = forumContentLikeService.isLiked(contentId, testUserId);

        // Then
        assertFalse(result);
        verify(contentLikeMapper, never()).existsByUserAndContentAndType(anyLong(), anyLong(), any());
    }

    @Test
    void testIsLiked_NullUserId_ReturnsFalse() {
        // Given
        Long userId = null;

        // When
        boolean result = forumContentLikeService.isLiked(testContentId, userId);

        // Then
        assertFalse(result);
        verify(contentLikeMapper, never()).existsByUserAndContentAndType(anyLong(), anyLong(), any());
    }

    // ==================== getLikeCount 方法测试 ====================

    @Test
    void testGetLikeCount_Success() {
        // Given
        int expectedCount = 5;
        when(contentLikeMapper.countByContentAndType(testContentId, ForumRelationType.LIKE))
            .thenReturn(expectedCount);

        // When
        int result = forumContentLikeService.getLikeCount(testContentId);

        // Then
        assertEquals(expectedCount, result);
        verify(contentLikeMapper, times(1)).countByContentAndType(testContentId, ForumRelationType.LIKE);
    }

    @Test
    void testGetLikeCount_NullContentId_ReturnsZero() {
        // Given
        Long contentId = null;

        // When
        int result = forumContentLikeService.getLikeCount(contentId);

        // Then
        assertEquals(0, result);
        verify(contentLikeMapper, never()).countByContentAndType(anyLong(), any());
    }

    @Test
    void testGetLikeCount_ZeroLikes_ReturnsZero() {
        // Given
        when(contentLikeMapper.countByContentAndType(testContentId, ForumRelationType.LIKE))
            .thenReturn(0);

        // When
        int result = forumContentLikeService.getLikeCount(testContentId);

        // Then
        assertEquals(0, result);
        verify(contentLikeMapper, times(1)).countByContentAndType(testContentId, ForumRelationType.LIKE);
    }

    // ==================== getLikedUserIds 方法测试 ====================

    @Test
    void testGetLikedUserIds_Success() {
        // Given
        List<Long> expectedUserIds = Arrays.asList(1L, 2L, 3L);
        when(contentLikeMapper.findUserIdsByContentAndType(testContentId, ForumRelationType.LIKE))
            .thenReturn(expectedUserIds);

        // When
        List<Long> result = forumContentLikeService.getLikedUserIds(testContentId);

        // Then
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals(expectedUserIds, result);
        verify(contentLikeMapper, times(1)).findUserIdsByContentAndType(testContentId, ForumRelationType.LIKE);
    }

    @Test
    void testGetLikedUserIds_EmptyList() {
        // Given
        when(contentLikeMapper.findUserIdsByContentAndType(testContentId, ForumRelationType.LIKE))
            .thenReturn(new ArrayList<>());

        // When
        List<Long> result = forumContentLikeService.getLikedUserIds(testContentId);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(contentLikeMapper, times(1)).findUserIdsByContentAndType(testContentId, ForumRelationType.LIKE);
    }

    @Test
    void testGetLikedUserIds_NullContentId_ThrowsException() {
        // Given
        Long contentId = null;

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> forumContentLikeService.getLikedUserIds(contentId));
        assertEquals("内容ID不能为空", exception.getMessage());

        verify(contentLikeMapper, never()).findUserIdsByContentAndType(anyLong(), any());
    }

    // ==================== getUserLikedContentIds 方法测试 ====================

    @Test
    void testGetUserLikedContentIds_Success() {
        // Given
        List<Long> expectedContentIds = Arrays.asList(1L, 2L, 3L);
        when(contentLikeMapper.findContentIdsByUserAndType(testUserId, ForumRelationType.LIKE))
            .thenReturn(expectedContentIds);

        // When
        List<Long> result = forumContentLikeService.getUserLikedContentIds(testUserId);

        // Then
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals(expectedContentIds, result);
        verify(contentLikeMapper, times(1)).findContentIdsByUserAndType(testUserId, ForumRelationType.LIKE);
    }

    @Test
    void testGetUserLikedContentIds_EmptyList() {
        // Given
        when(contentLikeMapper.findContentIdsByUserAndType(testUserId, ForumRelationType.LIKE))
            .thenReturn(new ArrayList<>());

        // When
        List<Long> result = forumContentLikeService.getUserLikedContentIds(testUserId);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(contentLikeMapper, times(1)).findContentIdsByUserAndType(testUserId, ForumRelationType.LIKE);
    }

    @Test
    void testGetUserLikedContentIds_NullUserId_ThrowsException() {
        // Given
        Long userId = null;

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> forumContentLikeService.getUserLikedContentIds(userId));
        assertEquals("用户ID不能为空", exception.getMessage());

        verify(contentLikeMapper, never()).findContentIdsByUserAndType(anyLong(), any());
    }

    // ==================== batchCheckLikeStatus 方法测试 ====================

    @Test
    void testBatchCheckLikeStatus_Success() {
        // Given
        List<Long> contentIds = Arrays.asList(1L, 2L, 3L);
        List<Long> likedContentIds = Arrays.asList(1L, 3L);
        when(contentLikeMapper.findLikedContentIdsByUserAndType(testUserId, contentIds, ForumRelationType.LIKE))
            .thenReturn(likedContentIds);

        // When
        Map<Long, Boolean> result = forumContentLikeService.batchCheckLikeStatus(testUserId, contentIds);

        // Then
        assertNotNull(result);
        assertEquals(3, result.size());
        assertTrue(result.get(1L));
        assertFalse(result.get(2L));
        assertTrue(result.get(3L));
        verify(contentLikeMapper, times(1)).findLikedContentIdsByUserAndType(testUserId, contentIds, ForumRelationType.LIKE);
    }

    @Test
    void testBatchCheckLikeStatus_EmptyContentIds_ReturnsEmptyMap() {
        // Given
        List<Long> contentIds = new ArrayList<>();

        // When
        Map<Long, Boolean> result = forumContentLikeService.batchCheckLikeStatus(testUserId, contentIds);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        // 实际实现中，空列表会直接返回空Map，不会调用mapper
        verify(contentLikeMapper, never()).findLikedContentIdsByUserAndType(anyLong(), anyList(), any());
    }

    @Test
    void testBatchCheckLikeStatus_NullUserId_ReturnsEmptyMap() {
        // Given
        Long userId = null;
        List<Long> contentIds = Arrays.asList(1L, 2L);

        // When
        Map<Long, Boolean> result = forumContentLikeService.batchCheckLikeStatus(userId, contentIds);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(contentLikeMapper, never()).findLikedContentIdsByUserAndType(anyLong(), anyList(), any());
    }

    @Test
    void testBatchCheckLikeStatus_NullContentIds_ReturnsEmptyMap() {
        // Given
        List<Long> contentIds = null;

        // When
        Map<Long, Boolean> result = forumContentLikeService.batchCheckLikeStatus(testUserId, contentIds);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(contentLikeMapper, never()).findLikedContentIdsByUserAndType(anyLong(), anyList(), any());
    }

    // ==================== batchGetLikeCounts 方法测试 ====================

    @Test
    void testBatchGetLikeCounts_Success() {
        // Given
        List<Long> contentIds = Arrays.asList(1L, 2L, 3L);
        Map<Long, Integer> expectedCounts = new HashMap<>();
        expectedCounts.put(1L, 5);
        expectedCounts.put(2L, 3);
        expectedCounts.put(3L, 0);
        when(metricMapper.getBatchMetrics(contentIds, "like_count")).thenReturn(expectedCounts);

        // When
        Map<Long, Integer> result = forumContentLikeService.batchGetLikeCounts(contentIds);

        // Then
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals(5, result.get(1L));
        assertEquals(3, result.get(2L));
        assertEquals(0, result.get(3L));
        verify(metricMapper, times(1)).getBatchMetrics(contentIds, "like_count");
    }

    @Test
    void testBatchGetLikeCounts_EmptyContentIds_ReturnsEmptyMap() {
        // Given
        List<Long> contentIds = new ArrayList<>();

        // When
        Map<Long, Integer> result = forumContentLikeService.batchGetLikeCounts(contentIds);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        // 实际实现中，空列表会直接返回空Map，不会调用mapper
        verify(metricMapper, never()).getBatchMetrics(anyList(), anyString());
    }

    @Test
    void testBatchGetLikeCounts_NullContentIds_ReturnsEmptyMap() {
        // Given
        List<Long> contentIds = null;

        // When
        Map<Long, Integer> result = forumContentLikeService.batchGetLikeCounts(contentIds);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(metricMapper, never()).getBatchMetrics(anyList(), anyString());
    }

    // ==================== getUserRecentLikes 方法测试 ====================

    @Test
    void testGetUserRecentLikes_Success() {
        // Given
        int limit = 5;
        List<UserContentRelation> expectedRelations = Arrays.asList(testRelation);
        when(contentLikeMapper.findRecentByUserAndType(testUserId, ForumRelationType.LIKE, limit))
            .thenReturn(expectedRelations);

        // When
        List<UserContentRelation> result = forumContentLikeService.getUserRecentLikes(testUserId, limit);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testRelation, result.get(0));
        verify(contentLikeMapper, times(1)).findRecentByUserAndType(testUserId, ForumRelationType.LIKE, limit);
    }

    @Test
    void testGetUserRecentLikes_EmptyResult() {
        // Given
        int limit = 5;
        when(contentLikeMapper.findRecentByUserAndType(testUserId, ForumRelationType.LIKE, limit))
            .thenReturn(new ArrayList<>());

        // When
        List<UserContentRelation> result = forumContentLikeService.getUserRecentLikes(testUserId, limit);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(contentLikeMapper, times(1)).findRecentByUserAndType(testUserId, ForumRelationType.LIKE, limit);
    }

    @Test
    void testGetUserRecentLikes_NullUserId_ThrowsException() {
        // Given
        Long userId = null;
        int limit = 5;

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> forumContentLikeService.getUserRecentLikes(userId, limit));
        assertEquals("用户ID不能为空", exception.getMessage());

        verify(contentLikeMapper, never()).findRecentByUserAndType(anyLong(), any(), anyInt());
    }

    // ==================== getContentRecentLikes 方法测试 ====================

    @Test
    void testGetContentRecentLikes_Success() {
        // Given
        int limit = 5;
        List<UserContentRelation> expectedRelations = Arrays.asList(testRelation);
        when(contentLikeMapper.findRecentByContentAndType(testContentId, ForumRelationType.LIKE, limit))
            .thenReturn(expectedRelations);

        // When
        List<UserContentRelation> result = forumContentLikeService.getContentRecentLikes(testContentId, limit);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testRelation, result.get(0));
        verify(contentLikeMapper, times(1)).findRecentByContentAndType(testContentId, ForumRelationType.LIKE, limit);
    }

    @Test
    void testGetContentRecentLikes_EmptyResult() {
        // Given
        int limit = 5;
        when(contentLikeMapper.findRecentByContentAndType(testContentId, ForumRelationType.LIKE, limit))
            .thenReturn(new ArrayList<>());

        // When
        List<UserContentRelation> result = forumContentLikeService.getContentRecentLikes(testContentId, limit);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(contentLikeMapper, times(1)).findRecentByContentAndType(testContentId, ForumRelationType.LIKE, limit);
    }

    @Test
    void testGetContentRecentLikes_NullContentId_ThrowsException() {
        // Given
        Long contentId = null;
        int limit = 5;

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> forumContentLikeService.getContentRecentLikes(contentId, limit));
        assertEquals("内容ID不能为空", exception.getMessage());

        verify(contentLikeMapper, never()).findRecentByContentAndType(anyLong(), any(), anyInt());
    }

    // ==================== syncLikeCount 方法测试 ====================

    @Test
    void testSyncLikeCount_Success() {
        // Given
        int actualCount = 5;
        when(contentLikeMapper.countByContentAndType(testContentId, ForumRelationType.LIKE))
            .thenReturn(actualCount);
        when(metricMapper.setMetricValue(testContentId, "like_count", actualCount)).thenReturn(1);

        // When
        forumContentLikeService.syncLikeCount(testContentId);

        // Then
        verify(contentLikeMapper, times(1)).countByContentAndType(testContentId, ForumRelationType.LIKE);
        verify(metricMapper, times(1)).setMetricValue(testContentId, "like_count", actualCount);
    }

    @Test
    void testSyncLikeCount_NullContentId_ThrowsException() {
        // Given
        Long contentId = null;

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> forumContentLikeService.syncLikeCount(contentId));
        assertEquals("内容ID不能为空", exception.getMessage());

        verify(contentLikeMapper, never()).countByContentAndType(anyLong(), any());
    }

    @Test
    void testSyncLikeCount_ZeroLikes() {
        // Given
        int actualCount = 0;
        when(contentLikeMapper.countByContentAndType(testContentId, ForumRelationType.LIKE))
            .thenReturn(actualCount);
        when(metricMapper.setMetricValue(testContentId, "like_count", actualCount)).thenReturn(1);

        // When
        forumContentLikeService.syncLikeCount(testContentId);

        // Then
        verify(contentLikeMapper, times(1)).countByContentAndType(testContentId, ForumRelationType.LIKE);
        verify(metricMapper, times(1)).setMetricValue(testContentId, "like_count", actualCount);
    }

    // ==================== batchSyncLikeCounts 方法测试 ====================

    @Test
    void testBatchSyncLikeCounts_Success() {
        // Given
        List<Long> contentIds = Arrays.asList(1L, 2L, 3L);
        when(contentLikeMapper.countByContentAndType(anyLong(), eq(ForumRelationType.LIKE)))
            .thenReturn(5);
        when(metricMapper.setMetricValue(anyLong(), eq("like_count"), anyInt()))
            .thenReturn(1);

        // When
        forumContentLikeService.batchSyncLikeCounts(contentIds);

        // Then
        verify(contentLikeMapper, times(3)).countByContentAndType(anyLong(), eq(ForumRelationType.LIKE));
        verify(metricMapper, times(3)).setMetricValue(anyLong(), eq("like_count"), anyInt());
    }

    @Test
    void testBatchSyncLikeCounts_EmptyContentIds_DoesNothing() {
        // Given
        List<Long> contentIds = new ArrayList<>();

        // When
        forumContentLikeService.batchSyncLikeCounts(contentIds);

        // Then
        verify(contentLikeMapper, never()).countByContentAndType(anyLong(), any());
        verify(metricMapper, never()).setMetricValue(anyLong(), anyString(), anyInt());
    }

    @Test
    void testBatchSyncLikeCounts_NullContentIds_DoesNothing() {
        // Given
        List<Long> contentIds = null;

        // When
        forumContentLikeService.batchSyncLikeCounts(contentIds);

        // Then
        verify(contentLikeMapper, never()).countByContentAndType(anyLong(), any());
        verify(metricMapper, never()).setMetricValue(anyLong(), anyString(), anyInt());
    }

    // ==================== getTopLikedContents 方法测试 ====================

    @Test
    void testGetTopLikedContents_Success() {
        // Given
        int limit = 10;
        List<Long> expectedContentIds = Arrays.asList(1L, 2L, 3L);
        when(metricMapper.findTopContentsByMetric("like_count", limit))
            .thenReturn(expectedContentIds);

        // When
        List<Long> result = forumContentLikeService.getTopLikedContents(limit);

        // Then
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals(expectedContentIds, result);
        verify(metricMapper, times(1)).findTopContentsByMetric("like_count", limit);
    }

    @Test
    void testGetTopLikedContents_EmptyResult() {
        // Given
        int limit = 10;
        when(metricMapper.findTopContentsByMetric("like_count", limit))
            .thenReturn(new ArrayList<>());

        // When
        List<Long> result = forumContentLikeService.getTopLikedContents(limit);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(metricMapper, times(1)).findTopContentsByMetric("like_count", limit);
    }

    // ==================== 集成测试场景 ====================

    @Test
    void testLikeAndUnlikeWorkflow() {
        // Given - 用户点赞内容
        when(contentLikeMapper.existsByUserAndContentAndType(testUserId, testContentId, ForumRelationType.LIKE))
            .thenReturn(false);
        when(contentLikeMapper.insert(any(UserContentRelation.class))).thenReturn(1);

        // When - 点赞
        boolean likeResult = forumContentLikeService.likeContent(testContentId, testUserId);

        // Then - 验证点赞成功
        assertTrue(likeResult);

        // Given - 用户取消点赞
        when(contentLikeMapper.existsByUserAndContentAndType(testUserId, testContentId, ForumRelationType.LIKE))
            .thenReturn(true);
        when(contentLikeMapper.deleteByUserAndContentAndType(testUserId, testContentId, ForumRelationType.LIKE))
            .thenReturn(1);

        // When - 取消点赞
        boolean unlikeResult = forumContentLikeService.unlikeContent(testContentId, testUserId);

        // Then - 验证取消点赞成功
        assertTrue(unlikeResult);

        verify(contentLikeMapper, times(2)).existsByUserAndContentAndType(testUserId, testContentId, ForumRelationType.LIKE);
        verify(contentLikeMapper, times(1)).insert(any(UserContentRelation.class));
        verify(contentLikeMapper, times(1)).deleteByUserAndContentAndType(testUserId, testContentId, ForumRelationType.LIKE);
    }

    @Test
    void testToggleLikeWorkflow() {
        // Given - 初始状态：未点赞
        when(contentLikeMapper.existsByUserAndContentAndType(testUserId, testContentId, ForumRelationType.LIKE))
            .thenReturn(false);
        when(contentLikeMapper.insert(any(UserContentRelation.class))).thenReturn(1);

        // When - 第一次切换（点赞）
        boolean firstToggle = forumContentLikeService.toggleLike(testContentId, testUserId);

        // Then - 验证点赞成功
        assertTrue(firstToggle);

        // Given - 状态：已点赞
        when(contentLikeMapper.existsByUserAndContentAndType(testUserId, testContentId, ForumRelationType.LIKE))
            .thenReturn(true);
        when(contentLikeMapper.deleteByUserAndContentAndType(testUserId, testContentId, ForumRelationType.LIKE))
            .thenReturn(1);

        // When - 第二次切换（取消点赞）
        boolean secondToggle = forumContentLikeService.toggleLike(testContentId, testUserId);

        // Then - 验证取消点赞成功
        assertFalse(secondToggle);

        // 每个toggleLike调用会调用existsByUserAndContentAndType两次（一次在toggleLike中，一次在调用的方法中）
        verify(contentLikeMapper, times(4)).existsByUserAndContentAndType(testUserId, testContentId, ForumRelationType.LIKE);
        verify(contentLikeMapper, times(1)).insert(any(UserContentRelation.class));
        verify(contentLikeMapper, times(1)).deleteByUserAndContentAndType(testUserId, testContentId, ForumRelationType.LIKE);
    }
}
