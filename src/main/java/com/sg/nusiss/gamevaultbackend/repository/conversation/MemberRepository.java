package com.sg.nusiss.gamevaultbackend.repository.conversation;


import com.sg.nusiss.gamevaultbackend.entity.conversation.Member;
import com.sg.nusiss.gamevaultbackend.entity.conversation.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

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
    @Query("SELECT m FROM Member m WHERE m.user.userId = ?1")
    List<Member> findByUserId(Long userId);

    /**
     * 检查用户是否在某个群聊中
     */
    @Query("SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END FROM Member m WHERE m.conversation.id = ?1 AND m.user.userId = ?2")
    boolean existsByConversationIdAndUserId(Long conversationId, Long userId);

    /**
     * 删除某个群聊的所有成员（解散群聊时用）
     */
    @Transactional
    void deleteByConversationId(Long conversationId);

    /**
     * 查询某个群聊的活跃成员
     */
    List<Member> findByConversationIdAndIsActive(Long conversationId, Boolean isActive);

    /** 检查用户是否在某个群聊中（活跃状态） */
    @Query("SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END FROM Member m WHERE m.conversation.id = ?1 AND m.user.userId = ?2 AND m.isActive = ?3")
    boolean existsByConversationIdAndUserIdAndIsActive(Long conversationId, Long userId, Boolean isActive);

    /** 查询特定用户在特定群聊中的成员记录（活跃状态） */
    @Query("SELECT m FROM Member m WHERE m.conversation.id = ?1 AND m.user.userId = ?2 AND m.isActive = ?3")
    Optional<Member> findByConversationIdAndUserIdAndIsActive(Long conversationId, Long userId, Boolean isActive);

    /** 查询用户的活跃成员记录 */
    @Query("SELECT m FROM Member m WHERE m.user.userId = ?1 AND m.isActive = ?2")
    List<Member> findByUserIdAndIsActive(Long userId, Boolean isActive);

    /**
     * 查询某个群聊的活跃成员（带JOIN FETCH避免懒加载）
     */
    @Query("SELECT m FROM Member m JOIN FETCH m.user WHERE m.conversation.id = ?1 AND m.isActive = ?2")
    List<Member> findByConversationIdAndIsActiveWithUser(Long conversationId, Boolean isActive);
}
