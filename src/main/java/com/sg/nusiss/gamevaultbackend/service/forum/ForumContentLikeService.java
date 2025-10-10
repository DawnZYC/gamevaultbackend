package com.sg.nusiss.gamevaultbackend.service.forum;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.sg.nusiss.gamevaultbackend.constant.forum.ForumRelationType;
import com.sg.nusiss.gamevaultbackend.entity.forum.UserContentRelation;
import com.sg.nusiss.gamevaultbackend.mapper.forum.ForumContentLikeMapper;
import com.sg.nusiss.gamevaultbackend.mapper.forum.ForumMetricMapper;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 内容点赞服务（基于 user_content_relations 表）
 *
 * 工作原理：
 * 1. 插入/删除 user_content_relations 记录
 * 2. 数据库触发器自动更新 content_metrics 表的 like_count
 * 3. 通过 MetricMapper 查询最新的统计数据
 */
@Service
public class ForumContentLikeService {

    @Autowired
    private ForumContentLikeMapper contentLikeMapper;

    @Autowired
    private ForumMetricMapper metricMapper;

    /**
     * 点赞内容
     * @param contentId 内容ID
     * @param userId 用户ID
     * @return 点赞是否成功
     */
    @Transactional
    public boolean likeContent(Long contentId, Long userId) {
        // 验证参数
        if (contentId == null || userId == null) {
            throw new IllegalArgumentException("内容ID和用户ID不能为空");
        }

        // 检查是否已点赞
        if (contentLikeMapper.existsByUserAndContentAndType(userId, contentId, ForumRelationType.LIKE)) {
            return false; // 已经点赞过
        }

        // 创建点赞关系
        UserContentRelation relation = new UserContentRelation(userId, contentId, ForumRelationType.LIKE);
        relation.setCreatedDate(LocalDateTime.now());

        int inserted = contentLikeMapper.insert(relation);

        // 数据库触发器会自动更新 content_metrics 表
        return inserted > 0;
    }

    /**
     * 取消点赞
     * @param contentId 内容ID
     * @param userId 用户ID
     * @return 取消点赞是否成功
     */
    @Transactional
    public boolean unlikeContent(Long contentId, Long userId) {
        // 验证参数
        if (contentId == null || userId == null) {
            throw new IllegalArgumentException("内容ID和用户ID不能为空");
        }

        // 检查是否已点赞
        if (!contentLikeMapper.existsByUserAndContentAndType(userId, contentId, ForumRelationType.LIKE)) {
            return false; // 未点赞过
        }

        // 删除点赞关系
        int deleted = contentLikeMapper.deleteByUserAndContentAndType(userId, contentId, ForumRelationType.LIKE);

        // 数据库触发器会自动更新 content_metrics 表
        return deleted > 0;
    }

    /**
     * 切换点赞状态（点赞/取消点赞）
     * @param contentId 内容ID
     * @param userId 用户ID
     * @return true表示点赞，false表示取消点赞
     */
    @Transactional
    public boolean toggleLike(Long contentId, Long userId) {
        if (contentLikeMapper.existsByUserAndContentAndType(userId, contentId, ForumRelationType.LIKE)) {
            unlikeContent(contentId, userId);
            return false;
        } else {
            likeContent(contentId, userId);
            return true;
        }
    }

    /**
     * 检查用户是否已点赞某内容
     */
    public boolean isLiked(Long contentId, Long userId) {
        if (contentId == null || userId == null) {
            return false;
        }
        return contentLikeMapper.existsByUserAndContentAndType(userId, contentId, ForumRelationType.LIKE);
    }

    /**
     * 获取内容的点赞数（直接从 user_content_relations 表统计）
     */
    public int getLikeCount(Long contentId) {
        if (contentId == null) {
            return 0;
        }

        // 直接从关系表统计实际点赞数，避免依赖 content_metrics 表
        return contentLikeMapper.countByContentAndType(contentId, ForumRelationType.LIKE);
    }

    /**
     * 获取内容的所有点赞用户ID
     */
    public List<Long> getLikedUserIds(Long contentId) {
        if (contentId == null) {
            throw new IllegalArgumentException("内容ID不能为空");
        }
        return contentLikeMapper.findUserIdsByContentAndType(contentId, ForumRelationType.LIKE);
    }

    /**
     * 获取用户点赞的所有内容ID
     */
    public List<Long> getUserLikedContentIds(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        return contentLikeMapper.findContentIdsByUserAndType(userId, ForumRelationType.LIKE);
    }

    /**
     * 批量获取用户对多个内容的点赞状态
     * @param userId 用户ID
     * @param contentIds 内容ID列表
     * @return Map<内容ID, 是否已点赞>
     */
    public Map<Long, Boolean> batchCheckLikeStatus(Long userId, List<Long> contentIds) {
        if (userId == null || contentIds == null || contentIds.isEmpty()) {
            return new HashMap<>();
        }

        List<Long> likedContentIds = contentLikeMapper
                .findLikedContentIdsByUserAndType(userId, contentIds, ForumRelationType.LIKE);

        Map<Long, Boolean> result = new HashMap<>();
        for (Long contentId : contentIds) {
            result.put(contentId, likedContentIds.contains(contentId));
        }

        return result;
    }

    /**
     * 批量获取多个内容的点赞数
     * @param contentIds 内容ID列表
     * @return Map<内容ID, 点赞数>
     */
    public Map<Long, Integer> batchGetLikeCounts(List<Long> contentIds) {
        if (contentIds == null || contentIds.isEmpty()) {
            return new HashMap<>();
        }

        // 使用 MetricMapper 的批量查询
        return metricMapper.getBatchMetrics(contentIds, "like_count");
    }

    /**
     * 获取用户最近点赞的内容
     */
    public List<UserContentRelation> getUserRecentLikes(Long userId, int limit) {
        if (userId == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        return contentLikeMapper.findRecentByUserAndType(userId, ForumRelationType.LIKE, limit);
    }

    /**
     * 获取内容的最近点赞记录
     */
    public List<UserContentRelation> getContentRecentLikes(Long contentId, int limit) {
        if (contentId == null) {
            throw new IllegalArgumentException("内容ID不能为空");
        }
        return contentLikeMapper.findRecentByContentAndType(contentId, ForumRelationType.LIKE, limit);
    }

    /**
     * 同步点赞数（修复数据不一致问题）
     * 从 user_content_relations 表统计实际数量，更新到 content_metrics 表
     */
    @Transactional
    public void syncLikeCount(Long contentId) {
        if (contentId == null) {
            throw new IllegalArgumentException("内容ID不能为空");
        }

        // 统计实际点赞数
        int actualCount = contentLikeMapper.countByContentAndType(contentId, ForumRelationType.LIKE);

        // 更新到 content_metrics 表
        metricMapper.setMetricValue(contentId, "like_count", actualCount);
    }

    /**
     * 批量同步点赞数
     */
    @Transactional
    public void batchSyncLikeCounts(List<Long> contentIds) {
        if (contentIds == null || contentIds.isEmpty()) {
            return;
        }

        for (Long contentId : contentIds) {
            syncLikeCount(contentId);
        }
    }

    /**
     * 获取热门内容（按点赞数排序）
     */
    public List<Long> getTopLikedContents(int limit) {
        return metricMapper.findTopContentsByMetric("like_count", limit);
    }
}

