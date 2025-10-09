package com.sg.nusiss.gamevaultbackend.mapper.forum;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.sg.nusiss.gamevaultbackend.entity.forum.UserContentRelation;

import java.util.List;

/**
 * 内容点赞 Mapper（使用 user_content_relations 表）
 */
@Mapper
public interface ForumContentLikeMapper {

    // ==================== 基础操作 ====================

    /**
     * 插入点赞关系
     * 会触发数据库触发器自动更新 content_metrics
     */
    int insert(UserContentRelation relation);

    /**
     * 删除点赞关系
     */
    int deleteByUserAndContentAndType(
            @Param("userId") Long userId,
            @Param("contentId") Long contentId,
            @Param("relationType") Long relationType
    );

    /**
     * 查找点赞关系
     */
    UserContentRelation findByUserAndContentAndType(
            @Param("userId") Long userId,
            @Param("contentId") Long contentId,
            @Param("relationType") Long relationType
    );

    // ==================== 查询操作 ====================

    /**
     * 检查是否已点赞
     */
    boolean existsByUserAndContentAndType(
            @Param("userId") Long userId,
            @Param("contentId") Long contentId,
            @Param("relationType") Long relationType
    );

    /**
     * 统计某内容的点赞数
     */
    int countByContentAndType(
            @Param("contentId") Long contentId,
            @Param("relationType") Long relationType
    );

    /**
     * 获取某内容的所有点赞用户ID
     */
    List<Long> findUserIdsByContentAndType(
            @Param("contentId") Long contentId,
            @Param("relationType") Long relationType
    );

    /**
     * 获取用户点赞的所有内容ID
     */
    List<Long> findContentIdsByUserAndType(
            @Param("userId") Long userId,
            @Param("relationType") Long relationType
    );

    /**
     * 批量检查用户对多个内容的点赞状态
     */
    List<Long> findLikedContentIdsByUserAndType(
            @Param("userId") Long userId,
            @Param("contentIds") List<Long> contentIds,
            @Param("relationType") Long relationType
    );

    /**
     * 获取用户最近点赞的内容
     */
    List<UserContentRelation> findRecentByUserAndType(
            @Param("userId") Long userId,
            @Param("relationType") Long relationType,
            @Param("limit") int limit
    );

    /**
     * 获取内容的最近点赞记录
     */
    List<UserContentRelation> findRecentByContentAndType(
            @Param("contentId") Long contentId,
            @Param("relationType") Long relationType,
            @Param("limit") int limit
    );

    // ==================== 批量操作 ====================

    /**
     * 删除某内容的所有点赞
     */
    int deleteByContentAndType(
            @Param("contentId") Long contentId,
            @Param("relationType") Long relationType
    );

    /**
     * 删除某用户的所有点赞
     */
    int deleteByUserAndType(
            @Param("userId") Long userId,
            @Param("relationType") Long relationType
    );
}

