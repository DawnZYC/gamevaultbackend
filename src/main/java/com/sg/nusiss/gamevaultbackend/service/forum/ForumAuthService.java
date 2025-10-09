package com.sg.nusiss.gamevaultbackend.service.forum;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.sg.nusiss.gamevaultbackend.entity.forum.ForumAccount;
import com.sg.nusiss.gamevaultbackend.entity.forum.ForumUser;
import com.sg.nusiss.gamevaultbackend.mapper.forum.ForumAccountMapper;
import com.sg.nusiss.gamevaultbackend.mapper.forum.ForumUserMapper;

import java.time.LocalDateTime;

/**
 * 极简认证服务
 * 处理登录和注册
 */
@Service
public class ForumAuthService {
    private static final Logger logger = LoggerFactory.getLogger(ForumAuthService.class);

    @Autowired
    private ForumAccountMapper accountMapper;

    @Autowired
    private ForumUserMapper userMapper;

    /**
     * 注册新用户
     */
    @Transactional
    public ForumAccount register(String username, String password) {
        // 检查用户名是否已存在
        if (accountMapper.existsByUsername(username)) {
            throw new IllegalArgumentException("用户名已存在");
        }

        // 创建User记录
        ForumUser user = new ForumUser();
        user.setUsername(username);
        user.setNickname(username);
        user.setStatus("active");
        user.setCreatedDate(LocalDateTime.now());
        user.setUpdatedDate(LocalDateTime.now());
        userMapper.insert(user);

        // 创建Account记录
        ForumAccount account = new ForumAccount();
        account.setUsername(username);
        account.setPassword(password);
        account.setUserId(user.getUserId());
        account.setCreatedDate(LocalDateTime.now());
        accountMapper.insert(account);

        logger.info("注册成功: username={}, userId={}", username, user.getUserId());
        return account;
    }

    /**
     * 用户登录
     */
    public ForumAccount login(String username, String password) {
        // 查找账户
        ForumAccount account = accountMapper.findByUsername(username);

        if (account == null) {
            return null;
        }

        // 验证密码
        if (!password.equals(account.getPassword())) {
            return null;
        }

        logger.info("登录成功: username={}", username);
        return account;
    }

    /**
     * 根据用户ID获取用户信息
     */
    public ForumUser getUserById(Long userId) {
        return userMapper.findById(userId);
    }
}

