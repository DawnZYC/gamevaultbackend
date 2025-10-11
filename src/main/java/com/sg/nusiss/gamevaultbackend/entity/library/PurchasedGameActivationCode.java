package com.sg.nusiss.gamevaultbackend.entity.library;

import jakarta.persistence.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "purchased_game_activation_code", // 统一命名为复数形态
        indexes = {
                @Index(name = "idx_user_id", columnList = "user_id"),
                @Index(name = "idx_order_item_id", columnList = "order_item_id", unique = true)
        }
)
public class PurchasedGameActivationCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "activation_id")
    private Long activationId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "order_item_id", nullable = false, unique = true)
    private Long orderItemId;

    @Column(name = "game_id", nullable = false)
    private Long gameId;

    @Column(name = "activation_code", nullable = false, unique = true, length = 255)
    private String activationCode;

    /** 工厂方法，便于快速构造 */
    public static PurchasedGameActivationCode of(Long userId, Long orderItemId, Long gameId, String code) {
        return PurchasedGameActivationCode.builder()
                .userId(userId)
                .orderItemId(orderItemId)
                .gameId(gameId)
                .activationCode(code)
                .build();
    }
}
