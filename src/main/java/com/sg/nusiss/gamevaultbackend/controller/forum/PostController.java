package com.sg.nusiss.gamevaultbackend.controller.forum;

import com.sg.nusiss.gamevaultbackend.common.BaseResponse;
import com.sg.nusiss.gamevaultbackend.common.ResultUtils;
import com.sg.nusiss.gamevaultbackend.dto.forum.PostDTO;
import com.sg.nusiss.gamevaultbackend.dto.forum.PostResponseDTO;
import com.sg.nusiss.gamevaultbackend.entity.forum.Content;
import com.sg.nusiss.gamevaultbackend.entity.auth.User;
import com.sg.nusiss.gamevaultbackend.service.forum.PostService;
import com.sg.nusiss.gamevaultbackend.repository.auth.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 论坛帖子控制器
 * 提供帖子相关的 REST API
 */
@RestController
@RequestMapping("/api/forum/posts")
@CrossOrigin(origins = "*")
public class PostController {

    private static final Logger logger = LoggerFactory.getLogger(PostController.class);

    @Autowired
    private PostService postService;
    
    @Autowired
    private UserRepository userRepository;

    /**
     * 获取帖子列表（分页）
     */
    @GetMapping(produces = "application/json")
    public BaseResponse<Map<String, Object>> getPostList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        logger.info("获取帖子列表 - 页码: {}, 每页大小: {}", page, size);

