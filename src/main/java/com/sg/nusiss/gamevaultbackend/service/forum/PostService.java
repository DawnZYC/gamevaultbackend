package com.sg.nusiss.gamevaultbackend.service.forum;

import com.sg.nusiss.gamevaultbackend.dto.forum.PostDTO;
import com.sg.nusiss.gamevaultbackend.dto.forum.PostResponseDTO;
import com.sg.nusiss.gamevaultbackend.entity.forum.Content;
import com.sg.nusiss.gamevaultbackend.entity.auth.User;
import com.sg.nusiss.gamevaultbackend.repository.forum.ContentRepository;
import com.sg.nusiss.gamevaultbackend.repository.auth.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 论坛帖子服务
 */
@Service
@Transactional
public class PostService {
    
    private static final Logger logger = LoggerFactory.getLogger(PostService.class);
    
    @Autowired
    private ContentRepository contentRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * 获取帖子列表（分页）
     */
    public List<Content> getPostList(int page, int size) {
        logger.info("获取帖子列表 - 页码: {}, 每页大小: {}", page, size);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Content> contentPage = contentRepository.findActivePosts(pageable);
        return contentPage.getContent();
    }
    
    /**
     * 获取帖子总数
     */
    public int getPostCount() {
        return (int) contentRepository.countActivePosts();
    }
    
    /**
     * 根据ID获取帖子详情
     */
    public Content getPostById(Long id) {
        logger.info("获取帖子详情 - 帖子ID: {}", id);
        
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("帖子ID无效");
        }
        
        return contentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("帖子不存在"));
    }
    
    /**
     * 增加帖子浏览次数
     */
    public void incrementViewCount(Long contentId) {
        logger.info("增加帖子浏览次数 - 帖子ID: {}", contentId);
        contentRepository.incrementViewCount(contentId);
    }
    
    /**
     * 创建新帖子
     */
    public Content createPost(String title, String body, Long authorId) {
        logger.info("创建新帖子 - 标题: {}, 作者ID: {}", title, authorId);
        
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("标题不能为空");
        }
        if (body == null || body.trim().isEmpty()) {
            throw new IllegalArgumentException("内容不能为空");
        }
        if (authorId == null || authorId <= 0) {
            throw new IllegalArgumentException("作者ID无效");
        }
        
        // 验证作者是否存在
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new IllegalArgumentException("作者不存在"));
        
        // 生成纯文本内容（去除HTML标签）
        String bodyPlain = body.replaceAll("<[^>]*>", "").trim();
        
        Content content = new Content(title.trim(), body.trim(), bodyPlain, authorId);
        return contentRepository.save(content);
    }
    
    /**
     * 搜索帖子
     */
    public List<Content> searchPosts(String keyword, int page, int size) {
        logger.info("搜索帖子 - 关键词: {}", keyword);
        
        if (keyword == null || keyword.trim().isEmpty()) {
            throw new IllegalArgumentException("搜索关键词不能为空");
        }
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Content> contentPage = contentRepository.searchPosts(keyword.trim(), pageable);
        return contentPage.getContent();
    }
    
    /**
     * 获取搜索结果总数
     */
    public int getSearchCount(String keyword) {
        return (int) contentRepository.countSearchResults(keyword);
    }
    
    /**
     * 删除帖子
     */
    public void deletePost(Long contentId, Long userId) {
        logger.info("删除帖子 - 帖子ID: {}, 用户ID: {}", contentId, userId);
        
        if (contentId == null || contentId <= 0) {
            throw new IllegalArgumentException("帖子ID无效");
        }
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("用户ID无效");
        }
        
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new RuntimeException("帖子不存在"));
        
        // 检查权限：只有作者可以删除自己的帖子
        if (!content.getAuthorId().equals(userId)) {
            throw new RuntimeException("权限不足：只能删除自己的帖子");
        }
        
        content.delete();
        contentRepository.save(content);
    }
    
    /**
     * 获取用户的帖子列表
     */
    public List<Content> getPostsByAuthorId(Long authorId, int page, int size) {
        logger.info("获取用户帖子 - 作者ID: {}, 页码: {}", authorId, page);
        
        if (authorId == null || authorId <= 0) {
            throw new IllegalArgumentException("作者ID无效");
        }
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Content> contentPage = contentRepository.findByAuthorIdAndStatus(authorId, pageable);
        return contentPage.getContent();
    }
    
    /**
     * 获取用户的帖子总数
     */
    public int getPostCountByAuthorId(Long authorId) {
        return (int) contentRepository.countByAuthorId(authorId);
    }
    
    /**
     * 转换为响应DTO列表
     */
    public List<PostResponseDTO> convertToResponseDTOs(List<Content> posts) {
        return posts.stream()
                .map(post -> {
                    User author = userRepository.findById(post.getAuthorId()).orElse(null);
                    PostResponseDTO dto = PostResponseDTO.fromContentAndUser(post, author);
                    
                    // 获取统计数据
                    Integer viewCount = contentRepository.getViewCount(post.getContentId());
                    Integer likeCount = contentRepository.getLikeCount(post.getContentId());
                    
                    dto.setViewCount(viewCount != null ? viewCount : 0);
                    dto.setLikeCount(likeCount != null ? likeCount : 0);
                    
                    return dto;
                })
                .collect(Collectors.toList());
    }
    
    /**
     * 安全获取用户信息
     */
    public User getUserSafely(Long userId) {
        try {
            return userRepository.findById(userId).orElse(null);
        } catch (Exception e) {
            logger.warn("获取用户信息失败 - 用户ID: {}", userId, e);
            return null;
        }
    }
}
