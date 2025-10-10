package com.sg.nusiss.gamevaultbackend.controller.forum;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.sg.nusiss.gamevaultbackend.annotation.forum.RequireForumAuth;
import com.sg.nusiss.gamevaultbackend.dto.forum.PostDTO;
import com.sg.nusiss.gamevaultbackend.dto.forum.PostResponseDTO;
import com.sg.nusiss.gamevaultbackend.entity.forum.ForumContent;
import com.sg.nusiss.gamevaultbackend.entity.forum.ForumUser;
import com.sg.nusiss.gamevaultbackend.entity.forum.UserContentRelation;
import com.sg.nusiss.gamevaultbackend.service.forum.ForumContentLikeService;
import com.sg.nusiss.gamevaultbackend.service.forum.ForumPostService;
import com.sg.nusiss.gamevaultbackend.service.forum.ForumUserService;
import com.sg.nusiss.gamevaultbackend.service.forum.ViewTracker;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 帖子控制器
 * 提供帖子相关的 REST API
 */
@RestController
@RequestMapping("/api/forum/posts")
@CrossOrigin(origins = "*")
public class ForumPostController {

    private static final Logger logger = LoggerFactory.getLogger(ForumPostController.class);

    @Autowired
    private ForumPostService postService;

    @Autowired
    private ForumUserService userService;

    @Autowired
    private ForumContentLikeService contentLikeService;
    
    @Autowired
    private ViewTracker viewTracker;

    /**
     * 获取帖子列表（分页）
     */
    @GetMapping
    public ResponseEntity<?> getPostList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        System.out.println("============ GET请求到达了 ============"); // 加这行

        logger.info("获取帖子列表 - 页码: {}, 每页大小: {}", page, size);

        Long userId = (Long) request.getAttribute("userId");

