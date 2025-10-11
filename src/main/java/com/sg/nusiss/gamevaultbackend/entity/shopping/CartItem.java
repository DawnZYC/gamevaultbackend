package com.sg.nusiss.gamevaultbackend.entity.shopping;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "cart_items")
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long cartItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @Column(nullable = false)
    private Long gameId;

    private LocalDateTime addedDate = LocalDateTime.now();

    @Column(nullable = false)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer quantity;

    // --- 业务方法 ---
    public BigDecimal calculateSubtotal() {
        return price.multiply(BigDecimal.valueOf(quantity));
    }

    public CartItem() {
        // JPA 默认构造函数
    }

    public CartItem(Long gameId, BigDecimal price, Integer quantity) {
        this.gameId = gameId;
        this.price = price;
        this.quantity = quantity;
        this.addedDate = LocalDateTime.now();
    }

}
