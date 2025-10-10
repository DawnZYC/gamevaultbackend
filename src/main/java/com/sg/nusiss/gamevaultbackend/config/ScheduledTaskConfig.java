package com.sg.nusiss.gamevaultbackend.config;

import com.sg.nusiss.gamevaultbackend.service.forum.ViewTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * 定时任务配置
 */
@Configuration
@EnableScheduling
public class ScheduledTaskConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(ScheduledTaskConfig.class);
    
    @Autowired
    private ViewTracker viewTracker;
    
    /**
     * 每10分钟清理一次过期的浏览记录
     */
    @Scheduled(fixedRate = 600000) // 10分钟 = 600,000毫秒
    public void cleanExpiredViewRecords() {
        try {
            int beforeCount = viewTracker.getRecordCount();
            viewTracker.cleanExpiredRecords();
            int afterCount = viewTracker.getRecordCount();
            
            if (beforeCount > afterCount) {
                logger.info("清理过期浏览记录完成 - 清理前: {}, 清理后: {}, 清理数量: {}", 
                    beforeCount, afterCount, (beforeCount - afterCount));
            }
        } catch (Exception e) {
            logger.error("清理过期浏览记录失败", e);
        }
    }
}

