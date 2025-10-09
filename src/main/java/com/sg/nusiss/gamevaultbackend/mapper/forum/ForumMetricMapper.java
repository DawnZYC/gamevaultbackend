package com.sg.nusiss.gamevaultbackend.mapper.forum;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.sg.nusiss.gamevaultbackend.entity.forum.ContentMetric;
import java.util.List;
import java.util.Map;

/**
 * 统计 Mapper 接口
 * 处理浏览量、点赞数等统计数据
 */
@Mapper
public interface ForumMetricMapper {

    // ==================== 基础操作 ====================

    /**
     * 插入或更新统计值
     */
    int insertOrUpdate(ContentMetric contentMetric);

    /**
     * 增加统计值
     * @param contentId 内容ID
     * @param metricName 统计类型名称（view_count, like_count等）
     * @param increment 增加数量
     */
    int incrementMetric(@Param("contentId") Long contentId,
                        @Param("metricName") String metricName,
                        @Param("increment") int increment);

    /**
     * 设置统计值
     */
    int setMetricValue(@Param("contentId") Long contentId,
                       @Param("metricName") String metricName,
                       @Param("value") int value);

    // ==================== 查询操作 ====================

    /**
     * 获取单个统计值
     */
    Integer getMetricValue(@Param("contentId") Long contentId,
                           @Param("metricName") String metricName);

    /**
     * 获取内容的所有统计数据
     * @return Map<metricName, metricValue>
     */
    Map<String, Integer> getContentMetrics(@Param("contentId") Long contentId);

    /**
     * 批量获取多个内容的统计数据
     * @param contentIds 内容ID列表
     * @param metricName 统计类型名称
     * @return Map<contentId, metricValue>
     */
    Map<Long, Integer> getBatchMetrics(@Param("contentIds") List<Long> contentIds,
                                       @Param("metricName") String metricName);

    /**
     * 查询热门内容（按统计值排序）
     */
    List<Long> findTopContentsByMetric(@Param("metricName") String metricName,
                                       @Param("limit") int limit);
}

