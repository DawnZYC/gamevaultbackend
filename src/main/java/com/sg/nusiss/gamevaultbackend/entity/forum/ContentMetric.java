package com.sg.nusiss.gamevaultbackend.entity.forum;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 内容统计实体类
 * 对应数据库 content_metrics 表
 * 用于存储浏览量、点赞数、回复数等统计数据
 */
@Setter
@Getter
public class ContentMetric {
    // Getters and Setters
    private Long id;
    private Long contentId;
    private Long metricId;
    private Integer metricValue;
    private LocalDateTime updatedDate;

    // 默认构造函数
    public ContentMetric() {}

    // 完整构造函数
    public ContentMetric(Long contentId, Long metricId, Integer metricValue) {
        this.contentId = contentId;
        this.metricId = metricId;
        this.metricValue = metricValue;
        this.updatedDate = LocalDateTime.now();
    }

    // 业务方法
    public void increment() {
        this.metricValue = (this.metricValue != null ? this.metricValue : 0) + 1;
        this.updatedDate = LocalDateTime.now();
    }

    public void increment(int amount) {
        this.metricValue = (this.metricValue != null ? this.metricValue : 0) + amount;
        this.updatedDate = LocalDateTime.now();
    }

    public void decrement() {
        this.metricValue = Math.max(0, (this.metricValue != null ? this.metricValue : 0) - 1);
        this.updatedDate = LocalDateTime.now();
    }

    public void setValue(int value) {
        this.metricValue = Math.max(0, value);
        this.updatedDate = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "ContentMetric{" +
                "id=" + id +
                ", contentId=" + contentId +
                ", metricId=" + metricId +
                ", metricValue=" + metricValue +
                ", updatedDate=" + updatedDate +
                '}';
    }
}

