package com.sg.nusiss.gamevaultbackend.config.forum;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 论坛分页配置属性类
 * 绑定 application.properties 中的 app.pagination.* 配置
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.pagination")
public class ForumPaginationConfig {

    /**
     * 默认每页大小
     */
    private int defaultPageSize = 20;

    /**
     * 最大每页大小
     */
    private int maxPageSize = 100;

    // 默认构造函数
    public ForumPaginationConfig() {}



    /**
     * 验证并调整页面大小
     */
    public int validatePageSize(int size) {
        if (size <= 0) {
            return defaultPageSize;
        }
        return Math.min(size, maxPageSize);
    }

    /**
     * 验证并调整页码
     */
    public int validatePageNumber(int page) {
        return Math.max(0, page);
    }

    @Override
    public String toString() {
        return "ForumPaginationConfig{" +
                "defaultPageSize=" + defaultPageSize +
                ", maxPageSize=" + maxPageSize +
                '}';
    }
}

