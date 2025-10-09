package com.sg.nusiss.gamevaultbackend.service.forum;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.sg.nusiss.gamevaultbackend.entity.forum.ForumUser;
import com.sg.nusiss.gamevaultbackend.mapper.forum.ForumUserMapper;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户业务服务类
 * 处理用户相关的业务逻辑
 * 注意：认证功能由其他模块负责，这里主要处理用户信息管理
 */
@Service
public class ForumUserService {

    @Autowired
    private ForumUserMapper userMapper;

    /**
     * 根据ID获取用户信息
     */
    public ForumUser getUserById(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }

        ForumUser user = userMapper.findById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        return user;
    }

    /**
     * 根据用户名获取用户信息
     */
    public ForumUser getUserByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("用户名不能为空");
        }

        ForumUser user = userMapper.findByUsername(username.trim());
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        return user;
    }

    /**
     * 创建用户（用于外部认证系统集成）
     * 当外部认证系统验证用户后，调用此方法创建用户记录
     */
    public ForumUser createUser(Long userId, String username, String nickname) {
        // 参数验证
        if (userId == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("用户名不能为空");
        }

        // 检查用户是否已存在
        ForumUser existingUser = userMapper.findById(userId);
        if (existingUser != null) {
            return existingUser; // 用户已存在，直接返回
        }

        // 检查用户名是否已被使用
        if (userMapper.existsByUsername(username.trim())) {
            throw new RuntimeException("用户名已被使用");
        }

        // 创建新用户
        ForumUser user = new ForumUser(userId, username.trim());
        if (nickname != null && !nickname.trim().isEmpty()) {
            user.setNickname(nickname.trim());
        }

        int result = userMapper.insert(user);
        if (result > 0) {
            return user;
        } else {
            throw new RuntimeException("创建用户失败");
        }
    }

    /**
     * 更新用户资料
     */
    public ForumUser updateUserProfile(Long userId, String nickname, String bio, String avatarUrl) {
        if (userId == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }

        ForumUser user = userMapper.findById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 更新用户信息
        boolean hasUpdates = false;

        if (nickname != null && !nickname.trim().isEmpty()) {
            user.setNickname(nickname.trim());
            hasUpdates = true;
        }

        if (bio != null) {
            user.setBio(bio.trim());
            hasUpdates = true;
        }

        if (avatarUrl != null) {
            user.setAvatarUrl(avatarUrl.trim());
            hasUpdates = true;
        }

        if (hasUpdates) {
            user.setUpdatedDate(LocalDateTime.now());
            int result = userMapper.update(user);
            if (result <= 0) {
                throw new RuntimeException("更新用户信息失败");
            }
        }

        return user;
    }

    /**
     * 检查用户是否存在
     */
    public boolean userExists(Long userId) {
        if (userId == null) {
            return false;
        }

        try {
            ForumUser user = userMapper.findById(userId);
            return user != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 检查用户名是否存在
     */
    public boolean usernameExists(String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }

        return userMapper.existsByUsername(username.trim());
    }

    /**
     * 获取活跃用户列表
     */
    public List<ForumUser> getActiveUsers(int page, int size) {
        if (page < 0) page = 0;
        if (size <= 0 || size > 100) size = 20;

        int offset = page * size;
        return userMapper.findActiveUsers(offset, size);
    }

    /**
     * 统计活跃用户数量
     */
    public int getActiveUserCount() {
        return userMapper.countActiveUsers();
    }

    /**
     * 批量获取用户信息
     */
    public List<ForumUser> getUsersByIds(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            throw new IllegalArgumentException("用户ID列表不能为空");
        }

        return userMapper.findByIds(userIds);
    }

    /**
     * 验证用户状态是否活跃
     */
    public boolean isUserActive(Long userId) {
        if (userId == null) {
            return false;
        }

        try {
            ForumUser user = userMapper.findById(userId);
            return user != null && user.isActive();
        } catch (Exception e) {
            return false;
        }
    }
}

