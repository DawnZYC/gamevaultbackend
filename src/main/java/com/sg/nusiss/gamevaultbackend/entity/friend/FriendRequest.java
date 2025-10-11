package com.sg.nusiss.gamevaultbackend.entity.friend;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * @ClassName FriendRequest
 * @Author HUANG ZHENJIA
 * @Date 2025/10/4
 * @Description
 */

@Data
@Entity
@Table(name = "friend_requests")
public class FriendRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "from_user_id", nullable = false)
    private Long fromUserId;

    @Column(name = "to_user_id", nullable = false)
    private Long toUserId;

    @Column(length = 200)
    private String message;

    @Column(length = 20, nullable = false)
    private String status = "pending"; // pending, accepted, rejected, cancelled

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "handled_at")
    private LocalDateTime handledAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
