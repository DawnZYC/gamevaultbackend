package com.sg.nusiss.gamevaultbackend.entity.library;

import jakarta.persistence.*;

@Entity
@Table(name = "purchased_game_activation_code")
public class PurchasedGameActivationCode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "activation_id")
    private Long activationId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "order_item_id", nullable = false)
    private Long orderItemId;

    @Column(name = "game_id", nullable = false)
    private Long gameId;

    @Column(name = "activation_code", nullable = false, length = 255)
    private String activationCode;

    public Long getActivationId() { return activationId; }
    public void setActivationId(Long activationId) { this.activationId = activationId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getOrderItemId() { return orderItemId; }
    public void setOrderItemId(Long orderItemId) { this.orderItemId = orderItemId; }
    public Long getGameId() { return gameId; }
    public void setGameId(Long gameId) { this.gameId = gameId; }
    public String getActivationCode() { return activationCode; }
    public void setActivationCode(String activationCode) { this.activationCode = activationCode; }
}




