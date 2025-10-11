package com.sg.nusiss.gamevaultbackend.entity.shopping;

import com.sg.nusiss.gamevaultbackend.entity.ENUM.CartStatus;
import com.sg.nusiss.gamevaultbackend.entity.ENUM.PaymentMethod;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "carts")
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long cartId;

    @Column(nullable = false)
    private Long userId;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<CartItem> cartItems = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    private CartStatus status = CartStatus.ACTIVE;

    private LocalDateTime createdDate = LocalDateTime.now();
    private LocalDateTime lastModifiedDate = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    private BigDecimal discountAmount = BigDecimal.ZERO;
    private BigDecimal finalAmount = BigDecimal.ZERO;

    // --- 业务方法 ---
    public void addGame(CartItem item) {
        cartItems.add(item);
        item.setCart(this);
        updateLastModified();
    }

    public void removeGame(Long gameId) {
        cartItems.removeIf(item -> item.getGameId().equals(gameId));
        updateLastModified();
    }

    public void clear() {
        cartItems.clear();
        discountAmount = BigDecimal.ZERO;
        finalAmount = BigDecimal.ZERO;
        updateLastModified();
    }

    public boolean isEmpty() {
        return cartItems.isEmpty();
    }

    public BigDecimal calculateTotalAmount() {
        return cartItems.stream()
                .map(CartItem::calculateSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal calculateFinalAmount() {
        finalAmount = calculateTotalAmount().subtract(discountAmount);
        return finalAmount;
    }

    private void updateLastModified() {
        this.lastModifiedDate = LocalDateTime.now();
    }

    public Cart() {
        // JPA 默认构造函数
    }

    public Cart(Long userId) {
        this.userId = userId;
        this.status = CartStatus.ACTIVE;
        this.createdDate = LocalDateTime.now();
        this.lastModifiedDate = LocalDateTime.now();
    }

    public Cart(Long userId, PaymentMethod paymentMethod) {
        this(userId);
        this.paymentMethod = paymentMethod;
    }
}
