package com.sg.nusiss.gamevaultbackend.entity.friend;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * @ClassName Friendship
 * @Author HUANG ZHENJIA
 * @Date 2025/10/4
 * @Description
 */

@Data
@Entity
@Table(name = "friendships")
public class Friendship {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "friend_id", nullable = false)
    private Long friendId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(length = 50)
    private String remark;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