        try {
            List<ForumContent> posts = postService.getPostList(page, size,userId);
            int totalCount = postService.getPostCount();

            return getResponseEntity(page, size, posts, totalCount);

        } catch (Exception e) {
            logger.error("获取帖子列表失败", e);
            return createErrorResponse("获取帖子列表失败", e.getMessage());
        }
    }

    /**
     * 根据ID获取帖子详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getPostById(@PathVariable Long id,HttpServletRequest request) {
        logger.info("获取帖子详情 - 帖子ID: {}", id);
        Long userId = (Long) request.getAttribute("userId");
        try {
            ForumContent post = postService.getPostById(id,userId);
            
            // 只有登录用户的浏览才计入统计
            if (userId != null) {
                // 使用ViewTracker防止重复计数
                boolean shouldIncrement = viewTracker.shouldIncrementView(userId, null, id);
                
                if (shouldIncrement) {
                    postService.incrementViewCount(id);
                    logger.debug("浏览量+1 - 帖子ID: {}, 用户ID: {}", id, userId);
                } else {
                    logger.debug("浏览量不变 - 用户{}在5分钟内重复访问帖子{}", userId, id);
                }
            } else {
                logger.debug("未登录用户访问 - 不计入浏览量 - 帖子ID: {}", id);
            }

            ForumUser author = getUserSafely(post.getAuthorId());
            PostResponseDTO dto = PostResponseDTO.fromContentAndUser(post, author);

            return ResponseEntity.ok(Map.of("post", dto));

        } catch (IllegalArgumentException e) {
            logger.warn("参数错误: {}", e.getMessage());
            return createErrorResponse("参数错误", e.getMessage(), HttpStatus.BAD_REQUEST);

        } catch (RuntimeException e) {
            logger.warn("帖子不存在: {}", e.getMessage());
            return createErrorResponse("帖子不存在", e.getMessage(), HttpStatus.NOT_FOUND);

        } catch (Exception e) {
            logger.error("获取帖子详情失败", e);
            return createErrorResponse("获取帖子详情失败", e.getMessage());
        }
    }

    /**
     * 创建新帖子
     */
    @PostMapping
    @RequireForumAuth
    public ResponseEntity<?> createPost(
            @Valid @RequestBody PostDTO postDTO,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        logger.info("创建帖子 - 用户ID: {}, 标题: {}", userId, postDTO.getTitle());

        try {
            // 验证用户登录
            if (userId == null) {
                return createErrorResponse("需要登录", "请先登录再发帖", HttpStatus.UNAUTHORIZED);
            }

            // 验证用户状态
            if (!userService.isUserActive(userId)) {
                return createErrorResponse("用户无效", "用户不存在或已被禁用", HttpStatus.FORBIDDEN);
            }

            // 创建帖子
            ForumContent post = postService.createPost(postDTO.getTitle(), postDTO.getBody(), userId);
            ForumUser author = userService.getUserById(userId);
            PostResponseDTO dto = PostResponseDTO.fromContentAndUser(post, author);

            Map<String, Object> response = new HashMap<>();
            response.put("post", dto);
            response.put("message", "帖子创建成功");

            logger.info("帖子创建成功 - 帖子ID: {}", post.getContentId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            logger.warn("参数错误: {}", e.getMessage());
            return createErrorResponse("参数错误", e.getMessage(), HttpStatus.BAD_REQUEST);

        } catch (Exception e) {
            logger.error("创建帖子失败", e);
            return createErrorResponse("创建帖子失败", e.getMessage());
        }
    }

    /**
     * 搜索帖子
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchPosts(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {

        logger.info("搜索帖子 - 关键词: {}", keyword);
        Long userId = (Long) request.getAttribute("userId");
        try {
            List<ForumContent> posts = postService.searchPosts(keyword, page, size,userId);
            int totalCount = postService.getSearchCount(keyword);

            List<PostResponseDTO> postDTOs = convertToResponseDTOs(posts);

            Map<String, Object> response = new HashMap<>();
            response.put("posts", postDTOs);
            response.put("keyword", keyword);
            response.put("currentPage", page);
            response.put("pageSize", size);
            response.put("totalCount", totalCount);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("搜索帖子失败", e);
            return createErrorResponse("搜索帖子失败", e.getMessage());
        }
    }

    /**
     * 删除帖子
     */
    @DeleteMapping("/{id}")
    @RequireForumAuth
    public ResponseEntity<?> deletePost(
            @PathVariable Long id,
            HttpServletRequest request) {

        Long userId = (Long) request.getAttribute("userId");

        logger.info("删除帖子 - 帖子ID: {}, 用户ID: {}", id, userId);

        try {
            if (userId == null) {
                return createErrorResponse("需要登录", "请先登录", HttpStatus.UNAUTHORIZED);
            }

            postService.deletePost(id, userId);
            return ResponseEntity.ok(Map.of("message", "帖子删除成功"));

        } catch (RuntimeException e) {
            if (e.getMessage().contains("权限")) {
                return createErrorResponse("权限不足", e.getMessage(), HttpStatus.FORBIDDEN);
            } else {
                return createErrorResponse("帖子不存在", e.getMessage(), HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            logger.error("删除帖子失败", e);
            return createErrorResponse("删除帖子失败", e.getMessage());
        }
    }

    /**
     * 获取用户的帖子列表
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserPosts(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        Long currentUserId = (Long) request.getAttribute("userId");

        logger.info("获取用户帖子 - 用户ID: {}, 页码: {}", userId, page);

        try {
            // 调用 service 获取该用户的帖子
            List<ForumContent> posts = postService.getPostsByAuthorId(userId, page, size,currentUserId);
            int totalCount = postService.getPostCountByAuthorId(userId);

            return getResponseEntity(page, size, posts, totalCount);

        } catch (Exception e) {
            logger.error("获取用户帖子失败", e);
            return createErrorResponse("获取用户帖子失败", e.getMessage());
        }
    }

    @NotNull
    private ResponseEntity<?> getResponseEntity(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size, List<ForumContent> posts, int totalCount) {
        List<PostResponseDTO> postDTOs = convertToResponseDTOs(posts);

        Map<String, Object> response = new HashMap<>();
        response.put("posts", postDTOs);
        response.put("currentPage", page);
        response.put("pageSize", size);
        response.put("totalCount", totalCount);
        response.put("totalPages", (int) Math.ceil((double) totalCount / size));

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{postId}/like/toggle")
    @RequireForumAuth
    public ResponseEntity<?> toggleLike(
            @PathVariable Long postId,
            HttpServletRequest request) {

        Long userId = (Long) request.getAttribute("userId");
        logger.info("切换点赞状态 - 帖子ID: {}, 用户ID: {}", postId, userId);

        try {
            // 验证用户登录
            if (userId == null) {
                return createErrorResponse("需要登录", "请先登录再点赞", HttpStatus.UNAUTHORIZED);
            }

            // 切换点赞状态
            boolean liked = contentLikeService.toggleLike(postId, userId);
            int likeCount = contentLikeService.getLikeCount(postId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", liked ? "点赞成功" : "取消点赞成功");
            response.put("liked", liked);
            response.put("likeCount", likeCount);

            logger.info("点赞操作成功 - 帖子ID: {}, 用户ID: {}, 状态: {}", postId, userId, liked ? "已点赞" : "已取消");
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.warn("参数错误: {}", e.getMessage());
            return createErrorResponse("参数错误", e.getMessage(), HttpStatus.BAD_REQUEST);

        } catch (Exception e) {
            logger.error("切换点赞失败", e);
            return createErrorResponse("操作失败", e.getMessage());
        }
    }

    /**
     * 点赞帖子
     * POST /api/forum/posts/{postId}/like
     */
    @PostMapping("/{postId}/like")
    @RequireForumAuth
    public ResponseEntity<?> likePost(
            @PathVariable Long postId,
            HttpServletRequest request) {

        Long userId = (Long) request.getAttribute("userId");
        logger.info("点赞帖子 - 帖子ID: {}, 用户ID: {}", postId, userId);

        try {
            if (userId == null) {
                return createErrorResponse("需要登录", "请先登录再点赞", HttpStatus.UNAUTHORIZED);
            }

            boolean success = contentLikeService.likeContent(postId, userId);

            if (success) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "点赞成功");
                response.put("liked", true);
                response.put("likeCount", contentLikeService.getLikeCount(postId));

                logger.info("点赞成功 - 帖子ID: {}, 用户ID: {}", postId, userId);
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "已经点赞过该帖子");
                response.put("liked", true);
                response.put("likeCount", contentLikeService.getLikeCount(postId));

                logger.info("重复点赞 - 帖子ID: {}, 用户ID: {}", postId, userId);
                return ResponseEntity.ok(response);
            }

        } catch (IllegalArgumentException e) {
            logger.warn("参数错误: {}", e.getMessage());
            return createErrorResponse("参数错误", e.getMessage(), HttpStatus.BAD_REQUEST);

        } catch (Exception e) {
            logger.error("点赞失败", e);
            return createErrorResponse("点赞失败", e.getMessage());
        }
    }

    /**
     * 取消点赞
     * DELETE /api/forum/posts/{postId}/like
     */
    @DeleteMapping("/{postId}/like")
    @RequireForumAuth
    public ResponseEntity<?> unlikePost(
            @PathVariable Long postId,
            HttpServletRequest request) {

        Long userId = (Long) request.getAttribute("userId");
        logger.info("取消点赞 - 帖子ID: {}, 用户ID: {}", postId, userId);

        try {
            if (userId == null) {
                return createErrorResponse("需要登录", "请先登录", HttpStatus.UNAUTHORIZED);
            }

            boolean success = contentLikeService.unlikeContent(postId, userId);

            if (success) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "取消点赞成功");
                response.put("liked", false);
                response.put("likeCount", contentLikeService.getLikeCount(postId));

                logger.info("取消点赞成功 - 帖子ID: {}, 用户ID: {}", postId, userId);
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "未点赞过该帖子");
                response.put("liked", false);
                response.put("likeCount", contentLikeService.getLikeCount(postId));

                return ResponseEntity.ok(response);
            }

        } catch (IllegalArgumentException e) {
            logger.warn("参数错误: {}", e.getMessage());
            return createErrorResponse("参数错误", e.getMessage(), HttpStatus.BAD_REQUEST);

        } catch (Exception e) {
            logger.error("取消点赞失败", e);
            return createErrorResponse("取消点赞失败", e.getMessage());
        }
    }

    /**
     * 获取点赞状态
     * GET /api/forum/posts/{postId}/like/status
     */
    @GetMapping("/{postId}/like/status")
    @RequireForumAuth
    public ResponseEntity<?> getLikeStatus(
            @PathVariable Long postId,
            HttpServletRequest request) {

        Long userId = (Long) request.getAttribute("userId");
        logger.info("获取点赞状态 - 帖子ID: {}, 用户ID: {}", postId, userId);

        try {
            if (userId == null) {
                return createErrorResponse("需要登录", "请先登录", HttpStatus.UNAUTHORIZED);
            }

            boolean liked = contentLikeService.isLiked(postId, userId);
            int likeCount = contentLikeService.getLikeCount(postId);

            Map<String, Object> response = new HashMap<>();
            response.put("liked", liked);
            response.put("likeCount", likeCount);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("获取点赞状态失败", e);
            return createErrorResponse("获取点赞状态失败", e.getMessage());
        }
    }

    /**
     * 获取帖子的点赞用户列表
     * GET /api/forum/posts/{postId}/likes
     */
    @GetMapping("/{postId}/likes")
    public ResponseEntity<?> getLikedUsers(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "20") int limit) {

        logger.info("获取点赞用户列表 - 帖子ID: {}", postId);

        try {
            List<Long> userIds = contentLikeService.getLikedUserIds(postId);
            int likeCount = contentLikeService.getLikeCount(postId);

            Map<String, Object> response = new HashMap<>();
            response.put("userIds", userIds);
            response.put("likeCount", likeCount);
            response.put("total", userIds.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("获取点赞用户失败", e);
            return createErrorResponse("获取点赞用户失败", e.getMessage());
        }
    }

    /**
     * 获取帖子的最近点赞记录
     * GET /api/forum/posts/{postId}/likes/recent
     */
    @GetMapping("/{postId}/likes/recent")
    public ResponseEntity<?> getRecentLikes(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "10") int limit) {

        logger.info("获取最近点赞 - 帖子ID: {}, 限制: {}", postId, limit);

        try {
            List<UserContentRelation> recentLikes = contentLikeService.getContentRecentLikes(postId, limit);
            int likeCount = contentLikeService.getLikeCount(postId);

            Map<String, Object> response = new HashMap<>();
            response.put("recentLikes", recentLikes);
            response.put("likeCount", likeCount);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("获取最近点赞失败", e);
            return createErrorResponse("获取最近点赞失败", e.getMessage());
        }
    }

    /**
     * 批量检查点赞状态（用于列表页）
     * POST /api/forum/posts/likes/batch-check
     */
    @PostMapping("/likes/batch-check")
    @RequireForumAuth
    public ResponseEntity<?> batchCheckLikeStatus(
            @RequestBody Map<String, List<Long>> request,
            HttpServletRequest httpRequest) {

        Long userId = (Long) httpRequest.getAttribute("userId");
        logger.info("批量检查点赞状态 - 用户ID: {}", userId);

        try {
            if (userId == null) {
                return createErrorResponse("需要登录", "请先登录", HttpStatus.UNAUTHORIZED);
            }

            List<Long> postIds = request.get("postIds");
            if (postIds == null || postIds.isEmpty()) {
                return createErrorResponse("参数错误", "帖子ID列表不能为空", HttpStatus.BAD_REQUEST);
            }

            Map<Long, Boolean> likeStatus = contentLikeService.batchCheckLikeStatus(userId, postIds);

            Map<String, Object> response = new HashMap<>();
            response.put("likeStatus", likeStatus);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("批量检查失败", e);
            return createErrorResponse("批量检查失败", e.getMessage());
        }
    }

    /**
     * 批量获取点赞数（用于列表页）
     * POST /api/forum/posts/likes/batch-counts
     */
    @PostMapping("/likes/batch-counts")
    public ResponseEntity<?> batchGetLikeCounts(@RequestBody Map<String, List<Long>> request) {
        logger.info("批量获取点赞数");

        try {
            List<Long> postIds = request.get("postIds");
            if (postIds == null || postIds.isEmpty()) {
                return createErrorResponse("参数错误", "帖子ID列表不能为空", HttpStatus.BAD_REQUEST);
            }

            Map<Long, Integer> likeCounts = contentLikeService.batchGetLikeCounts(postIds);

            Map<String, Object> response = new HashMap<>();
            response.put("likeCounts", likeCounts);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("批量获取点赞数失败", e);
            return createErrorResponse("批量获取点赞数失败", e.getMessage());
        }
    }

    /**
     * 获取用户最近点赞的帖子
     * GET /api/forum/posts/likes/my-recent
     */
    @GetMapping("/likes/my-recent")
    @RequireForumAuth
    public ResponseEntity<?> getMyRecentLikes(
            @RequestParam(defaultValue = "20") int limit,
            HttpServletRequest request) {

        Long userId = (Long) request.getAttribute("userId");
        logger.info("获取我的最近点赞 - 用户ID: {}, 限制: {}", userId, limit);

        try {
            if (userId == null) {
                return createErrorResponse("需要登录", "请先登录", HttpStatus.UNAUTHORIZED);
            }

            List<UserContentRelation> recentLikes = contentLikeService.getUserRecentLikes(userId, limit);

            Map<String, Object> response = new HashMap<>();
            response.put("recentLikes", recentLikes);
            response.put("total", recentLikes.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("获取最近点赞失败", e);
            return createErrorResponse("获取最近点赞失败", e.getMessage());
        }
    }

    /**
     * 获取热门帖子（按点赞数排序）
     * GET /api/forum/posts/likes/popular
     */
    @GetMapping("/likes/popular")
    public ResponseEntity<?> getPopularPosts(@RequestParam(defaultValue = "20") int limit) {
        logger.info("获取热门帖子 - 限制: {}", limit);

        try {
            List<Long> postIds = contentLikeService.getTopLikedContents(limit);

            Map<String, Object> response = new HashMap<>();
            response.put("postIds", postIds);
            response.put("total", postIds.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("获取热门帖子失败", e);
            return createErrorResponse("获取热门帖子失败", e.getMessage());
        }
    }
    /**
     * 创建回复
     * POST /api/forum/posts/{postId}/replies
     */
    @PostMapping("/{postId}/replies")
    @RequireForumAuth
    public ResponseEntity<?> createReply(
            @PathVariable Long postId,
            @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest) {

        Long userId = (Long) httpRequest.getAttribute("userId");
        String body = request.get("body");

        logger.info("创建回复 - 帖子ID: {}, 用户ID: {}", postId, userId);

        try {
            if (userId == null) {
                return createErrorResponse("需要登录", "请先登录", HttpStatus.UNAUTHORIZED);
            }

            if (body == null || body.trim().isEmpty()) {
                return createErrorResponse("参数错误", "回复内容不能为空", HttpStatus.BAD_REQUEST);
            }

            if (!userService.isUserActive(userId)) {
                return createErrorResponse("用户无效", "用户不存在或已被禁用", HttpStatus.FORBIDDEN);
            }

            // 创建回复
            ForumContent reply = postService.createReply(postId, body, userId);

            // 获取作者信息
            ForumUser author = getUserSafely(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("reply", reply);
            response.put("authorName", author != null ? author.getUsername() : null);
            response.put("authorNickname", author != null ? author.getNickname() : null);
            response.put("message", "回复创建成功");

            logger.info("回复创建成功 - 回复ID: {}", reply.getContentId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            logger.warn("参数错误: {}", e.getMessage());
            return createErrorResponse("参数错误", e.getMessage(), HttpStatus.BAD_REQUEST);

        } catch (Exception e) {
            logger.error("创建回复失败", e);
            return createErrorResponse("创建回复失败", e.getMessage());
        }
    }

    /**
     * 获取帖子的回复列表
     * GET /api/forum/posts/{postId}/replies
     */
    /**
     * 获取帖子的回复列表
     * GET /api/forum/posts/{postId}/replies
     */
    @GetMapping("/{postId}/replies")
    public ResponseEntity<?> getReplies(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        logger.info("获取回复列表 - 帖子ID: {}, 页码: {}, 每页: {}", postId, page, size);

        try {
            // 🔥 修复：传入 page 和 size 参数
            List<ForumContent> replies = postService.getRepliesByPostId(postId, page, size);
            int totalCount = postService.getReplyCountByPostId(postId);

            // 为每个回复添加作者信息
            List<Map<String, Object>> replyDTOs = new ArrayList<>();
            for (ForumContent reply : replies) {
                ForumUser author = getUserSafely(reply.getAuthorId());

                Map<String, Object> dto = new HashMap<>();
                dto.put("replyId", reply.getContentId());
                dto.put("body", reply.getBody());
                dto.put("bodyPlain", reply.getBodyPlain());
                dto.put("authorId", reply.getAuthorId());
                dto.put("authorName", author != null ? author.getUsername() : null);
                dto.put("authorNickname", author != null ? author.getNickname() : null);
                dto.put("authorAvatarUrl", author != null ? author.getAvatarUrl() : null);
                dto.put("likeCount", reply.getLikeCount() != null ? reply.getLikeCount() : 0);
                dto.put("createdDate", reply.getCreatedDate());
                dto.put("updatedDate", reply.getUpdatedDate());

                replyDTOs.add(dto);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("replies", replyDTOs);
            response.put("totalCount", totalCount);
            response.put("currentPage", page);
            response.put("pageSize", size);
            response.put("totalPages", (int) Math.ceil((double) totalCount / size));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("获取回复列表失败", e);
            return createErrorResponse("获取回复列表失败", e.getMessage());
        }
    }

    /**
     * 删除回复
     * DELETE /api/forum/posts/{postId}/replies/{replyId}
     */
    @DeleteMapping("/{postId}/replies/{replyId}")
    @RequireForumAuth
    public ResponseEntity<?> deleteReply(
            @PathVariable Long postId,
            @PathVariable Long replyId,
            HttpServletRequest request) {

        Long userId = (Long) request.getAttribute("userId");
        logger.info("删除回复 - 帖子ID: {}, 回复ID: {}, 用户ID: {}", postId, replyId, userId);

        try {
            if (userId == null) {
                return createErrorResponse("需要登录", "请先登录", HttpStatus.UNAUTHORIZED);
            }

            postService.deleteReply(replyId, userId);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "回复删除成功");

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            if (e.getMessage().contains("权限")) {
                return createErrorResponse("权限不足", e.getMessage(), HttpStatus.FORBIDDEN);
            } else if (e.getMessage().contains("不存在")) {
                return createErrorResponse("回复不存在", e.getMessage(), HttpStatus.NOT_FOUND);
            } else {
                return createErrorResponse("删除失败", e.getMessage());
            }
        } catch (Exception e) {
            logger.error("删除回复失败", e);
            return createErrorResponse("删除回复失败", e.getMessage());
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 转换为响应 DTO 列表
     */
    private List<PostResponseDTO> convertToResponseDTOs(List<ForumContent> posts) {
        List<PostResponseDTO> postDTOs = new ArrayList<>();
        for (ForumContent post : posts) {
            ForumUser author = getUserSafely(post.getAuthorId());
            PostResponseDTO dto = PostResponseDTO.fromContentAndUser(post, author);
            postDTOs.add(dto);
        }
        return postDTOs;
    }

    /**
     * 安全获取用户信息
     */
    private ForumUser getUserSafely(Long userId) {
        try {
            return userService.getUserById(userId);
        } catch (Exception e) {
            logger.warn("获取用户信息失败 - 用户ID: {}", userId);
            return null;
        }
    }

    /**
     * 创建错误响应
     */
    private ResponseEntity<Map<String, Object>> createErrorResponse(String error, String message) {
        return createErrorResponse(error, message, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * 创建错误响应
     */
    private ResponseEntity<Map<String, Object>> createErrorResponse(String error, String message, HttpStatus status) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", error);
        errorResponse.put("message", message);
        errorResponse.put("timestamp", LocalDateTime.now());
        return ResponseEntity.status(status).body(errorResponse);
    }
}

