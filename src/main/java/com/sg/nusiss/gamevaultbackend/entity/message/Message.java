package com.sg.nusiss.gamevaultbackend.entity.message;

import com.sg.nusiss.gamevaultbackend.entity.auth.User;
import com.sg.nusiss.gamevaultbackend.entity.conversation.Conversation;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @ClassName Message
 * @Author HUANG ZHENJIA
 * @Date 2025/9/30
 * @Description 聊天消息实体
 */
@Entity
@Table(name = "messages", indexes = {
        @Index(name = "idx_messages_conversation", columnList = "conversation_id"),
        @Index(name = "idx_messages_sender", columnList = "sender_id"),
        @Index(name = "idx_messages_receiver", columnList = "receiver_id"),
        @Index(name = "idx_messages_file_id", columnList = "file_id"),
        @Index(name = "idx_messages_message_type", columnList = "message_type")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "conversation_id")
    private Long conversationId;

    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    @Column(name = "receiver_id")
    private Long receiverId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "chat_type", length = 20)
    @Builder.Default
    private String chatType = "group";

    @Column(name = "message_type", length = 20)
    @Builder.Default
    private String messageType = "text"; // "text" | "file"

    /**
     * 文件ID（关联 chat_file_info 表）
     */
    @Column(name = "file_id", length = 64)
    private String fileId;

    /**
     * 文件名
     */
    @Column(name = "file_name", length = 255)
    private String fileName;

    /**
     * 文件大小（字节）
     */
    @Column(name = "file_size")
    private Long fileSize;

    /**
     * 文件类型：image | video | audio | document
     */
    @Column(name = "file_type", length = 20)
    private String fileType;

    /**
     * 文件扩展名
     */
    @Column(name = "file_ext", length = 20)
    private String fileExt;

    /**
     * 文件访问URL
     */
    @Column(name = "access_url", length = 1000)
    private String accessUrl;

    /**
     * 缩略图URL（用于图片、视频）
     */
    @Column(name = "thumbnail_url", length = 1000)
    private String thumbnailUrl;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "is_deleted")
    @Builder.Default
    private Boolean isDeleted = false;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}