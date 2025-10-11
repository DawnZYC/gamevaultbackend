package com.sg.nusiss.gamevaultbackend.repository.friend;

import com.sg.nusiss.gamevaultbackend.entity.friend.Friendship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

/**
 * @ClassName FriendshipRepository
 * @Author HUANG ZHENJIA
 * @Date 2025/10/4
 * @Description
 */
public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    // 查询用户的所有活跃好友
    List<Friendship> findByUserIdAndIsActive(Long userId, Boolean isActive);

    // 检查是否已是好友
    Optional<Friendship> findByUserIdAndFriendIdAndIsActive(Long userId, Long friendId, Boolean isActive);

    // 查询双向好友关系
    @Query("SELECT f FROM Friendship f WHERE " +
            "f.userId = ?1 AND f.friendId = ?2 AND f.isActive = true")
    Optional<Friendship> findActiveFriendship(Long userId, Long friendId);
}
