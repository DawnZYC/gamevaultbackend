package com.sg.nusiss.gamevaultbackend.repository.conversation;


import com.sg.nusiss.gamevaultbackend.entity.conversation.Member;
import com.sg.nusiss.gamevaultbackend.entity.conversation.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @ClassName MemberRepository
 * @Author HUANG ZHENJIA
 * @Date 2025/9/30
 * @Description
 */

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {
    List<Member> findByConversation(Conversation conversation);

    /**
     * 查询某个群聊的所有成员
     */

    List<Member> findByConversationId(Long conversationId);

    /**
     * 查询某个用户加入的所有群聊成员记录
     */
    List<Member> findByUserId(Long userId);

    /**
     * 检查用户是否在某个群聊中
     */
    boolean existsByConversationIdAndUserId(Long conversationId, Long userId);

    /**
     * 删除某个群聊的所有成员（解散群聊时用）
     */
    @Transactional
    void deleteByConversationId(Long conversationId);

    /**
     * 查询用户的活跃成员记录
     */
    List<Member> findByUserIdAndIsActive(Long userId, Boolean isActive);

    /**
     * 查询某个群聊的活跃成员
     */
    List<Member> findByConversationIdAndIsActive(Long conversationId, Boolean isActive);

    /**
     * 检查用户是否在某个群聊中（活跃状态）
     */
    boolean existsByConversationIdAndUserIdAndIsActive(
            Long conversationId, Long userId, Boolean isActive
    );
}
