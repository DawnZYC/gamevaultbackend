package com.sg.nusiss.gamevaultbackend.repository.conversation;

import com.sg.nusiss.gamevaultbackend.entity.conversation.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * @ClassName ConversationRepository
 * @Author HUANG ZHENJIA
 * @Date 2025/9/30
 * @Description
 */
@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    Conversation findByUuid(String uuid);
}
