package com.sg.nusiss.gamevaultbackend.repository.friend;

import com.sg.nusiss.gamevaultbackend.entity.friend.FriendRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

/**
 * @ClassName FriendRequestRepository
 * @Author HUANG ZHENJIA
 * @Date 2025/10/4
 * @Description
 */
public interface FriendRequestRepository extends JpaRepository<FriendRequest, Long> {

    // 查询接收到的好友请求
    List<FriendRequest> findByToUserIdAndStatus(Long toUserId, String status);

    // 查询发送的好友请求
    List<FriendRequest> findByFromUserIdAndStatus(Long fromUserId, String status);

    // 检查是否已有请求（任意方向）
    @Query("SELECT fr FROM FriendRequest fr WHERE " +
            "((fr.fromUserId = ?1 AND fr.toUserId = ?2) OR " +
            "(fr.fromUserId = ?2 AND fr.toUserId = ?1)) AND " +
            "fr.status = ?3")
    Optional<FriendRequest> findExistingRequest(Long userId1, Long userId2, String status);

    // 查找特定的请求
    Optional<FriendRequest> findByFromUserIdAndToUserId(Long fromUserId, Long toUserId);
}
