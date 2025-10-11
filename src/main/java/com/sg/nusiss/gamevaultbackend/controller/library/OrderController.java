package com.sg.nusiss.gamevaultbackend.controller.library;

import com.sg.nusiss.gamevaultbackend.dto.library.OrderDTO;
import com.sg.nusiss.gamevaultbackend.dto.library.OrderDetailDTO;
import com.sg.nusiss.gamevaultbackend.dto.library.OrderGroupSummaryDTO;
import com.sg.nusiss.gamevaultbackend.entity.ENUM.PaymentMethod;
import com.sg.nusiss.gamevaultbackend.entity.shopping.OrderItem;
import com.sg.nusiss.gamevaultbackend.repository.library.OrderItemRepository;
import com.sg.nusiss.gamevaultbackend.repository.library.PurchasedGameActivationCodeRepository;
import com.sg.nusiss.gamevaultbackend.service.shopping.CartService;
import com.sg.nusiss.gamevaultbackend.service.shopping.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * ✅ Order Controller
 * 统一使用 Spring Security 的 Jwt 注入方式，与队友风格保持一致
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final CartService cartService;
    private final OrderService orderService;
    private final OrderItemRepository orderItemRepo;
    private final PurchasedGameActivationCodeRepository pacRepo;

    /** 🧾 1) 创建订单（结账） */
    @PostMapping("/checkout")
    public ResponseEntity<OrderDTO> checkout(@AuthenticationPrincipal Jwt jwt,
                                             @RequestParam PaymentMethod method) {

        Long userId = ((Number) jwt.getClaims().get("uid")).longValue();
        var cart = cartService.getCartEntity(userId);
        var dto  = orderService.createPendingOrder(cart, method);
        cartService.markCheckedOut(userId, method);

        return ResponseEntity.ok(dto);
    }

    /** 📦 2) 查询当前用户所有订单 */
    @GetMapping
    public ResponseEntity<List<OrderDTO>> getOrders(@AuthenticationPrincipal Jwt jwt) {
        Long userId = ((Number) jwt.getClaims().get("uid")).longValue();
        return ResponseEntity.ok(orderService.findByUserId(userId));
    }

    /** 🔍 3) 查询单个订单详情（含激活码） */
    @GetMapping("/{orderId}")
    public ResponseEntity<Map<String, Object>> getOrderDetail(@PathVariable Long orderId,
                                                              @AuthenticationPrincipal Jwt jwt) {
        Long uid = ((Number) jwt.getClaims().get("uid")).longValue();

        List<OrderItem> items = orderItemRepo.findByUserIdOrderByOrderDateDesc(uid).stream()
                .filter(oi -> oi.getOrderId().equals(orderId))
                .collect(Collectors.toList());
        if (items.isEmpty()) return ResponseEntity.notFound().build();

        List<String> allActivationCodes = items.stream()
                .flatMap(oi -> pacRepo.findByOrderItemId(oi.getOrderItemId()).stream())
                .map(x -> x.getActivationCode())
                .collect(Collectors.toList());

        OrderItem first = items.get(0);
        Map<String, Object> result = new HashMap<>();
        result.put("orderId", orderId);
        result.put("createdAt", first.getOrderDate());
        result.put("status", first.getOrderStatus());
        result.put("total", items.stream()
                .map(x -> x.getDiscountPrice() != null ? x.getDiscountPrice() : x.getUnitPrice())
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add));
        result.put("items", items.stream().map(oi -> {
            OrderDetailDTO d = new OrderDetailDTO();
            d.orderItemId = oi.getOrderItemId();
            d.orderId = oi.getOrderId();
            d.userId = oi.getUserId();
            d.gameId = oi.getGameId();
            d.unitPrice = oi.getUnitPrice();
            d.discountPrice = oi.getDiscountPrice();
            d.orderDate = oi.getOrderDate();
            d.orderStatus = String.valueOf(oi.getOrderStatus());
            d.activationCodes = pacRepo.findByOrderItemId(oi.getOrderItemId()).stream()
                    .map(x -> x.getActivationCode()).collect(Collectors.toList());
            return d;
        }).collect(Collectors.toList()));
        result.put("activationCodes", allActivationCodes);

        return ResponseEntity.ok(result);
    }

    /** 📊 4) 查询订单摘要（分组汇总） */
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary(@AuthenticationPrincipal Jwt jwt) {
        Long uid = ((Number) jwt.getClaims().get("uid")).longValue();
        var grouped = orderItemRepo.findByUserIdOrderByOrderDateDesc(uid).stream()
                .collect(Collectors.groupingBy(OrderItem::getOrderId, Collectors.toList()));

        List<OrderGroupSummaryDTO> list = grouped.entrySet().stream().map(e -> {
                    var arr = e.getValue();
                    OrderGroupSummaryDTO s = new OrderGroupSummaryDTO();
                    s.orderId = e.getKey();
                    s.createdAt = arr.get(0).getOrderDate();
                    s.status = String.valueOf(arr.get(0).getOrderStatus());
                    s.total = arr.stream()
                            .map(x -> x.getDiscountPrice() != null ? x.getDiscountPrice() : x.getUnitPrice())
                            .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
                    return s;
                }).sorted((a, b) -> b.createdAt.compareTo(a.createdAt))
                .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of("items", list));
    }

    /** 💳 5) 支付成功 */
    @PostMapping("/{orderId}/pay")
    public ResponseEntity<OrderDTO> pay(@PathVariable Long orderId,
                                        @AuthenticationPrincipal Jwt jwt) {
        Long userId = ((Number) jwt.getClaims().get("uid")).longValue();
        return ResponseEntity.ok(orderService.captureAndFulfill(orderId, userId));
    }

    /** ❌ 6) 支付失败 */
    @PostMapping("/{orderId}/fail")
    public ResponseEntity<Void> fail(@PathVariable Long orderId,
                                     @AuthenticationPrincipal Jwt jwt) {
        Long userId = ((Number) jwt.getClaims().get("uid")).longValue();
        orderService.markFailed(orderId, userId);
        return ResponseEntity.ok().build();
    }
}
