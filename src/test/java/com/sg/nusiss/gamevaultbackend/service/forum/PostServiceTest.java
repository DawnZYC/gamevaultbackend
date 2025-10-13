package com.sg.nusiss.gamevaultbackend.service.forum;

import com.sg.nusiss.gamevaultbackend.entity.forum.ForumContent;
import com.sg.nusiss.gamevaultbackend.mapper.forum.ForumContentMapper;
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

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ForumPostService 单元测试
 * 目标：100% 代码覆盖率
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ForumPostService 单元测试")
class PostServiceTest {

    @Mock
    private ForumContentMapper contentMapper;

    @Mock
    private ForumMetricMapper metricMapper;

    @Mock
    private ForumContentLikeService contentLikeService;

    @InjectMocks
    private ForumPostService service;

    private Long testAuthorId;
    private Long testPostId;
    private Long testUserId;
    private ForumContent testPost;

    @BeforeEach
    void setUp() {
        testAuthorId = 1L;
        testPostId = 100L;
        testUserId = 2L;

        testPost = new ForumContent("post", "Test Title", "Test Body", testAuthorId);
        testPost.setContentId(testPostId);
        testPost.setLikeCount(5);
    }

    // ================================================================
    // createPost() 方法测试
    // ================================================================

    @Nested
    @DisplayName("createPost() 测试")
    class CreatePostTests {

        @Test
        @DisplayName("创建成功 - 返回帖子对象")
        void createPost_Success_ReturnsPost() {
            // Given
            when(contentMapper.insert(any(ForumContent.class))).thenReturn(1);

            // When
            ForumContent result = service.createPost("Test Title", "Test Body", testAuthorId);

            // Then
            assertNotNull(result);
            assertEquals("Test Title", result.getTitle());
            assertEquals("Test Body", result.getBody());
            assertEquals(testAuthorId, result.getAuthorId());

            // 验证调用了初始化统计
            verify(metricMapper).setMetricValue(any(), eq("view_count"), eq(0));
            verify(metricMapper).setMetricValue(any(), eq("like_count"), eq(0));
            verify(metricMapper).setMetricValue(any(), eq("reply_count"), eq(0));
        }

        @Test
        @DisplayName("标题为 null - 抛出异常")
        void createPost_NullTitle_ThrowsException() {
            // When & Then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> service.createPost(null, "Body", testAuthorId)
            );
            assertEquals("帖子标题不能为空", exception.getMessage());
            verify(contentMapper, never()).insert(any());
        }

