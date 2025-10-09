package com.sg.nusiss.gamevaultbackend.util.forum;

import org.hashids.Hashids;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.Arrays;
import java.util.Objects;

/**
 * HashID工具类 - 用于加密/解密ID
 * 将数字ID转换为短字符串，避免在URL中暴露真实ID
 */
@Component
public class HashIdUtil {

    private final Hashids postHashids;
    private final Hashids userHashids;
    private final Hashids forumHashids;

    public HashIdUtil(@Value("${hashid.salt:forum-backend-2024}") String salt) {
        // 使用不同的盐值和最小长度来区分不同类型的ID
        this.postHashids = new Hashids(salt + "-post", 8);
        this.userHashids = new Hashids(salt + "-user", 6);
        this.forumHashids = new Hashids(salt + "-forum", 6);
    }

    // ==================== Post ID 编码/解码 ====================

    /**
     * 编码帖子ID
     * 例如: 123 -> "x9Kje2Qp"
     */
    public String encodePostId(Long id) {
        if (id == null) return null;
        return postHashids.encode(id);
    }

    /**
     * 解码帖子ID
     * 例如: "x9Kje2Qp" -> 123
     */
    public Long decodePostId(String hash) {
        if (hash == null || hash.isEmpty()) return null;
        try {
            long[] decode = postHashids.decode(hash);
            return decode.length > 0 ? decode[0] : null;
        } catch (Exception e) {
            return null;
        }
    }

    // ==================== User ID 编码/解码 ====================

    public String encodeUserId(Long id) {
        if (id == null) return null;
        return userHashids.encode(id);
    }

    public Long decodeUserId(String hash) {
        if (hash == null || hash.isEmpty()) return null;
        try {
            long[] decode = userHashids.decode(hash);
            return decode.length > 0 ? decode[0] : null;
        } catch (Exception e) {
            return null;
        }
    }

    // ==================== Forum ID 编码/解码 ====================

    public String encodeForumId(Long id) {
        if (id == null) return null;
        return forumHashids.encode(id);
    }

    public Long decodeForumId(String hash) {
        if (hash == null || hash.isEmpty()) return null;
        try {
            long[] decode = forumHashids.decode(hash);
            return decode.length > 0 ? decode[0] : null;
        } catch (Exception e) {
            return null;
        }
    }

    // ==================== 批量编码 ====================

    /**
     * 批量编码多个ID
     * 使用Java 8 Stream API进行类型转换
     */
    public String encodeMultipleIds(Long... ids) {
        if (ids == null || ids.length == 0) return null;

        // 检查是否有null值
        if (Arrays.stream(ids).anyMatch(Objects::isNull)) {
            return null;
        }

        // 转换为原始类型数组
        long[] primitiveIds = Arrays.stream(ids)
                .mapToLong(Long::longValue)
                .toArray();

        return postHashids.encode(primitiveIds);
    }

    /**
     * 批量解码多个ID
     */
    public Long[] decodeMultipleIds(String hash) {
        if (hash == null || hash.isEmpty()) return new Long[0];
        try {
            long[] primitiveIds = postHashids.decode(hash);
            // 转换为包装类型数组
            return Arrays.stream(primitiveIds)
                    .boxed()
                    .toArray(Long[]::new);
        } catch (Exception e) {
            return new Long[0];
        }
    }

    /**
     * 编码单个ID的辅助方法（兼容原始类型）
     */
    public String encode(long id) {
        return postHashids.encode(id);
    }

    /**
     * 解码的辅助方法（返回原始类型）
     */
    public long decode(String hash) {
        if (hash == null || hash.isEmpty()) return -1;
        try {
            long[] decode = postHashids.decode(hash);
            return decode.length > 0 ? decode[0] : -1;
        } catch (Exception e) {
            return -1;
        }
    }
}

