package com.sg.nusiss.gamevaultbackend.controller;

import com.sg.nusiss.gamevaultbackend.dto.OrderDetailDto;
import com.sg.nusiss.gamevaultbackend.dto.OrderGroupSummaryDto;
import com.sg.nusiss.gamevaultbackend.dto.OrderItemDto;
import com.sg.nusiss.gamevaultbackend.entity.OrderItem;
import com.sg.nusiss.gamevaultbackend.repository.OrderItemRepository;
import com.sg.nusiss.gamevaultbackend.repository.PurchasedGameActivationCodeRepository;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class OrdersController {
    private final OrderItemRepository orderItemRepo;
    private final PurchasedGameActivationCodeRepository pacRepo;

    public OrdersController(OrderItemRepository orderItemRepo, PurchasedGameActivationCodeRepository pacRepo) {
        this.orderItemRepo = orderItemRepo; this.pacRepo = pacRepo;
    }

    @GetMapping("/orders")
    public Map<String, Object> list(@AuthenticationPrincipal Jwt jwt) {
        Long uid = ((Number) jwt.getClaims().get("uid")).longValue();
        List<OrderItemDto> items = orderItemRepo.findByUserIdOrderByOrderDateDesc(uid).stream().map(oi -> {
            OrderItemDto d = new OrderItemDto();
            d.orderItemId = oi.getOrderItemId();
            d.orderId = oi.getOrderId();
            d.userId = oi.getUserId();
            d.gameId = oi.getGameId();
            d.unitPrice = oi.getUnitPrice();
            d.discountPrice = oi.getDiscountPrice();
            d.orderDate = oi.getOrderDate();
            d.orderStatus = oi.getOrderStatus();
            return d;
        }).collect(Collectors.toList());
        return Map.of("items", items);
    }

    // Summary list aggregated by order_id
    @GetMapping("/orders/summary")
    public Map<String, Object> summary(@AuthenticationPrincipal Jwt jwt) {
        Long uid = ((Number) jwt.getClaims().get("uid")).longValue();
        var grouped = orderItemRepo.findByUserIdOrderByOrderDateDesc(uid).stream()
                .collect(Collectors.groupingBy(OrderItem::getOrderId, Collectors.toList()));
        List<OrderGroupSummaryDto> list = grouped.entrySet().stream().map(e -> {
            var arr = e.getValue();
            OrderGroupSummaryDto s = new OrderGroupSummaryDto();
            s.orderId = e.getKey();
            s.createdAt = arr.get(0).getOrderDate();
            s.status = arr.get(0).getOrderStatus();
            s.total = arr.stream()
                    .map(x -> x.getDiscountPrice() != null ? x.getDiscountPrice() : x.getUnitPrice())
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
            return s;
        }).sorted((a,b) -> b.createdAt.compareTo(a.createdAt)).collect(Collectors.toList());
        return Map.of("items", list);
    }

    @GetMapping("/orders/{id}")
    public Map<String, Object> detail(@PathVariable("id") Long orderId, @AuthenticationPrincipal Jwt jwt) {
        Long uid = ((Number) jwt.getClaims().get("uid")).longValue();
        List<OrderItem> items = orderItemRepo.findByUserIdOrderByOrderDateDesc(uid).stream()
                .filter(oi -> oi.getOrderId().equals(orderId))
                .collect(Collectors.toList());
        
        if (items.isEmpty()) throw new RuntimeException("Order not found or forbidden");
        
        // Collect all activation codes for items in this order
        List<String> allActivationCodes = items.stream()
                .flatMap(oi -> pacRepo.findByOrderItemId(oi.getOrderItemId()).stream())
                .map(x -> x.getActivationCode())
                .collect(Collectors.toList());
        
        // Return order information (using first order item's basic info)
        OrderItem first = items.get(0);
        return Map.of(
            "orderId", orderId,
            "createdAt", first.getOrderDate(),
            "status", first.getOrderStatus(),
            "total", items.stream()
                    .map(x -> x.getDiscountPrice() != null ? x.getDiscountPrice() : x.getUnitPrice())
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add),
            "items", items.stream().map(oi -> {
                OrderDetailDto d = new OrderDetailDto();
                d.orderItemId = oi.getOrderItemId();
                d.orderId = oi.getOrderId();
                d.userId = oi.getUserId();
                d.gameId = oi.getGameId();
                d.unitPrice = oi.getUnitPrice();
                d.discountPrice = oi.getDiscountPrice();
                d.orderDate = oi.getOrderDate();
                d.orderStatus = oi.getOrderStatus();
                d.activationCodes = pacRepo.findByOrderItemId(oi.getOrderItemId()).stream()
                        .map(x -> x.getActivationCode()).collect(Collectors.toList());
                return d;
            }).collect(Collectors.toList()),
            "activationCodes", allActivationCodes
        );
    }
}


