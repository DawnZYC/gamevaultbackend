package com.sg.nusiss.gamevaultbackend.mapper.forum;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.sg.nusiss.gamevaultbackend.entity.forum.ForumAccount;

/**
 * 账户 Mapper 接口
 * 处理认证相关的数据库操作
 */
@Mapper
public interface ForumAccountMapper {

    /**
     * 根据用户名查找账户
     */
    ForumAccount findByUsername(@Param("username") String username);

    /**
     * 插入新账户
     */
    int insert(ForumAccount account);

    /**
     * 检查用户名是否存在
     */
    boolean existsByUsername(@Param("username") String username);

    /**
     * 根据账户ID查找
     */
    ForumAccount findById(@Param("accountId") Long accountId);

    /**
     * 根据用户ID查找账户
     */
    ForumAccount findByUserId(@Param("userId") Long userId);
}

