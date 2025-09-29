package com.sg.nusiss.gamevaultbackend.repository.friend;

import com.sg.nusiss.gamevaultbackend.entity.friend.Friend;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * @ClassName FriendRepository
 * @Author HUANG ZHENJIA
 * @Date 2025/9/29
 * @Description
 */
public interface FriendRepository extends JpaRepository<Friend, Long> {
    /**
     * 查询某个用户和某个好友之间的关系
     */
    Optional<Friend> findByUserIdAndFriendId(Long userId, Long friendId);

    /**
     * 获取某个用户的所有好友（accepted 状态）
     */
    List<Friend> findAllByUserIdAndStatus(Long userId, String status);

    /**
     * 删除一条好友关系
     */
    void deleteByUserIdAndFriendId(Long userId, Long friendId);
}
