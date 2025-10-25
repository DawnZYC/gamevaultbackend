package com.sg.nusiss.gamevaultbackend.service.forum;

import com.sg.nusiss.gamevaultbackend.entity.forum.ForumContent;
import com.sg.nusiss.gamevaultbackend.mapper.forum.ForumContentMapper;
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
 * @ClassName ForumPostServiceTest
 * @Author Hou Zheyu
 * @Date 2025/10/25
 * @Description ForumPostService单元测试类，覆盖所有方法
 */
@ExtendWith(MockitoExtension.class)
class ForumPostServiceTest {

    @Mock
    private ForumContentMapper contentMapper;

    @Mock
    private ForumMetricMapper metricMapper;

    @Mock
    private ForumContentLikeService contentLikeService;

    @InjectMocks
    private ForumPostService forumPostService;

    private ForumContent testPost;
    private ForumContent testReply;
    private Long testUserId;
    private Long testPostId;

    @BeforeEach
    void setUp() {
        testUserId = 1L;
        testPostId = 1L;

        // 创建测试帖子
        testPost = new ForumContent();
        testPost.setContentId(testPostId);
        testPost.setContentType("post");
        testPost.setTitle("测试帖子");
        testPost.setBody("测试内容");
        testPost.setAuthorId(testUserId);
        testPost.setStatus("active");
        testPost.setCreatedDate(LocalDateTime.now());
        testPost.setUpdatedDate(LocalDateTime.now());
        testPost.setLikeCount(0);
        testPost.setViewCount(0);
        testPost.setReplyCount(0);

        // 创建测试回复
        testReply = new ForumContent();
        testReply.setContentId(2L);
        testReply.setContentType("reply");
        testReply.setBody("测试回复");
        testReply.setAuthorId(testUserId);
        testReply.setParentId(testPostId);
        testReply.setStatus("active");
        testReply.setCreatedDate(LocalDateTime.now());
        testReply.setUpdatedDate(LocalDateTime.now());
        testReply.setLikeCount(0);
    }

    // ==================== createPost 方法测试 ====================

    @Test
    void testCreatePost_Success() {
        // Given
        String title = "测试帖子标题";
        String body = "测试帖子内容";
        Long authorId = testUserId;

        when(contentMapper.insert(any(ForumContent.class))).thenAnswer(invocation -> {
            ForumContent post = invocation.getArgument(0);
            post.setContentId(1L); // 模拟数据库返回的ID
            return 1;
        });
        when(metricMapper.setMetricValue(anyLong(), anyString(), anyInt())).thenReturn(1);

        // When
        ForumContent result = forumPostService.createPost(title, body, authorId);

        // Then
        assertNotNull(result);
        assertEquals("post", result.getContentType());
        assertEquals(title, result.getTitle());
        assertEquals(body, result.getBody());
        assertEquals(authorId, result.getAuthorId());
        assertEquals("active", result.getStatus());

        verify(contentMapper, times(1)).insert(any(ForumContent.class));
        verify(metricMapper, times(3)).setMetricValue(anyLong(), anyString(), anyInt());
    }

