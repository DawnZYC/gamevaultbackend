package com.sg.nusiss.gamevaultbackend.repository.auth;

import com.sg.nusiss.gamevaultbackend.entity.auth.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    // 搜索用户（邮箱或用户名）
    @Query("SELECT u FROM User u WHERE " +
            "u.email = ?1 OR u.username = ?1")
    Optional<User> findByEmailOrUsername(String keyword);

    // 模糊搜索用户
    @Query("SELECT u FROM User u WHERE " +
            "u.email LIKE %?1% OR u.username LIKE %?1%")
    List<User> searchUsers(String keyword);
}

