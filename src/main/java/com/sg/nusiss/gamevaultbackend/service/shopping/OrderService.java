package com.sg.nusiss.gamevaultbackend.service.shopping;

import com.sg.nusiss.gamevaultbackend.dto.library.OrderItemDTO;
import com.sg.nusiss.gamevaultbackend.entity.ENUM.OrderStatus;
import com.sg.nusiss.gamevaultbackend.entity.ENUM.PaymentMethod;
import com.sg.nusiss.gamevaultbackend.dto.library.OrderDTO;
import com.sg.nusiss.gamevaultbackend.entity.library.PurchasedGameActivationCode;
import com.sg.nusiss.gamevaultbackend.entity.shopping.*;
import com.sg.nusiss.gamevaultbackend.repository.library.PurchasedGameActivationCodeRepository;
import com.sg.nusiss.gamevaultbackend.repository.shopping.GameRepository;
import com.sg.nusiss.gamevaultbackend.repository.shopping.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final PurchasedGameActivationCodeRepository purchasedRepo;
    private final GameActivationCodeService codeService;
    private final GameRepository gameRepository;

    /**
     * Step1: 创建 Pending 订单（不分配激活码）
     */
    @Transactional
    public OrderDTO createPendingOrder(Cart cart, PaymentMethod method) {
        if (cart.isEmpty()) throw new IllegalStateException("Cart is empty");

        Order order = new Order();
        order.setUserId(cart.getUserId());
        order.setPaymentMethod(method);
        order.setStatus(OrderStatus.PENDING);
        order.setOrderDate(LocalDateTime.now());

        BigDecimal finalAmount = BigDecimal.ZERO;
        for (CartItem ci : cart.getCartItems()) {
            for (int i = 0; i < ci.getQuantity(); i++) {
                OrderItem oi = new OrderItem();
                oi.setOrder(order);
                oi.setUserId(cart.getUserId());
                oi.setOrderDate(order.getOrderDate());
                oi.setOrderStatus(OrderStatus.PENDING);
                oi.setGameId(ci.getGameId());
                oi.setUnitPrice(ci.getPrice());
                order.getOrderItems().add(oi);
                finalAmount = finalAmount.add(ci.getPrice());
            }
        }
        order.setFinalAmount(finalAmount);

        Order saved = orderRepository.saveAndFlush(order);
        return convertToDTO(saved);
    }

    /**
     * Step2: 支付成功 → 标记为 COMPLETED 并为每个订单项分配激活码。
     * 激活码分配逻辑：
     *  1. 从未使用表取一条激活码；
     *  2. 写入 purchased 表；
     *  3. 删除旧库存；
     *  4. 自动补足库存到目标数量（例如30个）。
     */
    @Transactional
    public OrderDTO captureAndFulfill(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        if (!order.getUserId().equals(userId))
            throw new IllegalStateException("Forbidden");

        if (order.getStatus() == OrderStatus.COMPLETED)
            return convertToDTO(order); // 幂等处理

        order.setStatus(OrderStatus.COMPLETED);

        for (OrderItem oi : order.getOrderItems()) {
            PurchasedGameActivationCode purchased =
                    codeService.assignCodeToOrderItem(userId, oi.getOrderItemId(), oi.getGameId());
            oi.setOrderStatus(OrderStatus.COMPLETED);
            // 如果有必要，也可在此处记录分配日志
        }

        return convertToDTO(order);
    }

    /** 支付失败标记 - 订单取消 */
    @Transactional
    public void markFailed(Long orderId, Long userId) {
        Order o = orderRepository.findById(orderId).orElseThrow();
        if (!o.getUserId().equals(userId)) throw new IllegalStateException("Forbidden");
        if (o.getStatus() == OrderStatus.COMPLETED) return; // 已完成的订单不能取消
        o.setStatus(OrderStatus.CANCELLED);
        
        // 同时更新所有订单项的状态
        for (OrderItem oi : o.getOrderItems()) {
            oi.setOrderStatus(OrderStatus.CANCELLED);
        }
    }

    public Optional<OrderDTO> findById(Long orderId) {
        return orderRepository.findById(orderId).map(this::convertToDTO);
    }

    public List<OrderDTO> findByUserId(Long userId) {
        return orderRepository.findByUserIdOrderByOrderIdDesc(userId).stream()
                .map(this::convertToDTO)
                .toList();
    }

    /**
     * DTO 转换：包含激活码与游戏信息。
     */
    private OrderDTO convertToDTO(Order order) {
        OrderDTO dto = new OrderDTO();
        dto.setOrderId(order.getOrderId());
        dto.setUserId(order.getUserId());
        dto.setStatus(order.getStatus().name());
        dto.setPaymentMethod(order.getPaymentMethod().name());
        dto.setFinalAmount(order.getFinalAmount());
        dto.setOrderDate(order.getOrderDate());

        // 批量取游戏信息避免 N+1 查询
        List<Long> gameIds = order.getOrderItems().stream()
                .map(OrderItem::getGameId)
                .toList();

        Map<Long, Game> gameMap = gameRepository.findAllById(gameIds)
                .stream()
                .collect(Collectors.toMap(Game::getGameId, g -> g));

        dto.setOrderItems(order.getOrderItems().stream().map(oi -> {
            OrderItemDTO oid = new OrderItemDTO();
            oid.setOrderItemId(oi.getOrderItemId());
            oid.setOrderId(order.getOrderId());
            oid.setUserId(oi.getUserId());
            oid.setOrderDate(oi.getOrderDate());
            oid.setOrderStatus(oi.getOrderStatus().name());
            oid.setGameId(oi.getGameId());
            oid.setUnitPrice(oi.getUnitPrice());

            purchasedRepo.findByOrderItemId(oi.getOrderItemId())
                    .ifPresent(code -> oid.setActivationCode(code.getActivationCode()));

            Game game = gameMap.get(oi.getGameId());
            if (game != null) {
                oid.setGameTitle(game.getTitle());
                oid.setImageUrl(game.getImageUrl());
            }

            return oid;
        }).collect(Collectors.toList()));

        return dto;
    }
}
