package com.sg.nusiss.gamevaultbackend.mapper.forum;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.sg.nusiss.gamevaultbackend.entity.forum.ForumContent;
import java.util.List;

/**
 * 内容 Mapper 接口
 * 处理帖子的 CRUD 操作
 */
@Mapper
public interface ForumContentMapper {

    // ==================== 基础 CRUD ====================

    /**
     * 根据ID查询内容
     */
    ForumContent findById(@Param("contentId") Long contentId);

    /**
     * 插入新内容
     */
    int insert(ForumContent content);

    /**
     * 更新内容
     */
    int update(ForumContent content);

    /**
     * 软删除内容
     */
    int softDelete(@Param("contentId") Long contentId);

    // ==================== 帖子相关查询 ====================

    /**
     * 查询所有活跃帖子（分页）
     * @param offset 偏移量
     * @param limit 限制数量
     */
    List<ForumContent> findActivePosts(@Param("offset") int offset, @Param("limit") int limit);

    /**
     * 统计活跃帖子总数
     */
    int countActivePosts();

    /**
     * 根据作者ID查询帖子
     */
    List<ForumContent> findPostsByAuthor(@Param("authorId") Long authorId);

    /**
     * 搜索帖子（按标题和内容）
     * @param keyword 搜索关键词
     * @param offset 偏移量
     * @param limit 限制数量
     */
    List<ForumContent> searchPosts(@Param("keyword") String keyword,
                              @Param("offset") int offset,
                              @Param("limit") int limit);

    /**
     * 统计搜索结果数量
     */
    int countSearchPosts(@Param("keyword") String keyword);

    // ==================== 层级结构查询 ====================

    /**
     * 查询子内容（回复）
     */
    /**
     * 查询子内容（回复）- 带分页
     */
    List<ForumContent> findChildren(
            @Param("parentId") Long parentId,
            @Param("offset") int offset,
            @Param("limit") int limit
    );

    /**
     * 统计子内容数量
     */
    int countChildren(@Param("parentId") Long parentId);

    // ==================== 统计相关 ====================

    /**
     * 增加浏览量
     * 这个方法通常会更新 content_metrics 表中的浏览量字段
     */
    int incrementViewCount(@Param("contentId") Long contentId);

    /**
     * 增加点赞数
     */
    int incrementLikeCount(@Param("contentId") Long contentId);

    /**
     * 减少点赞数
     */
    int decrementLikeCount(@Param("contentId") Long contentId);

    /**
     * 更新回复数量
     */
    int updateReplyCount(@Param("contentId") Long contentId, @Param("count") int count);


    List<ForumContent> findByAuthorId(@Param("authorId") Long authorId,
                                 @Param("offset") int offset,
                                 @Param("limit") int limit);

    int countByAuthorId(@Param("authorId") Long authorId);

    /**
     * 查询用户的活跃帖子（未删除）
     */
    List<ForumContent> selectActiveByAuthorId(
            @Param("authorId") Long authorId,
            @Param("offset") int offset,
            @Param("size") int size
    );

    /**
     * 统计用户的活跃帖子数（未删除）
     */
    int countActiveByAuthorId(@Param("authorId") Long authorId);
}

