package com.sg.nusiss.gamevaultbackend.mapper.forum;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.sg.nusiss.gamevaultbackend.entity.forum.ForumUser;
import java.util.List;

/**
 * 用户 Mapper 接口
 * 处理用户的基础 CRUD 操作
 */
@Mapper
public interface ForumUserMapper {

    // ==================== 基础 CRUD ====================

    /**
     * 根据ID查询用户
     */
    ForumUser findById(@Param("userId") Long userId);

    /**
     * 根据用户名查询用户
     */
    ForumUser findByUsername(@Param("username") String username);

    /**
     * 插入新用户
     */
    int insert(ForumUser user);

    /**
     * 更新用户信息
     */
    int update(ForumUser user);

    // ==================== 查询操作 ====================

    /**
     * 检查用户名是否存在
     */
    boolean existsByUsername(@Param("username") String username);

    /**
     * 查询活跃用户列表
     */
    List<ForumUser> findActiveUsers(@Param("offset") int offset, @Param("limit") int limit);

    /**
     * 统计活跃用户数量
     */
    int countActiveUsers();

    /**
     * 批量查询用户（根据ID列表）
     */
    List<ForumUser> findByIds(@Param("userIds") List<Long> userIds);
}

