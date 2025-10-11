package com.sg.nusiss.gamevaultbackend.controller.shopping;

import com.sg.nusiss.gamevaultbackend.dto.shopping.CartDTO;
import com.sg.nusiss.gamevaultbackend.dto.library.OrderDTO;
import com.sg.nusiss.gamevaultbackend.entity.ENUM.PaymentMethod;
import com.sg.nusiss.gamevaultbackend.service.shopping.CartService;
import com.sg.nusiss.gamevaultbackend.service.discount.DiscountFactory;
import com.sg.nusiss.gamevaultbackend.service.discount.IDiscountStrategy;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/**
 * ✅ 统一采用 Spring Security 的 Jwt 注入机制
 * 无需再使用 AuthClient 或手动解析 token
 */
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    /** 🛒 获取购物车 */
    @GetMapping
    public ResponseEntity<CartDTO> getCart(@AuthenticationPrincipal Jwt jwt) {
        Long userId = ((Number) jwt.getClaims().get("uid")).longValue();
        return ResponseEntity.ok(cartService.getCart(userId));
    }

    /** ➕ 添加商品到购物车 */
    @PostMapping("/items")
    public ResponseEntity<CartDTO> addToCart(@AuthenticationPrincipal Jwt jwt,
                                             @RequestParam(name = "gameId") Long gameId,
                                             @RequestParam(name = "quantity", defaultValue = "1") int quantity) {
        Long userId = ((Number) jwt.getClaims().get("uid")).longValue();
        return ResponseEntity.ok(cartService.addGame(userId, gameId, quantity));
    }

    /** 🔄 更新购物车商品数量 */
    @PutMapping("/items/{gameId}")
    public ResponseEntity<CartDTO> updateQuantity(@AuthenticationPrincipal Jwt jwt,
                                                  @PathVariable Long gameId,
                                                  @RequestParam(name = "quantity") int quantity) {
        Long userId = ((Number) jwt.getClaims().get("uid")).longValue();
        return ResponseEntity.ok(cartService.updateQuantity(userId, gameId, quantity));
    }

    /** ❌ 移除购物车内的商品 */
    @DeleteMapping("/items/{gameId}")
    public ResponseEntity<CartDTO> removeFromCart(@AuthenticationPrincipal Jwt jwt,
                                                  @PathVariable Long gameId) {
        Long userId = ((Number) jwt.getClaims().get("uid")).longValue();
        return ResponseEntity.ok(cartService.removeGame(userId, gameId));
    }

    /** 🧹 清空购物车 */
    @DeleteMapping
    public ResponseEntity<CartDTO> clearCart(@AuthenticationPrincipal Jwt jwt) {
        Long userId = ((Number) jwt.getClaims().get("uid")).longValue();
        return ResponseEntity.ok(cartService.clearCart(userId));
    }

    /** 💰 应用折扣策略 */
    @PostMapping("/discount")
    public ResponseEntity<CartDTO> applyDiscount(@AuthenticationPrincipal Jwt jwt,
                                                 @RequestBody DiscountRequest request) {
        Long userId = ((Number) jwt.getClaims().get("uid")).longValue();
        IDiscountStrategy strategy = DiscountFactory.createDiscount(request.getStrategyType());
        cartService.setDiscountStrategy(strategy);
        cartService.applyDiscounts(userId);
        return ResponseEntity.ok(cartService.getCart(userId));
    }

    /** 💳 结账 */
    @PostMapping("/checkout")
    public ResponseEntity<OrderDTO> checkout(@AuthenticationPrincipal Jwt jwt,
                                             @RequestParam PaymentMethod method) {
        Long userId = ((Number) jwt.getClaims().get("uid")).longValue();
        return ResponseEntity.ok(cartService.checkout(userId, method));
    }

    @Data
    private static class DiscountRequest {
        private String strategyType;
    }
}