        try {
            List<Content> posts = postService.getPostList(page, size);
            int totalCount = postService.getPostCount();

            List<PostResponseDTO> postDTOs = postService.convertToResponseDTOs(posts);

            Map<String, Object> response = new HashMap<>();
            response.put("posts", postDTOs);
            response.put("currentPage", page);
            response.put("pageSize", size);
            response.put("totalCount", totalCount);
            response.put("totalPages", (int) Math.ceil((double) totalCount / size));

            return ResultUtils.success(response);

        } catch (Exception e) {
            logger.error("获取帖子列表失败", e);
            return ResultUtils.error(500, "获取帖子列表失败: " + e.getMessage());
        }
    }

    /**
     * 根据ID获取帖子详情
     */
    @GetMapping(value = "/{id}", produces = "application/json")
    public BaseResponse<Map<String, Object>> getPostById(@PathVariable Long id) {
        logger.info("获取帖子详情 - 帖子ID: {}", id);

        try {
            Content post = postService.getPostById(id);
            postService.incrementViewCount(id);

            User author = postService.getUserSafely(post.getAuthorId());
            PostResponseDTO dto = PostResponseDTO.fromContentAndUser(post, author);

            Map<String, Object> response = new HashMap<>();
            response.put("post", dto);

            return ResultUtils.success(response);

        } catch (IllegalArgumentException e) {
            logger.warn("参数错误: {}", e.getMessage());
            return ResultUtils.error(400, "参数错误: " + e.getMessage());

        } catch (RuntimeException e) {
            logger.warn("帖子不存在: {}", e.getMessage());
            return ResultUtils.error(404, "帖子不存在: " + e.getMessage());

        } catch (Exception e) {
            logger.error("获取帖子详情失败", e);
            return ResultUtils.error(500, "获取帖子详情失败: " + e.getMessage());
        }
    }

    /**
     * 创建新帖子
     */
    @PostMapping
    public BaseResponse<Map<String, Object>> createPost(
            @Valid @RequestBody PostDTO postDTO,
            @AuthenticationPrincipal Jwt jwt) {
        
        try {
            // 从JWT获取用户ID
            Long userId = null;
            Object uidClaim = jwt.getClaims().get("uid");
            if (uidClaim instanceof Number) {
                userId = ((Number) uidClaim).longValue();
            }
            
            if (userId == null) {
                return ResultUtils.error(401, "需要登录");
            }

            logger.info("创建帖子 - 用户ID: {}, 标题: {}", userId, postDTO.getTitle());

            // 验证用户状态
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                return ResultUtils.error(403, "用户不存在");
            }

            // 创建帖子
            Content post = postService.createPost(postDTO.getTitle(), postDTO.getBody(), userId);
            User author = postService.getUserSafely(userId);
            PostResponseDTO dto = PostResponseDTO.fromContentAndUser(post, author);

            Map<String, Object> response = new HashMap<>();
            response.put("post", dto);
            response.put("message", "帖子创建成功");

            logger.info("帖子创建成功 - 帖子ID: {}", post.getContentId());
            return ResultUtils.success(response);

        } catch (IllegalArgumentException e) {
            logger.warn("参数错误: {}", e.getMessage());
            return ResultUtils.error(400, "参数错误: " + e.getMessage());

        } catch (Exception e) {
            logger.error("创建帖子失败", e);
            return ResultUtils.error(500, "创建帖子失败: " + e.getMessage());
        }
    }

    /**
     * 搜索帖子
     */
    @GetMapping(value = "/search", produces = "application/json")
    public BaseResponse<Map<String, Object>> searchPosts(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        logger.info("搜索帖子 - 关键词: {}", keyword);

        try {
            List<Content> posts = postService.searchPosts(keyword, page, size);
            int totalCount = postService.getSearchCount(keyword);

            List<PostResponseDTO> postDTOs = postService.convertToResponseDTOs(posts);

            Map<String, Object> response = new HashMap<>();
            response.put("posts", postDTOs);
            response.put("keyword", keyword);
            response.put("currentPage", page);
            response.put("pageSize", size);
            response.put("totalCount", totalCount);

            return ResultUtils.success(response);

        } catch (Exception e) {
            logger.error("搜索帖子失败", e);
            return ResultUtils.error(500, "搜索帖子失败: " + e.getMessage());
        }
    }

    /**
     * 删除帖子
     */
    @DeleteMapping("/{id}")
    public BaseResponse<Map<String, Object>> deletePost(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {

        try {
            // 从JWT获取用户ID
            Long userId = null;
            Object uidClaim = jwt.getClaims().get("uid");
            if (uidClaim instanceof Number) {
                userId = ((Number) uidClaim).longValue();
            }
            
            if (userId == null) {
                return ResultUtils.error(401, "需要登录");
            }

            logger.info("删除帖子 - 帖子ID: {}, 用户ID: {}", id, userId);

            postService.deletePost(id, userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "帖子删除成功");
            return ResultUtils.success(response);

        } catch (RuntimeException e) {
            if (e.getMessage().contains("权限")) {
                return ResultUtils.error(403, "权限不足: " + e.getMessage());
            } else {
                return ResultUtils.error(404, "帖子不存在: " + e.getMessage());
            }
        } catch (Exception e) {
            logger.error("删除帖子失败", e);
            return ResultUtils.error(500, "删除帖子失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户的帖子列表
     */
    @GetMapping(value = "/user/{userId}", produces = "application/json")
    public BaseResponse<Map<String, Object>> getUserPosts(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        logger.info("获取用户帖子 - 用户ID: {}, 页码: {}", userId, page);

        try {
            List<Content> posts = postService.getPostsByAuthorId(userId, page, size);
            int totalCount = postService.getPostCountByAuthorId(userId);

            List<PostResponseDTO> postDTOs = postService.convertToResponseDTOs(posts);

            Map<String, Object> response = new HashMap<>();
            response.put("posts", postDTOs);
            response.put("currentPage", page);
            response.put("pageSize", size);
            response.put("totalCount", totalCount);
            response.put("totalPages", (int) Math.ceil((double) totalCount / size));

            return ResultUtils.success(response);

        } catch (Exception e) {
            logger.error("获取用户帖子失败", e);
            return ResultUtils.error(500, "获取用户帖子失败: " + e.getMessage());
        }
    }
}
