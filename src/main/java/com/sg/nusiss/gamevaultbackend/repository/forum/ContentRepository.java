package com.sg.nusiss.gamevaultbackend.repository.forum;

import com.sg.nusiss.gamevaultbackend.entity.forum.Content;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 论坛帖子Repository
 */
@Repository
public interface ContentRepository extends JpaRepository<Content, Long> {
    
    /**
     * 获取活跃状态的帖子列表（分页）
     */
    @Query("SELECT c FROM Content c WHERE c.contentType = 'post' AND c.status = 'active' ORDER BY c.createdDate DESC")
    Page<Content> findActivePosts(Pageable pageable);
    
    /**
     * 根据作者ID获取帖子列表（分页）
     */
    @Query("SELECT c FROM Content c WHERE c.authorId = :authorId AND c.contentType = 'post' AND c.status = 'active' ORDER BY c.createdDate DESC")
    Page<Content> findByAuthorIdAndStatus(@Param("authorId") Long authorId, Pageable pageable);
    
    /**
     * 搜索帖子（标题和内容包含关键词）
     */
    @Query("SELECT c FROM Content c WHERE c.contentType = 'post' AND c.status = 'active' AND " +
           "(LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(c.body) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(c.bodyPlain) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY c.createdDate DESC")
    Page<Content> searchPosts(@Param("keyword") String keyword, Pageable pageable);
    
    /**
     * 增加浏览次数（使用数据库函数）
     */
    @Modifying
    @Query(value = "SELECT increment_metric(:contentId, 'view_count')", nativeQuery = true)
    void incrementViewCount(@Param("contentId") Long contentId);
    
    /**
     * 统计活跃帖子总数
     */
    @Query("SELECT COUNT(c) FROM Content c WHERE c.contentType = 'post' AND c.status = 'active'")
    long countActivePosts();
    
    /**
     * 统计搜索结果的帖子总数
     */
    @Query("SELECT COUNT(c) FROM Content c WHERE c.contentType = 'post' AND c.status = 'active' AND " +
           "(LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(c.body) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(c.bodyPlain) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    long countSearchResults(@Param("keyword") String keyword);
    
    /**
     * 统计指定作者的帖子总数
     */
    @Query("SELECT COUNT(c) FROM Content c WHERE c.authorId = :authorId AND c.contentType = 'post' AND c.status = 'active'")
    long countByAuthorId(@Param("authorId") Long authorId);
    
    /**
     * 检查帖子是否属于指定用户
     */
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Content c WHERE c.contentId = :contentId AND c.authorId = :authorId")
    boolean existsByContentIdAndAuthorId(@Param("contentId") Long contentId, @Param("authorId") Long authorId);
    
    /**
     * 获取帖子的浏览次数
     */
    @Query(value = "SELECT COALESCE(cm.metric_value, 0) FROM content_metrics cm " +
                   "JOIN metric_definitions md ON cm.metric_id = md.metric_id " +
                   "WHERE cm.content_id = :contentId AND md.metric_name = 'view_count'", 
           nativeQuery = true)
    Integer getViewCount(@Param("contentId") Long contentId);
    
    /**
     * 获取帖子的点赞数
     */
    @Query(value = "SELECT COALESCE(cm.metric_value, 0) FROM content_metrics cm " +
                   "JOIN metric_definitions md ON cm.metric_id = md.metric_id " +
                   "WHERE cm.content_id = :contentId AND md.metric_name = 'like_count'", 
           nativeQuery = true)
    Integer getLikeCount(@Param("contentId") Long contentId);
}