    @Test
    void testCreatePost_EmptyTitle_ThrowsException() {
        // Given
        String title = "";
        String body = "测试内容";
        Long authorId = testUserId;

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> forumPostService.createPost(title, body, authorId));
        assertEquals("帖子标题不能为空", exception.getMessage());
    }

    @Test
    void testCreatePost_EmptyBody_ThrowsException() {
        // Given
        String title = "测试标题";
        String body = "";
        Long authorId = testUserId;

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> forumPostService.createPost(title, body, authorId));
        assertEquals("帖子内容不能为空", exception.getMessage());
    }

    @Test
    void testCreatePost_NullAuthorId_ThrowsException() {
        // Given
        String title = "测试标题";
        String body = "测试内容";
        Long authorId = null;

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> forumPostService.createPost(title, body, authorId));
        assertEquals("作者ID不能为空", exception.getMessage());
    }

    @Test
    void testCreatePost_InsertFails_ThrowsException() {
        // Given
        String title = "测试标题";
        String body = "测试内容";
        Long authorId = testUserId;

        when(contentMapper.insert(any(ForumContent.class))).thenReturn(0);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> forumPostService.createPost(title, body, authorId));
        assertEquals("创建帖子失败", exception.getMessage());
    }

    // ==================== getPostById 方法测试 ====================

    @Test
    void testGetPostById_Success() {
        // Given
        Long postId = testPostId;
        Long currentUserId = testUserId;

        when(contentMapper.findById(postId)).thenReturn(testPost);
        when(contentLikeService.isLiked(postId, currentUserId)).thenReturn(true);

        // When
        ForumContent result = forumPostService.getPostById(postId, currentUserId);

        // Then
        assertNotNull(result);
        assertEquals(testPostId, result.getContentId());
        assertTrue(result.getIsLikedByCurrentUser());

        verify(contentMapper, times(1)).findById(postId);
        verify(contentLikeService, times(1)).isLiked(postId, currentUserId);
    }

    @Test
    void testGetPostById_PostNotFound_ThrowsException() {
        // Given
        Long postId = testPostId;
        Long currentUserId = testUserId;

        when(contentMapper.findById(postId)).thenReturn(null);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> forumPostService.getPostById(postId, currentUserId));
        assertEquals("帖子不存在", exception.getMessage());
    }

    @Test
    void testGetPostById_NullCurrentUserId() {
        // Given
        Long postId = testPostId;
        Long currentUserId = null;

        when(contentMapper.findById(postId)).thenReturn(testPost);

        // When
        ForumContent result = forumPostService.getPostById(postId, currentUserId);

        // Then
        assertNotNull(result);
        assertNull(result.getIsLikedByCurrentUser());
        verify(contentLikeService, never()).isLiked(anyLong(), anyLong());
    }

    // ==================== getPostList 方法测试 ====================

    @Test
    void testGetPostList_Success() {
        // Given
        int page = 0;
        int size = 10;
        Long currentUserId = testUserId;
        List<ForumContent> posts = Arrays.asList(testPost);
        Map<Long, Boolean> likeStatus = new HashMap<>();
        likeStatus.put(testPostId, true);

        when(contentMapper.findActivePosts(anyInt(), anyInt())).thenReturn(posts);
        when(contentLikeService.batchCheckLikeStatus(currentUserId, Arrays.asList(testPostId)))
            .thenReturn(likeStatus);

        // When
        List<ForumContent> result = forumPostService.getPostList(page, size, currentUserId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.get(0).getIsLikedByCurrentUser());

        verify(contentMapper, times(1)).findActivePosts(page * size, size);
        verify(contentLikeService, times(1)).batchCheckLikeStatus(currentUserId, Arrays.asList(testPostId));
    }

    @Test
    void testGetPostList_EmptyList() {
        // Given
        int page = 0;
        int size = 10;
        Long currentUserId = testUserId;

        when(contentMapper.findActivePosts(anyInt(), anyInt())).thenReturn(new ArrayList<>());

        // When
        List<ForumContent> result = forumPostService.getPostList(page, size, currentUserId);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(contentLikeService, never()).batchCheckLikeStatus(anyLong(), anyList());
    }

    @Test
    void testGetPostList_NullCurrentUserId() {
        // Given
        int page = 0;
        int size = 10;
        Long currentUserId = null;
        List<ForumContent> posts = Arrays.asList(testPost);

        when(contentMapper.findActivePosts(anyInt(), anyInt())).thenReturn(posts);

        // When
        List<ForumContent> result = forumPostService.getPostList(page, size, currentUserId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertNull(result.get(0).getIsLikedByCurrentUser());
        verify(contentLikeService, never()).batchCheckLikeStatus(anyLong(), anyList());
    }

    // ==================== getPostCount 方法测试 ====================

    @Test
    void testGetPostCount_Success() {
        // Given
        int expectedCount = 5;
        when(contentMapper.countActivePosts()).thenReturn(expectedCount);

        // When
        int result = forumPostService.getPostCount();

        // Then
        assertEquals(expectedCount, result);
        verify(contentMapper, times(1)).countActivePosts();
    }

    // ==================== searchPosts 方法测试 ====================

    @Test
    void testSearchPosts_Success() {
        // Given
        String keyword = "测试";
        int page = 0;
        int size = 10;
        Long currentUserId = testUserId;
        List<ForumContent> posts = Arrays.asList(testPost);
        Map<Long, Boolean> likeStatus = new HashMap<>();
        likeStatus.put(testPostId, true);

        when(contentMapper.searchPosts(keyword, page * size, size)).thenReturn(posts);
        when(contentLikeService.batchCheckLikeStatus(currentUserId, Arrays.asList(testPostId)))
            .thenReturn(likeStatus);

        // When
        List<ForumContent> result = forumPostService.searchPosts(keyword, page, size, currentUserId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.get(0).getIsLikedByCurrentUser());

        verify(contentMapper, times(1)).searchPosts(keyword, page * size, size);
        verify(contentLikeService, times(1)).batchCheckLikeStatus(currentUserId, Arrays.asList(testPostId));
    }

    @Test
    void testSearchPosts_EmptyKeyword_CallsGetPostList() {
        // Given
        String keyword = "";
        int page = 0;
        int size = 10;
        Long currentUserId = testUserId;
        List<ForumContent> posts = Arrays.asList(testPost);

        when(contentMapper.findActivePosts(anyInt(), anyInt())).thenReturn(posts);
        when(contentLikeService.batchCheckLikeStatus(anyLong(), anyList())).thenReturn(new HashMap<>());

        // When
        List<ForumContent> result = forumPostService.searchPosts(keyword, page, size, currentUserId);

        // Then
        assertNotNull(result);
        verify(contentMapper, times(1)).findActivePosts(page * size, size);
    }

    @Test
    void testSearchPosts_InvalidPageAndSize() {
        // Given
        String keyword = "测试";
        int page = -1;
        int size = 0;
        Long currentUserId = testUserId;

        when(contentMapper.searchPosts(keyword, 0, 20)).thenReturn(new ArrayList<>());

        // When
        List<ForumContent> result = forumPostService.searchPosts(keyword, page, size, currentUserId);

        // Then
        assertNotNull(result);
        verify(contentMapper, times(1)).searchPosts(keyword, 0, 20);
    }

    // ==================== getSearchCount 方法测试 ====================

    @Test
    void testGetSearchCount_Success() {
        // Given
        String keyword = "测试";
        int expectedCount = 3;
        when(contentMapper.countSearchPosts(keyword)).thenReturn(expectedCount);

        // When
        int result = forumPostService.getSearchCount(keyword);

        // Then
        assertEquals(expectedCount, result);
        verify(contentMapper, times(1)).countSearchPosts(keyword);
    }

    @Test
    void testGetSearchCount_EmptyKeyword_CallsGetPostCount() {
        // Given
        String keyword = "";
        int expectedCount = 5;
        when(contentMapper.countActivePosts()).thenReturn(expectedCount);

        // When
        int result = forumPostService.getSearchCount(keyword);

        // Then
        assertEquals(expectedCount, result);
        verify(contentMapper, times(1)).countActivePosts();
    }

    // ==================== incrementViewCount 方法测试 ====================

    @Test
    void testIncrementViewCount_Success() {
        // Given
        Long postId = testPostId;
        when(contentMapper.findById(postId)).thenReturn(testPost);
        when(metricMapper.incrementMetric(postId, "view_count", 1)).thenReturn(1);

        // When
        forumPostService.incrementViewCount(postId);

        // Then
        verify(contentMapper, times(1)).findById(postId);
        verify(metricMapper, times(1)).incrementMetric(postId, "view_count", 1);
    }

    @Test
    void testIncrementViewCount_NullPostId_ThrowsException() {
        // Given
        Long postId = null;

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> forumPostService.incrementViewCount(postId));
        assertEquals("帖子ID不能为空", exception.getMessage());
    }

    @Test
    void testIncrementViewCount_PostNotFound_ThrowsException() {
        // Given
        Long postId = testPostId;
        when(contentMapper.findById(postId)).thenReturn(null);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> forumPostService.incrementViewCount(postId));
        assertEquals("帖子不存在", exception.getMessage());
    }

    @Test
    void testIncrementViewCount_NotPost_ThrowsException() {
        // Given
        Long postId = testPostId;
        testPost.setContentType("reply");
        when(contentMapper.findById(postId)).thenReturn(testPost);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> forumPostService.incrementViewCount(postId));
        assertEquals("帖子不存在", exception.getMessage());
    }

    // ==================== deletePost 方法测试 ====================

    @Test
    void testDeletePost_Success() {
        // Given
        Long postId = testPostId;
        Long userId = testUserId;
        when(contentMapper.findById(postId)).thenReturn(testPost);
        when(contentMapper.softDelete(postId)).thenReturn(1);

        // When
        forumPostService.deletePost(postId, userId);

        // Then
        verify(contentMapper, times(1)).findById(postId);
        verify(contentMapper, times(1)).softDelete(postId);
    }

    @Test
    void testDeletePost_NullPostId_ThrowsException() {
        // Given
        Long postId = null;
        Long userId = testUserId;

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> forumPostService.deletePost(postId, userId));
        assertEquals("帖子ID不能为空", exception.getMessage());
    }

    @Test
    void testDeletePost_PostNotFound_ThrowsException() {
        // Given
        Long postId = testPostId;
        Long userId = testUserId;
        when(contentMapper.findById(postId)).thenReturn(null);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> forumPostService.deletePost(postId, userId));
        assertEquals("帖子不存在", exception.getMessage());
    }

    @Test
    void testDeletePost_NotAuthor_ThrowsException() {
        // Given
        Long postId = testPostId;
        Long userId = 2L; // 不同的用户ID
        when(contentMapper.findById(postId)).thenReturn(testPost);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> forumPostService.deletePost(postId, userId));
        assertEquals("没有权限删除此帖子", exception.getMessage());
    }

    // ==================== updatePost 方法测试 ====================

    @Test
    void testUpdatePost_Success() {
        // Given
        Long postId = testPostId;
        String newTitle = "新标题";
        String newBody = "新内容";
        Long userId = testUserId;
        when(contentMapper.findById(postId)).thenReturn(testPost);
        when(contentMapper.update(any(ForumContent.class))).thenReturn(1);

        // When
        ForumContent result = forumPostService.updatePost(postId, newTitle, newBody, userId);

        // Then
        assertNotNull(result);
        assertEquals(newTitle, result.getTitle());
        assertEquals(newBody, result.getBody());
        assertNotNull(result.getUpdatedDate());

        verify(contentMapper, times(1)).findById(postId);
        verify(contentMapper, times(1)).update(any(ForumContent.class));
    }

    @Test
    void testUpdatePost_NullPostId_ThrowsException() {
        // Given
        Long postId = null;
        String newTitle = "新标题";
        String newBody = "新内容";
        Long userId = testUserId;

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> forumPostService.updatePost(postId, newTitle, newBody, userId));
        assertEquals("帖子ID不能为空", exception.getMessage());
    }

    @Test
    void testUpdatePost_PostNotFound_ThrowsException() {
        // Given
        Long postId = testPostId;
        String newTitle = "新标题";
        String newBody = "新内容";
        Long userId = testUserId;
        when(contentMapper.findById(postId)).thenReturn(null);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> forumPostService.updatePost(postId, newTitle, newBody, userId));
        assertEquals("帖子不存在", exception.getMessage());
    }

    @Test
    void testUpdatePost_NotAuthor_ThrowsException() {
        // Given
        Long postId = testPostId;
        String newTitle = "新标题";
        String newBody = "新内容";
        Long userId = 2L; // 不同的用户ID
        when(contentMapper.findById(postId)).thenReturn(testPost);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> forumPostService.updatePost(postId, newTitle, newBody, userId));
        assertEquals("没有权限编辑此帖子", exception.getMessage());
    }

    @Test
    void testUpdatePost_UpdateFails_ThrowsException() {
        // Given
        Long postId = testPostId;
        String newTitle = "新标题";
        String newBody = "新内容";
        Long userId = testUserId;
        when(contentMapper.findById(postId)).thenReturn(testPost);
        when(contentMapper.update(any(ForumContent.class))).thenReturn(0);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> forumPostService.updatePost(postId, newTitle, newBody, userId));
        assertEquals("更新帖子失败", exception.getMessage());
    }

    // ==================== getPostsByAuthorId 方法测试 ====================

    @Test
    void testGetPostsByAuthorId_Success() {
        // Given
        Long authorId = testUserId;
        int page = 0;
        int size = 10;
        Long currentUserId = testUserId;
        List<ForumContent> posts = Arrays.asList(testPost);
        Map<Long, Boolean> likeStatus = new HashMap<>();
        likeStatus.put(testPostId, true);

        when(contentMapper.selectActiveByAuthorId(authorId, page * size, size)).thenReturn(posts);
        when(contentLikeService.batchCheckLikeStatus(currentUserId, Arrays.asList(testPostId)))
            .thenReturn(likeStatus);

        // When
        List<ForumContent> result = forumPostService.getPostsByAuthorId(authorId, page, size, currentUserId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.get(0).getIsLikedByCurrentUser());

        verify(contentMapper, times(1)).selectActiveByAuthorId(authorId, page * size, size);
        verify(contentLikeService, times(1)).batchCheckLikeStatus(currentUserId, Arrays.asList(testPostId));
    }

    @Test
    void testGetPostsByAuthorId_NullAuthorId_ThrowsException() {
        // Given
        Long authorId = null;
        int page = 0;
        int size = 10;
        Long currentUserId = testUserId;

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> forumPostService.getPostsByAuthorId(authorId, page, size, currentUserId));
        assertEquals("作者ID不能为空", exception.getMessage());
    }

    @Test
    void testGetPostsByAuthorId_InvalidPageAndSize() {
        // Given
        Long authorId = testUserId;
        int page = -1;
        int size = 0;
        Long currentUserId = testUserId;

        when(contentMapper.selectActiveByAuthorId(authorId, 0, 20)).thenReturn(new ArrayList<>());

        // When
        List<ForumContent> result = forumPostService.getPostsByAuthorId(authorId, page, size, currentUserId);

        // Then
        assertNotNull(result);
        verify(contentMapper, times(1)).selectActiveByAuthorId(authorId, 0, 20);
    }

    // ==================== getPostCountByAuthorId 方法测试 ====================

    @Test
    void testGetPostCountByAuthorId_Success() {
        // Given
        Long authorId = testUserId;
        int expectedCount = 3;
        when(contentMapper.countActiveByAuthorId(authorId)).thenReturn(expectedCount);

        // When
        int result = forumPostService.getPostCountByAuthorId(authorId);

        // Then
        assertEquals(expectedCount, result);
        verify(contentMapper, times(1)).countActiveByAuthorId(authorId);
    }

    @Test
    void testGetPostCountByAuthorId_NullAuthorId_ThrowsException() {
        // Given
        Long authorId = null;

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> forumPostService.getPostCountByAuthorId(authorId));
        assertEquals("作者ID不能为空", exception.getMessage());
    }

    // ==================== createReply 方法测试 ====================

    @Test
    void testCreateReply_Success() {
        // Given
        Long parentId = testPostId;
        String body = "测试回复内容";
        Long authorId = testUserId;

        when(contentMapper.findById(parentId)).thenReturn(testPost);
        when(contentMapper.insert(any(ForumContent.class))).thenAnswer(invocation -> {
            ForumContent reply = invocation.getArgument(0);
            reply.setContentId(2L); // 模拟数据库返回的ID
            return 1;
        });
        when(metricMapper.setMetricValue(anyLong(), eq("like_count"), eq(0))).thenReturn(1);
        when(metricMapper.incrementMetric(parentId, "reply_count", 1)).thenReturn(1);

        // When
        ForumContent result = forumPostService.createReply(parentId, body, authorId);

        // Then
        assertNotNull(result);
        assertEquals("reply", result.getContentType());
        assertEquals(body, result.getBody());
        assertEquals(authorId, result.getAuthorId());
        assertEquals(parentId, result.getParentId());

        verify(contentMapper, times(1)).findById(parentId);
        verify(contentMapper, times(1)).insert(any(ForumContent.class));
        verify(metricMapper, times(1)).setMetricValue(anyLong(), eq("like_count"), eq(0));
        verify(metricMapper, times(1)).incrementMetric(parentId, "reply_count", 1);
    }

    @Test
    void testCreateReply_NullParentId_ThrowsException() {
        // Given
        Long parentId = null;
        String body = "测试回复内容";
        Long authorId = testUserId;

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> forumPostService.createReply(parentId, body, authorId));
        assertEquals("父内容ID不能为空", exception.getMessage());
    }

    @Test
    void testCreateReply_EmptyBody_ThrowsException() {
        // Given
        Long parentId = testPostId;
        String body = "";
        Long authorId = testUserId;

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> forumPostService.createReply(parentId, body, authorId));
        assertEquals("回复内容不能为空", exception.getMessage());
    }

    @Test
    void testCreateReply_NullAuthorId_ThrowsException() {
        // Given
        Long parentId = testPostId;
        String body = "测试回复内容";
        Long authorId = null;

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> forumPostService.createReply(parentId, body, authorId));
        assertEquals("作者ID不能为空", exception.getMessage());
    }

    @Test
    void testCreateReply_ParentNotFound_ThrowsException() {
        // Given
        Long parentId = testPostId;
        String body = "测试回复内容";
        Long authorId = testUserId;

        when(contentMapper.findById(parentId)).thenReturn(null);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> forumPostService.createReply(parentId, body, authorId));
        assertEquals("父内容不存在", exception.getMessage());
    }

    @Test
    void testCreateReply_InsertFails_ThrowsException() {
        // Given
        Long parentId = testPostId;
        String body = "测试回复内容";
        Long authorId = testUserId;

        when(contentMapper.findById(parentId)).thenReturn(testPost);
        when(contentMapper.insert(any(ForumContent.class))).thenReturn(0);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> forumPostService.createReply(parentId, body, authorId));
        assertEquals("创建回复失败", exception.getMessage());
    }

    // ==================== getRepliesByPostId 方法测试 ====================

    @Test
    void testGetRepliesByPostId_Success() {
        // Given
        Long postId = testPostId;
        int page = 0;
        int size = 10;
        List<ForumContent> replies = Arrays.asList(testReply);

        when(contentMapper.findChildren(postId, page * size, size)).thenReturn(replies);

        // When
        List<ForumContent> result = forumPostService.getRepliesByPostId(postId, page, size);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        // 实际实现中，只有当likeCount为null时才会从数据库获取
        // 这里testReply已经有likeCount=0，所以不会调用getMetricValue
        verify(contentMapper, times(1)).findChildren(postId, page * size, size);
        verify(metricMapper, never()).getMetricValue(anyLong(), eq("like_count"));
    }

    @Test
    void testGetRepliesByPostId_NullPostId_ThrowsException() {
        // Given
        Long postId = null;
        int page = 0;
        int size = 10;

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> forumPostService.getRepliesByPostId(postId, page, size));
        assertEquals("帖子ID不能为空", exception.getMessage());
    }

    @Test
    void testGetRepliesByPostId_InvalidPageAndSize() {
        // Given
        Long postId = testPostId;
        int page = -1;
        int size = 0;

        when(contentMapper.findChildren(postId, 0, 20)).thenReturn(new ArrayList<>());

        // When
        List<ForumContent> result = forumPostService.getRepliesByPostId(postId, page, size);

        // Then
        assertNotNull(result);
        verify(contentMapper, times(1)).findChildren(postId, 0, 20);
    }

    // ==================== getReplyCountByPostId 方法测试 ====================

    @Test
    void testGetReplyCountByPostId_Success() {
        // Given
        Long postId = testPostId;
        int expectedCount = 5;
        when(contentMapper.countChildren(postId)).thenReturn(expectedCount);

        // When
        int result = forumPostService.getReplyCountByPostId(postId);

        // Then
        assertEquals(expectedCount, result);
        verify(contentMapper, times(1)).countChildren(postId);
    }

    @Test
    void testGetReplyCountByPostId_NullPostId_ThrowsException() {
        // Given
        Long postId = null;

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> forumPostService.getReplyCountByPostId(postId));
        assertEquals("帖子ID不能为空", exception.getMessage());
    }

    // ==================== deleteReply 方法测试 ====================

    @Test
    void testDeleteReply_Success() {
        // Given
        Long replyId = 2L;
        Long userId = testUserId;
        testReply.setContentId(replyId);

        when(contentMapper.findById(replyId)).thenReturn(testReply);
        when(contentMapper.softDelete(replyId)).thenReturn(1);
        when(metricMapper.incrementMetric(testReply.getParentId(), "reply_count", -1)).thenReturn(1);

        // When
        forumPostService.deleteReply(replyId, userId);

        // Then
        verify(contentMapper, times(1)).findById(replyId);
        verify(contentMapper, times(1)).softDelete(replyId);
        verify(metricMapper, times(1)).incrementMetric(testReply.getParentId(), "reply_count", -1);
    }

    @Test
    void testDeleteReply_NullReplyId_ThrowsException() {
        // Given
        Long replyId = null;
        Long userId = testUserId;

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> forumPostService.deleteReply(replyId, userId));
        assertEquals("回复ID不能为空", exception.getMessage());
    }

    @Test
    void testDeleteReply_ReplyNotFound_ThrowsException() {
        // Given
        Long replyId = 2L;
        Long userId = testUserId;

        when(contentMapper.findById(replyId)).thenReturn(null);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> forumPostService.deleteReply(replyId, userId));
        assertEquals("回复不存在", exception.getMessage());
    }

    @Test
    void testDeleteReply_NotReply_ThrowsException() {
        // Given
        Long replyId = 2L;
        Long userId = testUserId;
        testReply.setContentType("post");

        when(contentMapper.findById(replyId)).thenReturn(testReply);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> forumPostService.deleteReply(replyId, userId));
        assertEquals("回复不存在", exception.getMessage());
    }

    @Test
    void testDeleteReply_NotAuthor_ThrowsException() {
        // Given
        Long replyId = 2L;
        Long userId = 2L; // 不同的用户ID

        when(contentMapper.findById(replyId)).thenReturn(testReply);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> forumPostService.deleteReply(replyId, userId));
        assertEquals("没有权限删除此回复", exception.getMessage());
    }
}