        @Test
        @DisplayName("标题为空字符串 - 抛出异常")
        void createPost_EmptyTitle_ThrowsException() {
            // When & Then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> service.createPost("   ", "Body", testAuthorId)
            );
            assertEquals("帖子标题不能为空", exception.getMessage());
        }

        @Test
        @DisplayName("内容为 null - 抛出异常")
        void createPost_NullBody_ThrowsException() {
            // When & Then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> service.createPost("Title", null, testAuthorId)
            );
            assertEquals("帖子内容不能为空", exception.getMessage());
        }

        @Test
        @DisplayName("内容为空字符串 - 抛出异常")
        void createPost_EmptyBody_ThrowsException() {
            // When & Then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> service.createPost("Title", "   ", testAuthorId)
            );
            assertEquals("帖子内容不能为空", exception.getMessage());
        }

        @Test
        @DisplayName("作者ID为 null - 抛出异常")
        void createPost_NullAuthorId_ThrowsException() {
            // When & Then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> service.createPost("Title", "Body", null)
            );
            assertEquals("作者ID不能为空", exception.getMessage());
        }

        @Test
        @DisplayName("数据库插入失败 - 抛出异常")
        void createPost_InsertFails_ThrowsException() {
            // Given
            when(contentMapper.insert(any(ForumContent.class))).thenReturn(0);

            // When & Then
            RuntimeException exception = assertThrows(
                    RuntimeException.class,
                    () -> service.createPost("Title", "Body", testAuthorId)
            );
            assertEquals("创建帖子失败", exception.getMessage());
        }

        @Test
        @DisplayName("初始化统计失败 - 不影响帖子创建")
        void createPost_MetricsInitFails_StillSucceeds() {
            // Given
            when(contentMapper.insert(any(ForumContent.class))).thenReturn(1);
            doThrow(new RuntimeException("Metrics error"))
                    .when(metricMapper).setMetricValue(any(), anyString(), anyInt());

            // When - 不应该抛异常
            ForumContent result = service.createPost("Title", "Body", testAuthorId);

            // Then
            assertNotNull(result);
        }

        @Test
        @DisplayName("数据库异常 - 向上传递异常")
        void createPost_DatabaseException_ThrowsException() {
            // Given
            when(contentMapper.insert(any())).thenThrow(new RuntimeException("DB Error"));

            // When & Then
            assertThrows(RuntimeException.class,
                    () -> service.createPost("Title", "Body", testAuthorId));
        }

        @Test
        @DisplayName("标题和内容有前后空格 - 自动trim")
        void createPost_TrimWhitespace_Success() {
            // Given
            when(contentMapper.insert(any(ForumContent.class))).thenReturn(1);

            // When
            service.createPost("  Title  ", "  Body  ", testAuthorId);

            // Then
            ArgumentCaptor<ForumContent> captor = ArgumentCaptor.forClass(ForumContent.class);
            verify(contentMapper).insert(captor.capture());
            assertEquals("Title", captor.getValue().getTitle());
            assertEquals("Body", captor.getValue().getBody());
        }
    }

    // ================================================================
    // getPostById() 方法测试
    // ================================================================

    @Nested
    @DisplayName("getPostById() 测试")
    class GetPostByIdTests {

        @Test
        @DisplayName("获取成功 - 返回帖子（无当前用户）")
        void getPostById_Success_WithoutCurrentUser() {
            // Given
            when(contentMapper.findById(testPostId)).thenReturn(testPost);

            // When
            ForumContent result = service.getPostById(testPostId, null);

            // Then
            assertNotNull(result);
            assertEquals(testPostId, result.getContentId());
            verify(contentLikeService, never()).isLiked(any(), any());
        }

        @Test
        @DisplayName("获取成功 - 返回帖子（有当前用户，已点赞）")
        void getPostById_Success_WithCurrentUserLiked() {
            // Given
            when(contentMapper.findById(testPostId)).thenReturn(testPost);
            when(contentLikeService.isLiked(testPostId, testUserId)).thenReturn(true);

            // When
            ForumContent result = service.getPostById(testPostId, testUserId);

            // Then
            assertNotNull(result);
            assertTrue(result.getIsLikedByCurrentUser());
            verify(contentLikeService).isLiked(testPostId, testUserId);
        }

        @Test
        @DisplayName("获取成功 - 返回帖子（有当前用户，未点赞）")
        void getPostById_Success_WithCurrentUserNotLiked() {
            // Given
            when(contentMapper.findById(testPostId)).thenReturn(testPost);
            when(contentLikeService.isLiked(testPostId, testUserId)).thenReturn(false);

            // When
            ForumContent result = service.getPostById(testPostId, testUserId);

            // Then
            assertNotNull(result);
            assertFalse(result.getIsLikedByCurrentUser());
        }

        @Test
        @DisplayName("帖子不存在 - 抛出异常")
        void getPostById_NotFound_ThrowsException() {
            // Given
            when(contentMapper.findById(testPostId)).thenReturn(null);

            // When & Then
            RuntimeException exception = assertThrows(
                    RuntimeException.class,
                    () -> service.getPostById(testPostId, testUserId)
            );
            assertEquals("帖子不存在", exception.getMessage());
        }
    }

    // ================================================================
    // getPostList() 方法测试
    // ================================================================

    @Nested
    @DisplayName("getPostList() 测试")
    class GetPostListTests {

        @Test
        @DisplayName("获取列表成功 - 无当前用户")
        void getPostList_Success_WithoutCurrentUser() {
            // Given
            List<ForumContent> posts = Arrays.asList(testPost);
            when(contentMapper.findActivePosts(0, 20)).thenReturn(posts);

            // When
            List<ForumContent> result = service.getPostList(0, 20, null);

            // Then
            assertEquals(1, result.size());
            verify(contentLikeService, never()).batchCheckLikeStatus(any(), any());
        }

        @Test
        @DisplayName("获取列表成功 - 有当前用户")
        void getPostList_Success_WithCurrentUser() {
            // Given
            ForumContent post1 = new ForumContent("post", "Title1", "Body1", testAuthorId);
            post1.setContentId(100L);
            ForumContent post2 = new ForumContent("post", "Title2", "Body2", testAuthorId);
            post2.setContentId(101L);

            List<ForumContent> posts = Arrays.asList(post1, post2);
            when(contentMapper.findActivePosts(0, 20)).thenReturn(posts);

            Map<Long, Boolean> likeStatus = new HashMap<>();
            likeStatus.put(100L, true);
            likeStatus.put(101L, false);
            when(contentLikeService.batchCheckLikeStatus(testUserId, Arrays.asList(100L, 101L)))
                    .thenReturn(likeStatus);

            // When
            List<ForumContent> result = service.getPostList(0, 20, testUserId);

            // Then
            assertEquals(2, result.size());
            assertTrue(result.get(0).getIsLikedByCurrentUser());
            assertFalse(result.get(1).getIsLikedByCurrentUser());
        }

        @Test
        @DisplayName("获取空列表 - 返回空")
        void getPostList_EmptyList_ReturnsEmpty() {
            // Given
            when(contentMapper.findActivePosts(0, 20)).thenReturn(Collections.emptyList());

            // When
            List<ForumContent> result = service.getPostList(0, 20, testUserId);

            // Then
            assertTrue(result.isEmpty());
            verify(contentLikeService, never()).batchCheckLikeStatus(any(), any());
        }

        @Test
        @DisplayName("分页参数 - offset 计算正确")
        void getPostList_Pagination_CorrectOffset() {
            // Given
            when(contentMapper.findActivePosts(40, 20)).thenReturn(Collections.emptyList());

            // When
            service.getPostList(2, 20, null);

            // Then
            verify(contentMapper).findActivePosts(40, 20);
        }
    }

    // ================================================================
    // getPostCount() 方法测试
    // ================================================================

    @Nested
    @DisplayName("getPostCount() 测试")
    class GetPostCountTests {

        @Test
        @DisplayName("统计成功 - 返回数量")
        void getPostCount_Success_ReturnsCount() {
            // Given
            when(contentMapper.countActivePosts()).thenReturn(42);

            // When
            int result = service.getPostCount();

            // Then
            assertEquals(42, result);
        }

        @Test
        @DisplayName("无帖子 - 返回0")
        void getPostCount_NoPosts_ReturnsZero() {
            // Given
            when(contentMapper.countActivePosts()).thenReturn(0);

            // When
            int result = service.getPostCount();

            // Then
            assertEquals(0, result);
        }
    }

    // ================================================================
    // searchPosts() 方法测试
    // ================================================================

    @Nested
    @DisplayName("searchPosts() 测试")
    class SearchPostsTests {

        @Test
        @DisplayName("搜索成功 - 返回结果")
        void searchPosts_Success_ReturnsResults() {
            // Given
            List<ForumContent> posts = Arrays.asList(testPost);
            when(contentMapper.searchPosts("keyword", 0, 20)).thenReturn(posts);

            // When
            List<ForumContent> result = service.searchPosts("keyword", 0, 20, null);

            // Then
            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("搜索关键词为 null - 返回所有帖子")
        void searchPosts_NullKeyword_ReturnsAllPosts() {
            // Given
            List<ForumContent> posts = Arrays.asList(testPost);
            when(contentMapper.findActivePosts(0, 20)).thenReturn(posts);

            // When
            List<ForumContent> result = service.searchPosts(null, 0, 20, null);

            // Then
            assertEquals(1, result.size());
            verify(contentMapper).findActivePosts(0, 20);
            verify(contentMapper, never()).searchPosts(any(), anyInt(), anyInt());
        }

        @Test
        @DisplayName("搜索关键词为空字符串 - 返回所有帖子")
        void searchPosts_EmptyKeyword_ReturnsAllPosts() {
            // Given
            List<ForumContent> posts = Arrays.asList(testPost);
            when(contentMapper.findActivePosts(0, 20)).thenReturn(posts);

            // When
            List<ForumContent> result = service.searchPosts("   ", 0, 20, null);

            // Then
            assertEquals(1, result.size());
            verify(contentMapper).findActivePosts(0, 20);
        }

        @Test
        @DisplayName("page 为负数 - 自动修正为0")
        void searchPosts_NegativePage_CorrectsToZero() {
            // Given
            when(contentMapper.searchPosts("keyword", 0, 20)).thenReturn(Collections.emptyList());

            // When
            service.searchPosts("keyword", -1, 20, null);

            // Then
            verify(contentMapper).searchPosts("keyword", 0, 20);
        }

        @Test
        @DisplayName("size 为0 - 自动修正为20")
        void searchPosts_ZeroSize_CorrectsTo20() {
            // Given
            when(contentMapper.searchPosts("keyword", 0, 20)).thenReturn(Collections.emptyList());

            // When
            service.searchPosts("keyword", 0, 0, null);

            // Then
            verify(contentMapper).searchPosts("keyword", 0, 20);
        }

        @Test
        @DisplayName("size 超过100 - 自动修正为20")
        void searchPosts_SizeOver100_CorrectsTo20() {
            // Given
            when(contentMapper.searchPosts("keyword", 0, 20)).thenReturn(Collections.emptyList());

            // When
            service.searchPosts("keyword", 0, 150, null);

            // Then
            verify(contentMapper).searchPosts("keyword", 0, 20);
        }

        @Test
        @DisplayName("搜索结果有点赞状态")
        void searchPosts_WithCurrentUser_SetsLikeStatus() {
            // Given
            testPost.setContentId(100L);
            List<ForumContent> posts = Arrays.asList(testPost);
            when(contentMapper.searchPosts("keyword", 0, 20)).thenReturn(posts);

            Map<Long, Boolean> likeStatus = new HashMap<>();
            likeStatus.put(100L, true);
            when(contentLikeService.batchCheckLikeStatus(testUserId, Arrays.asList(100L)))
                    .thenReturn(likeStatus);

            // When
            List<ForumContent> result = service.searchPosts("keyword", 0, 20, testUserId);

            // Then
            assertTrue(result.get(0).getIsLikedByCurrentUser());
        }

        @Test
        @DisplayName("关键词前后有空格 - 自动trim")
        void searchPosts_TrimKeyword_Success() {
            // Given
            when(contentMapper.searchPosts("keyword", 0, 20)).thenReturn(Collections.emptyList());

            // When
            service.searchPosts("  keyword  ", 0, 20, null);

            // Then
            verify(contentMapper).searchPosts("keyword", 0, 20);
        }
    }

    // ================================================================
    // getSearchCount() 方法测试
    // ================================================================

    @Nested
    @DisplayName("getSearchCount() 测试")
    class GetSearchCountTests {

        @Test
        @DisplayName("统计搜索结果 - 返回数量")
        void getSearchCount_Success_ReturnsCount() {
            // Given
            when(contentMapper.countSearchPosts("keyword")).thenReturn(10);

            // When
            int result = service.getSearchCount("keyword");

            // Then
            assertEquals(10, result);
        }

        @Test
        @DisplayName("关键词为 null - 返回总数")
        void getSearchCount_NullKeyword_ReturnsTotal() {
            // Given
            when(contentMapper.countActivePosts()).thenReturn(100);

            // When
            int result = service.getSearchCount(null);

            // Then
            assertEquals(100, result);
            verify(contentMapper, never()).countSearchPosts(any());
        }

        @Test
        @DisplayName("关键词为空字符串 - 返回总数")
        void getSearchCount_EmptyKeyword_ReturnsTotal() {
            // Given
            when(contentMapper.countActivePosts()).thenReturn(100);

            // When
            int result = service.getSearchCount("   ");

            // Then
            assertEquals(100, result);
        }
    }

    // ================================================================
    // incrementViewCount() 方法测试
    // ================================================================

    @Nested
    @DisplayName("incrementViewCount() 测试")
    class IncrementViewCountTests {

        @Test
        @DisplayName("增加浏览量成功")
        void incrementViewCount_Success() {
            // Given
            when(contentMapper.findById(testPostId)).thenReturn(testPost);

            // When
            service.incrementViewCount(testPostId);

            // Then
            verify(metricMapper).incrementMetric(testPostId, "view_count", 1);
        }

        @Test
        @DisplayName("postId 为 null - 抛出异常")
        void incrementViewCount_NullPostId_ThrowsException() {
            // When & Then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> service.incrementViewCount(null)
            );
            assertEquals("帖子ID不能为空", exception.getMessage());
        }

        @Test
        @DisplayName("帖子不存在 - 抛出异常")
        void incrementViewCount_PostNotFound_ThrowsException() {
            // Given
            when(contentMapper.findById(testPostId)).thenReturn(null);

            // When & Then
            RuntimeException exception = assertThrows(
                    RuntimeException.class,
                    () -> service.incrementViewCount(testPostId)
            );
            assertEquals("帖子不存在", exception.getMessage());
        }

        @Test
        @DisplayName("内容不是帖子 - 抛出异常")
        void incrementViewCount_NotPost_ThrowsException() {
            // Given
            ForumContent reply = new ForumContent("reply", "Body", testAuthorId, 1L);
            when(contentMapper.findById(testPostId)).thenReturn(reply);

            // When & Then
            RuntimeException exception = assertThrows(
                    RuntimeException.class,
                    () -> service.incrementViewCount(testPostId)
            );
            assertEquals("帖子不存在", exception.getMessage());
        }
    }

    // ================================================================
    // deletePost() 方法测试
    // ================================================================

    @Nested
    @DisplayName("deletePost() 测试")
    class DeletePostTests {

        @Test
        @DisplayName("删除成功")
        void deletePost_Success() {
            // Given
            when(contentMapper.findById(testPostId)).thenReturn(testPost);

            // When
            service.deletePost(testPostId, testAuthorId);

            // Then
            verify(contentMapper).softDelete(testPostId);
        }

        @Test
        @DisplayName("postId 为 null - 抛出异常")
        void deletePost_NullPostId_ThrowsException() {
            // When & Then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> service.deletePost(null, testAuthorId)
            );
            assertEquals("帖子ID不能为空", exception.getMessage());
        }

        @Test
        @DisplayName("帖子不存在 - 抛出异常")
        void deletePost_NotFound_ThrowsException() {
            // Given
            when(contentMapper.findById(testPostId)).thenReturn(null);

            // When & Then
            RuntimeException exception = assertThrows(
                    RuntimeException.class,
                    () -> service.deletePost(testPostId, testAuthorId)
            );
            assertEquals("帖子不存在", exception.getMessage());
        }

        @Test
        @DisplayName("不是帖子 - 抛出异常")
        void deletePost_NotPost_ThrowsException() {
            // Given
            ForumContent reply = new ForumContent("reply", "Body", testAuthorId, 1L);
            when(contentMapper.findById(testPostId)).thenReturn(reply);

            // When & Then
            RuntimeException exception = assertThrows(
                    RuntimeException.class,
                    () -> service.deletePost(testPostId, testAuthorId)
            );
            assertEquals("帖子不存在", exception.getMessage());
        }

        @Test
        @DisplayName("非作者删除 - 抛出异常")
        void deletePost_NotAuthor_ThrowsException() {
            // Given
            when(contentMapper.findById(testPostId)).thenReturn(testPost);

            // When & Then
            RuntimeException exception = assertThrows(
                    RuntimeException.class,
                    () -> service.deletePost(testPostId, 999L)
            );
            assertEquals("没有权限删除此帖子", exception.getMessage());
            verify(contentMapper, never()).softDelete(any());
        }
    }

    // ================================================================
    // updatePost() 方法测试
    // ================================================================

    @Nested
    @DisplayName("updatePost() 测试")
    class UpdatePostTests {

        @Test
        @DisplayName("更新成功 - 标题和内容")
        void updatePost_Success_TitleAndBody() {
            // Given
            when(contentMapper.findById(testPostId)).thenReturn(testPost);
            when(contentMapper.update(any(ForumContent.class))).thenReturn(1);

            // When
            ForumContent result = service.updatePost(testPostId, "New Title", "New Body", testAuthorId);

            // Then
            assertNotNull(result);
            assertEquals("New Title", result.getTitle());
            assertEquals("New Body", result.getBody());
            verify(contentMapper).update(any(ForumContent.class));
        }

        @Test
        @DisplayName("只更新标题")
        void updatePost_OnlyTitle_Success() {
            // Given
            String originalBody = testPost.getBody();
            when(contentMapper.findById(testPostId)).thenReturn(testPost);
            when(contentMapper.update(any(ForumContent.class))).thenReturn(1);

            // When
            ForumContent result = service.updatePost(testPostId, "New Title", null, testAuthorId);

            // Then
            assertEquals("New Title", result.getTitle());
            assertEquals(originalBody, result.getBody());
        }

        @Test
        @DisplayName("只更新内容")
        void updatePost_OnlyBody_Success() {
            // Given
            String originalTitle = testPost.getTitle();
            when(contentMapper.findById(testPostId)).thenReturn(testPost);
            when(contentMapper.update(any(ForumContent.class))).thenReturn(1);

            // When
            ForumContent result = service.updatePost(testPostId, null, "New Body", testAuthorId);

            // Then
            assertEquals(originalTitle, result.getTitle());
            assertEquals("New Body", result.getBody());
        }

        @Test
        @DisplayName("postId 为 null - 抛出异常")
        void updatePost_NullPostId_ThrowsException() {
            // When & Then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> service.updatePost(null, "Title", "Body", testAuthorId)
            );
            assertEquals("帖子ID不能为空", exception.getMessage());
        }

        @Test
        @DisplayName("帖子不存在 - 抛出异常")
        void updatePost_NotFound_ThrowsException() {
            // Given
            when(contentMapper.findById(testPostId)).thenReturn(null);

            // When & Then
            RuntimeException exception = assertThrows(
                    RuntimeException.class,
                    () -> service.updatePost(testPostId, "Title", "Body", testAuthorId)
            );
            assertEquals("帖子不存在", exception.getMessage());
        }

        @Test
        @DisplayName("非作者更新 - 抛出异常")
        void updatePost_NotAuthor_ThrowsException() {
            // Given
            when(contentMapper.findById(testPostId)).thenReturn(testPost);

            // When & Then
            RuntimeException exception = assertThrows(
                    RuntimeException.class,
                    () -> service.updatePost(testPostId, "Title", "Body", 999L)
            );
            assertEquals("没有权限编辑此帖子", exception.getMessage());
        }

        @Test
        @DisplayName("数据库更新失败 - 抛出异常")
        void updatePost_UpdateFails_ThrowsException() {
            // Given
            when(contentMapper.findById(testPostId)).thenReturn(testPost);
            when(contentMapper.update(any())).thenReturn(0);

            // When & Then
            RuntimeException exception = assertThrows(
                    RuntimeException.class,
                    () -> service.updatePost(testPostId, "Title", "Body", testAuthorId)
            );
            assertEquals("更新帖子失败", exception.getMessage());
        }

        @Test
        @DisplayName("标题和内容有空格 - 自动trim")
        void updatePost_TrimWhitespace_Success() {
            // Given
            when(contentMapper.findById(testPostId)).thenReturn(testPost);
            when(contentMapper.update(any())).thenReturn(1);

            // When
            ForumContent result = service.updatePost(testPostId, "  Title  ", "  Body  ", testAuthorId);

            // Then
            assertEquals("Title", result.getTitle());
            assertEquals("Body", result.getBody());
        }

        @Test
        @DisplayName("空字符串不更新")
        void updatePost_EmptyString_NotUpdate() {
            // Given
            String originalTitle = testPost.getTitle();
            String originalBody = testPost.getBody();
            when(contentMapper.findById(testPostId)).thenReturn(testPost);
            when(contentMapper.update(any())).thenReturn(1);

            // When
            ForumContent result = service.updatePost(testPostId, "   ", "   ", testAuthorId);

            // Then
            assertEquals(originalTitle, result.getTitle());
            assertEquals(originalBody, result.getBody());
        }
    }

    // ================================================================
    // getPostsByAuthorId() 方法测试
    // ================================================================

    @Nested
    @DisplayName("getPostsByAuthorId() 测试")
    class GetPostsByAuthorIdTests {

        @Test
        @DisplayName("获取成功 - 返回作者帖子")
        void getPostsByAuthorId_Success_ReturnsPosts() {
            // Given
            List<ForumContent> posts = Arrays.asList(testPost);
            when(contentMapper.selectActiveByAuthorId(testAuthorId, 0, 20)).thenReturn(posts);

            // When
            List<ForumContent> result = service.getPostsByAuthorId(testAuthorId, 0, 20, null);

            // Then
            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("authorId 为 null - 抛出异常")
        void getPostsByAuthorId_NullAuthorId_ThrowsException() {
            // When & Then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> service.getPostsByAuthorId(null, 0, 20, null)
            );
            assertEquals("作者ID不能为空", exception.getMessage());
        }

        @Test
        @DisplayName("page 为负数 - 修正为0")
        void getPostsByAuthorId_NegativePage_CorrectsToZero() {
            // Given
            when(contentMapper.selectActiveByAuthorId(testAuthorId, 0, 20))
                    .thenReturn(Collections.emptyList());

            // When
            service.getPostsByAuthorId(testAuthorId, -1, 20, null);

            // Then
            verify(contentMapper).selectActiveByAuthorId(testAuthorId, 0, 20);
        }

        @Test
        @DisplayName("size 超过100 - 修正为20")
        void getPostsByAuthorId_SizeOver100_CorrectsTo20() {
            // Given
            when(contentMapper.selectActiveByAuthorId(testAuthorId, 0, 20))
                    .thenReturn(Collections.emptyList());

            // When
            service.getPostsByAuthorId(testAuthorId, 0, 150, null);

            // Then
            verify(contentMapper).selectActiveByAuthorId(testAuthorId, 0, 20);
        }

        @Test
        @DisplayName("有当前用户 - 设置点赞状态")
        void getPostsByAuthorId_WithCurrentUser_SetsLikeStatus() {
            // Given
            testPost.setContentId(100L);
            List<ForumContent> posts = Arrays.asList(testPost);
            when(contentMapper.selectActiveByAuthorId(testAuthorId, 0, 20)).thenReturn(posts);

            Map<Long, Boolean> likeStatus = new HashMap<>();
            likeStatus.put(100L, true);
            when(contentLikeService.batchCheckLikeStatus(testUserId, Arrays.asList(100L)))
                    .thenReturn(likeStatus);

            // When
            List<ForumContent> result = service.getPostsByAuthorId(testAuthorId, 0, 20, testUserId);

            // Then
            assertTrue(result.get(0).getIsLikedByCurrentUser());
        }
    }

    // ================================================================
    // getPostCountByAuthorId() 方法测试
    // ================================================================

    @Nested
    @DisplayName("getPostCountByAuthorId() 测试")
    class GetPostCountByAuthorIdTests {

        @Test
        @DisplayName("统计成功 - 返回数量")
        void getPostCountByAuthorId_Success_ReturnsCount() {
            // Given
            when(contentMapper.countActiveByAuthorId(testAuthorId)).thenReturn(10);

            // When
            int result = service.getPostCountByAuthorId(testAuthorId);

            // Then
            assertEquals(10, result);
        }

        @Test
        @DisplayName("authorId 为 null - 抛出异常")
        void getPostCountByAuthorId_NullAuthorId_ThrowsException() {
            // When & Then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> service.getPostCountByAuthorId(null)
            );
            assertEquals("作者ID不能为空", exception.getMessage());
        }
    }

    // ================================================================
    // createReply() 方法测试
    // ================================================================

    @Nested
    @DisplayName("createReply() 测试")
    class CreateReplyTests {

        @Test
        @DisplayName("创建回复成功")
        void createReply_Success_ReturnsReply() {
            // Given
            when(contentMapper.findById(testPostId)).thenReturn(testPost);
            when(contentMapper.insert(any(ForumContent.class))).thenReturn(1);

            // When
            ForumContent result = service.createReply(testPostId, "Reply Body", testUserId);

            // Then
            assertNotNull(result);
            assertEquals("Reply Body", result.getBody());
            assertEquals(testUserId, result.getAuthorId());
            assertEquals(testPostId, result.getParentId());

            // 验证初始化回复统计
            verify(metricMapper).setMetricValue(any(), eq("like_count"), eq(0));
            // 验证父内容回复数+1
            verify(metricMapper).incrementMetric(testPostId, "reply_count", 1);
        }

        @Test
        @DisplayName("parentId 为 null - 抛出异常")
        void createReply_NullParentId_ThrowsException() {
            // When & Then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> service.createReply(null, "Body", testUserId)
            );
            assertEquals("父内容ID不能为空", exception.getMessage());
        }

        @Test
        @DisplayName("body 为 null - 抛出异常")
        void createReply_NullBody_ThrowsException() {
            // When & Then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> service.createReply(testPostId, null, testUserId)
            );
            assertEquals("回复内容不能为空", exception.getMessage());
        }

        @Test
        @DisplayName("body 为空字符串 - 抛出异常")
        void createReply_EmptyBody_ThrowsException() {
            // When & Then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> service.createReply(testPostId, "   ", testUserId)
            );
            assertEquals("回复内容不能为空", exception.getMessage());
        }

        @Test
        @DisplayName("authorId 为 null - 抛出异常")
        void createReply_NullAuthorId_ThrowsException() {
            // When & Then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> service.createReply(testPostId, "Body", null)
            );
            assertEquals("作者ID不能为空", exception.getMessage());
        }

        @Test
        @DisplayName("父内容不存在 - 抛出异常")
        void createReply_ParentNotFound_ThrowsException() {
            // Given
            when(contentMapper.findById(testPostId)).thenReturn(null);

            // When & Then
            RuntimeException exception = assertThrows(
                    RuntimeException.class,
                    () -> service.createReply(testPostId, "Body", testUserId)
            );
            assertEquals("父内容不存在", exception.getMessage());
        }

        @Test
        @DisplayName("数据库插入失败 - 抛出异常")
        void createReply_InsertFails_ThrowsException() {
            // Given
            when(contentMapper.findById(testPostId)).thenReturn(testPost);
            when(contentMapper.insert(any())).thenReturn(0);

            // When & Then
            RuntimeException exception = assertThrows(
                    RuntimeException.class,
                    () -> service.createReply(testPostId, "Body", testUserId)
            );
            assertEquals("创建回复失败", exception.getMessage());
        }

        @Test
        @DisplayName("内容有前后空格 - 自动trim")
        void createReply_TrimWhitespace_Success() {
            // Given
            when(contentMapper.findById(testPostId)).thenReturn(testPost);
            when(contentMapper.insert(any())).thenReturn(1);

            // When
            service.createReply(testPostId, "  Body  ", testUserId);

            // Then
            ArgumentCaptor<ForumContent> captor = ArgumentCaptor.forClass(ForumContent.class);
            verify(contentMapper).insert(captor.capture());
            assertEquals("Body", captor.getValue().getBody());
        }

        @Test
        @DisplayName("初始化统计失败 - 记录日志但不抛异常")
        void createReply_MetricsInitFails_LogsError() {
            // Given
            when(contentMapper.findById(testPostId)).thenReturn(testPost);
            when(contentMapper.insert(any())).thenReturn(1);
            doThrow(new RuntimeException("Metrics error"))
                    .when(metricMapper).setMetricValue(any(), eq("like_count"), anyInt());

            // When - 不应该抛异常
            ForumContent result = service.createReply(testPostId, "Body", testUserId);

            // Then
            assertNotNull(result);
        }
    }

    // ================================================================
    // getRepliesByPostId() 方法测试
    // ================================================================

    @Nested
    @DisplayName("getRepliesByPostId() 测试")
    class GetRepliesByPostIdTests {

        @Test
        @DisplayName("获取回复成功")
        void getRepliesByPostId_Success_ReturnsReplies() {
            // Given
            ForumContent reply = new ForumContent("reply", "Reply", testUserId, testPostId);
            reply.setLikeCount(3);
            List<ForumContent> replies = Arrays.asList(reply);
            when(contentMapper.findChildren(testPostId, 0, 20)).thenReturn(replies);

            // When
            List<ForumContent> result = service.getRepliesByPostId(testPostId, 0, 20);

            // Then
            assertEquals(1, result.size());
            assertEquals(3, result.get(0).getLikeCount());
        }

        @Test
        @DisplayName("回复没有点赞数 - 从 metric 加载")
        void getRepliesByPostId_NoLikeCount_LoadsFromMetric() {
            // Given
            ForumContent reply = new ForumContent("reply", "Reply", testUserId, testPostId);
            reply.setContentId(200L);
            reply.setLikeCount(null);  // 没有点赞数

            List<ForumContent> replies = Arrays.asList(reply);
            when(contentMapper.findChildren(testPostId, 0, 20)).thenReturn(replies);
            when(metricMapper.getMetricValue(200L, "like_count")).thenReturn(5);

            // When
            List<ForumContent> result = service.getRepliesByPostId(testPostId, 0, 20);

            // Then
            assertEquals(5, result.get(0).getLikeCount());
            verify(metricMapper).getMetricValue(200L, "like_count");
        }

        @Test
        @DisplayName("metric 返回 null - 设置为0")
        void getRepliesByPostId_MetricNull_SetsZero() {
            // Given
            ForumContent reply = new ForumContent("reply", "Reply", testUserId, testPostId);
            reply.setContentId(200L);
            reply.setLikeCount(null);

            List<ForumContent> replies = Arrays.asList(reply);
            when(contentMapper.findChildren(testPostId, 0, 20)).thenReturn(replies);
            when(metricMapper.getMetricValue(200L, "like_count")).thenReturn(null);

            // When
            List<ForumContent> result = service.getRepliesByPostId(testPostId, 0, 20);

            // Then
            assertEquals(0, result.get(0).getLikeCount());
        }

        @Test
        @DisplayName("postId 为 null - 抛出异常")
        void getRepliesByPostId_NullPostId_ThrowsException() {
            // When & Then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> service.getRepliesByPostId(null, 0, 20)
            );
            assertEquals("帖子ID不能为空", exception.getMessage());
        }

        @Test
        @DisplayName("page 为负数 - 修正为0")
        void getRepliesByPostId_NegativePage_CorrectsToZero() {
            // Given
            when(contentMapper.findChildren(testPostId, 0, 20)).thenReturn(Collections.emptyList());

            // When
            service.getRepliesByPostId(testPostId, -1, 20);

            // Then
            verify(contentMapper).findChildren(testPostId, 0, 20);
        }

        @Test
        @DisplayName("size 超过100 - 修正为20")
        void getRepliesByPostId_SizeOver100_CorrectsTo20() {
            // Given
            when(contentMapper.findChildren(testPostId, 0, 20)).thenReturn(Collections.emptyList());

            // When
            service.getRepliesByPostId(testPostId, 0, 150);

            // Then
            verify(contentMapper).findChildren(testPostId, 0, 20);
        }
    }

    // ================================================================
    // getReplyCountByPostId() 方法测试
    // ================================================================

    @Nested
    @DisplayName("getReplyCountByPostId() 测试")
    class GetReplyCountByPostIdTests {

        @Test
        @DisplayName("统计成功 - 返回数量")
        void getReplyCountByPostId_Success_ReturnsCount() {
            // Given
            when(contentMapper.countChildren(testPostId)).thenReturn(15);

            // When
            int result = service.getReplyCountByPostId(testPostId);

            // Then
            assertEquals(15, result);
        }

        @Test
        @DisplayName("postId 为 null - 抛出异常")
        void getReplyCountByPostId_NullPostId_ThrowsException() {
            // When & Then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> service.getReplyCountByPostId(null)
            );
            assertEquals("帖子ID不能为空", exception.getMessage());
        }
    }

    // ================================================================
    // deleteReply() 方法测试
    // ================================================================

    @Nested
    @DisplayName("deleteReply() 测试")
    class DeleteReplyTests {

        @Test
        @DisplayName("删除回复成功")
        void deleteReply_Success() {
            // Given
            ForumContent reply = new ForumContent("reply", "Reply", testUserId, testPostId);
            reply.setContentId(200L);
            when(contentMapper.findById(200L)).thenReturn(reply);

            // When
            service.deleteReply(200L, testUserId);

            // Then
            verify(contentMapper).softDelete(200L);
            verify(metricMapper).incrementMetric(testPostId, "reply_count", -1);
        }

        @Test
        @DisplayName("replyId 为 null - 抛出异常")
        void deleteReply_NullReplyId_ThrowsException() {
            // When & Then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> service.deleteReply(null, testUserId)
            );
            assertEquals("回复ID不能为空", exception.getMessage());
        }

        @Test
        @DisplayName("回复不存在 - 抛出异常")
        void deleteReply_NotFound_ThrowsException() {
            // Given
            when(contentMapper.findById(200L)).thenReturn(null);

            // When & Then
            RuntimeException exception = assertThrows(
                    RuntimeException.class,
                    () -> service.deleteReply(200L, testUserId)
            );
            assertEquals("回复不存在", exception.getMessage());
        }

        @Test
        @DisplayName("不是回复 - 抛出异常")
        void deleteReply_NotReply_ThrowsException() {
            // Given
            when(contentMapper.findById(200L)).thenReturn(testPost);

            // When & Then
            RuntimeException exception = assertThrows(
                    RuntimeException.class,
                    () -> service.deleteReply(200L, testUserId)
            );
            assertEquals("回复不存在", exception.getMessage());
        }

        @Test
        @DisplayName("非作者删除 - 抛出异常")
        void deleteReply_NotAuthor_ThrowsException() {
            // Given
            ForumContent reply = new ForumContent("reply", "Reply", testUserId, testPostId);
            when(contentMapper.findById(200L)).thenReturn(reply);

            // When & Then
            RuntimeException exception = assertThrows(
                    RuntimeException.class,
                    () -> service.deleteReply(200L, 999L)
            );
            assertEquals("没有权限删除此回复", exception.getMessage());
            verify(contentMapper, never()).softDelete(any());
        }
    }
}