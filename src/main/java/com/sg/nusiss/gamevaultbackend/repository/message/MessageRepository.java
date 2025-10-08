package com.sg.nusiss.gamevaultbackend.repository.message;

import com.sg.nusiss.gamevaultbackend.entity.message.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * @ClassName MessageRepository
 * @Author HUANG ZHENJIA
 * @Date 2025/9/30
 * @Description
 */

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    /**
     * 查询群聊的最新消息（分页）
     */
    @Query("SELECT m FROM Message m WHERE m.conversationId = ?1 AND m.isDeleted = false ORDER BY m.createdAt DESC")
    Page<Message> findByConversationId(Long conversationId, Pageable pageable);

    /**
     * 查询群聊的所有消息（不分页，用于导出）
     */
    @Query("SELECT m FROM Message m WHERE m.conversationId = ?1 AND m.isDeleted = false ORDER BY m.createdAt ASC")
    List<Message> findAllByConversationId(Long conversationId);

    /**
     * 查询某条消息之前的历史消息（向上翻页）
     */
    @Query("SELECT m FROM Message m WHERE m.conversationId = ?1 AND m.id < ?2 AND m.isDeleted = false ORDER BY m.createdAt DESC")
    Page<Message> findHistoryBeforeMessage(Long conversationId, Long messageId, Pageable pageable);

    // 私聊消息查询（双向）
    @Query("SELECT m FROM Message m WHERE m.chatType = 'private' AND " +
            "((m.senderId = ?1 AND m.receiverId = ?2) OR (m.senderId = ?2 AND m.receiverId = ?1)) " +
            "AND m.isDeleted = false ORDER BY m.createdAt DESC")
    Page<Message> findPrivateMessages(Long userId1, Long userId2, Pageable pageable);

    // 获取私聊的最后一条消息
    @Query("SELECT m FROM Message m WHERE m.chatType = 'private' AND " +
            "((m.senderId = ?1 AND m.receiverId = ?2) OR (m.senderId = ?2 AND m.receiverId = ?1)) " +
            "AND m.isDeleted = false ORDER BY m.createdAt DESC LIMIT 1")
    Message findLastPrivateMessage(Long userId1, Long userId2);
}
