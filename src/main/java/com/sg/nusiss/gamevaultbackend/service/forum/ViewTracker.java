package com.sg.nusiss.gamevaultbackend.service.forum;

import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 浏览记录追踪器
 * 用于防止短时间内重复计数浏览量
 */
@Service
public class ViewTracker {
    
    // 存储格式: "userId_postId" -> lastViewTime
    // 如果userId为null，使用sessionId
    private final Map<String, Long> viewRecords = new ConcurrentHashMap<>();
    
    // 时间窗口：5分钟内不重复计数
    private static final long VIEW_WINDOW_MS = TimeUnit.MINUTES.toMillis(5);
    
    /**
     * 检查是否应该增加浏览量
     * @param userId 用户ID（可能为null，未登录用户）
     * @param sessionId Session ID（用于未登录用户）
     * @param postId 帖子ID
     * @return true如果应该增加浏览量，false如果在时间窗口内已浏览过
     */
    public boolean shouldIncrementView(Long userId, String sessionId, Long postId) {
        String key = buildKey(userId, sessionId, postId);
        long currentTime = System.currentTimeMillis();
        
        Long lastViewTime = viewRecords.get(key);
        
        // 如果从未浏览过，或者超过时间窗口
        if (lastViewTime == null || (currentTime - lastViewTime) > VIEW_WINDOW_MS) {
            viewRecords.put(key, currentTime);
            return true;
        }
        
        // 在时间窗口内，不增加浏览量
        return false;
    }
    
    /**
     * 构建缓存key
     */
    private String buildKey(Long userId, String sessionId, Long postId) {
        String userIdentifier = userId != null ? "user_" + userId : "session_" + sessionId;
        return userIdentifier + "_post_" + postId;
    }
    
    /**
     * 清理过期记录（可选，由定时任务调用）
     */
    public void cleanExpiredRecords() {
        long currentTime = System.currentTimeMillis();
        viewRecords.entrySet().removeIf(entry -> 
            (currentTime - entry.getValue()) > VIEW_WINDOW_MS
        );
    }
    
    /**
     * 获取当前记录数（用于监控）
     */
    public int getRecordCount() {
        return viewRecords.size();
    }
}

