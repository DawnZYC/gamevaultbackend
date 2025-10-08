package com.sg.nusiss.gamevaultbackend.service.message;

import com.sg.nusiss.gamevaultbackend.dto.message.response.MessageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @ClassName MessageCacheService
 * @Author HUANG ZHENJIA
 * @Date 2025/10/5
 * @Description
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MessageCacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    // Redis key 前缀
    private static final String MESSAGE_CACHE_PREFIX = "chat:messages:";
    // 每个群聊缓存最近多少条消息
    private static final int CACHE_SIZE = 100;
    // 缓存过期时间（天）
    private static final long CACHE_EXPIRE_DAYS = 7;

    /**
     * 获取 Redis key
     */
    private String getCacheKey(Long conversationId) {
        return MESSAGE_CACHE_PREFIX + conversationId;
    }

    /**
     * 缓存单条消息
     */
    public void cacheMessage(MessageResponse message) {
        try {
            String key = getCacheKey(message.getConversationId());

            // 添加到列表末尾（最新消息）
            redisTemplate.opsForList().rightPush(key, message);

            // 保持列表长度
            Long size = redisTemplate.opsForList().size(key);
            if (size != null && size > CACHE_SIZE) {
                redisTemplate.opsForList().leftPop(key);
            }

            // 设置过期时间
            redisTemplate.expire(key, CACHE_EXPIRE_DAYS, TimeUnit.DAYS);

            log.debug("消息已缓存 - 群聊ID: {}, 消息ID: {}",
                    message.getConversationId(), message.getId());
        } catch (Exception e) {
            log.error("缓存消息失败", e);
            // 不抛异常，让主流程继续
        }
    }

    /**
     * 获取缓存的最近消息
     */
    public List<MessageResponse> getCachedMessages(Long conversationId, int limit) {
        try {
            String key = getCacheKey(conversationId);

            Long size = redisTemplate.opsForList().size(key);
            if (size == null || size == 0) {
                return new ArrayList<>();
            }

            // 获取最后 limit 条
            long start = Math.max(0, size - limit);
            List<Object> cached = redisTemplate.opsForList().range(key, start, -1);

            if (cached == null || cached.isEmpty()) {
                return new ArrayList<>();
            }

            // 转换类型
            List<MessageResponse> messages = new ArrayList<>();
            for (Object obj : cached) {
                if (obj instanceof MessageResponse) {
                    messages.add((MessageResponse) obj);
                }
            }

            log.debug("从缓存获取消息 - 群聊ID: {}, 数量: {}", conversationId, messages.size());
            return messages;
        } catch (Exception e) {
            log.error("获取缓存消息失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 批量缓存消息（初始化缓存用）
     */
    public void batchCacheMessages(Long conversationId, List<MessageResponse> messages) {
        try {
            if (messages == null || messages.isEmpty()) {
                return;
            }

            String key = getCacheKey(conversationId);

            // 清空旧缓存
            redisTemplate.delete(key);

            // 只缓存最近的消息
            int start = Math.max(0, messages.size() - CACHE_SIZE);
            List<MessageResponse> toCache = messages.subList(start, messages.size());

            // 批量添加
            for (MessageResponse msg : toCache) {
                redisTemplate.opsForList().rightPush(key, msg);
            }

            // 设置过期时间
            redisTemplate.expire(key, CACHE_EXPIRE_DAYS, TimeUnit.DAYS);

            log.info("批量缓存消息 - 群聊ID: {}, 数量: {}", conversationId, toCache.size());
        } catch (Exception e) {
            log.error("批量缓存消息失败", e);
        }
    }

    /**
     * 清除缓存
     */
    public void clearCache(Long conversationId) {
        try {
            String key = getCacheKey(conversationId);
            redisTemplate.delete(key);
            log.info("清除缓存 - 群聊ID: {}", conversationId);
        } catch (Exception e) {
            log.error("清除缓存失败", e);
        }
    }
}
