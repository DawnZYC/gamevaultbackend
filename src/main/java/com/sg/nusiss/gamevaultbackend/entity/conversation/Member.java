package com.sg.nusiss.gamevaultbackend.entity.conversation;

import com.sg.nusiss.gamevaultbackend.entity.auth.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * @ClassName Member
 * @Author HUANG ZHENJIA
 * @Date 2025/9/30
 * @Description 群聊成员实体类
 */
@Entity
@Table(name = "members")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 群 ID
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    // 用户 ID
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    private User user;

    // 角色（普通成员 / 群主 / 管理员）
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String role = "member";

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    // 新增：是否活跃（逻辑删除标记）
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    // 新增：退出时间
    @Column(name = "left_at")
    private LocalDateTime leftAt;

    // 新增：退出原因
    // 可能的值：
    // - "群聊已解散" (群主解散群聊)
    // - "主动退出" (用户自己退群)
    // - "被移除" (被群主或管理员踢出)
    @Column(name = "leave_reason", length = 100)
    private String leaveReason;

    @PrePersist
    protected void onJoin() {
        if (this.joinedAt == null) {
            this.joinedAt = LocalDateTime.now();
        }
    }
}