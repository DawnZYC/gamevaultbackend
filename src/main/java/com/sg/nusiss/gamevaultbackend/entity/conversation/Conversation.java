package com.sg.nusiss.gamevaultbackend.entity.conversation;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * @ClassName Conversation
 * @Author HUANG ZHENJIA
 * @Date 2025/9/30
 * @Description 群聊会话实体类
 */
@Entity
@Table(name = "conversations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // UUID，保证跨系统唯一
    @Column(nullable = false, unique = true, length = 150)
    private String uuid;

    @Column(nullable = false, length = 255)
    private String title;

    // 群主 ID
    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    // active | dissolved
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "active";

    // 消息序号（可做消息追踪/断点续传）
    @Column(name = "next_seq", nullable = false)
    @Builder.Default
    private Long nextSeq = 1L;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "dissolved_at")
    private LocalDateTime dissolvedAt;

    // 新增：解散原因
    @Column(name = "dissolved_reason", length = 500)
    private String dissolvedReason;

    // 新增：解散操作人ID
    @Column(name = "dissolved_by")
    private Long dissolvedBy;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.uuid == null || this.uuid.isEmpty()) {
            this.uuid = UUID.randomUUID().toString();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}