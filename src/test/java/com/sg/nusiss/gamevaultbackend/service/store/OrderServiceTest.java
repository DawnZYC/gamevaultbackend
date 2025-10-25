package com.sg.nusiss.gamevaultbackend.service.store;

import com.sg.nusiss.gamevaultbackend.dto.library.OrderDTO;
import com.sg.nusiss.gamevaultbackend.dto.library.OrderItemDTO;
import com.sg.nusiss.gamevaultbackend.entity.ENUM.OrderStatus;
import com.sg.nusiss.gamevaultbackend.entity.ENUM.PaymentMethod;
import com.sg.nusiss.gamevaultbackend.entity.library.PurchasedGameActivationCode;
import com.sg.nusiss.gamevaultbackend.entity.shopping.*;
import com.sg.nusiss.gamevaultbackend.repository.library.PurchasedGameActivationCodeRepository;
import com.sg.nusiss.gamevaultbackend.repository.shopping.GameRepository;
import com.sg.nusiss.gamevaultbackend.repository.shopping.OrderRepository;
import com.sg.nusiss.gamevaultbackend.entity.ENUM.CartStatus;
import com.sg.nusiss.gamevaultbackend.service.shopping.GameActivationCodeService;
import com.sg.nusiss.gamevaultbackend.service.shopping.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * @ClassName OrderServiceTest
 * @Author AI Assistant
 * @Date 2025/01/27
 * @Description OrderService单元测试类，覆盖所有方法
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PurchasedGameActivationCodeRepository purchasedRepo;

    @Mock
    private GameActivationCodeService codeService;

    @Mock
    private GameRepository gameRepository;

    @InjectMocks
    private OrderService orderService;

    private Cart testCart;
    private Order testOrder;
    private OrderItem testOrderItem;
    private Game testGame;
    private PurchasedGameActivationCode testActivationCode;

    @BeforeEach
    void setUp() {
        // 设置测试购物车
        testCart = new Cart(1L);
        testCart.setCartId(1L);
        testCart.setStatus(CartStatus.ACTIVE);

        CartItem cartItem = new CartItem(1L, new BigDecimal("99.99"), 2);
        cartItem.setCartItemId(1L);
        cartItem.setCart(testCart);
        testCart.getCartItems().add(cartItem);

        // 设置测试游戏
        testGame = new Game();
        testGame.setGameId(1L);
        testGame.setTitle("测试游戏");
        testGame.setPrice(new BigDecimal("99.99"));
        testGame.setImageUrl("http://example.com/image.jpg");

        // 设置测试订单
        testOrder = new Order();
        testOrder.setOrderId(1L);
        testOrder.setUserId(1L);
        testOrder.setStatus(OrderStatus.PENDING);
        testOrder.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        testOrder.setOrderDate(LocalDateTime.now());
        testOrder.setFinalAmount(new BigDecimal("199.98"));

        // 设置测试订单项
        testOrderItem = new OrderItem();
        testOrderItem.setOrderItemId(1L);
        testOrderItem.setOrder(testOrder);
        testOrderItem.setUserId(1L);
        testOrderItem.setOrderDate(testOrder.getOrderDate());
        testOrderItem.setOrderStatus(OrderStatus.PENDING);
        testOrderItem.setGameId(1L);
        testOrderItem.setUnitPrice(new BigDecimal("99.99"));
        testOrder.getOrderItems().add(testOrderItem);

        // 设置测试激活码
        testActivationCode = new PurchasedGameActivationCode();
        testActivationCode.setActivationId(1L);
        testActivationCode.setUserId(1L);
        testActivationCode.setOrderItemId(1L);
        testActivationCode.setGameId(1L);
        testActivationCode.setActivationCode("TEST-CODE-123");
    }

    // ==================== createPendingOrder 方法测试 ====================

    @Test
    void testCreatePendingOrder_Success() {
        // Given
        PaymentMethod paymentMethod = PaymentMethod.CREDIT_CARD;

        when(orderRepository.saveAndFlush(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setOrderId(1L);
            return order;
        });

        // When
        OrderDTO result = orderService.createPendingOrder(testCart, paymentMethod);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getUserId());
        assertEquals(paymentMethod.name(), result.getPaymentMethod());
        assertEquals("PENDING", result.getStatus());
        assertNotNull(result.getOrderDate());
        assertNotNull(result.getOrderItems());
        assertEquals(2, result.getOrderItems().size()); // 2个商品，每个数量为2

        verify(orderRepository, times(1)).saveAndFlush(any(Order.class));
    }

    @Test
    void testCreatePendingOrder_EmptyCart_ThrowsException() {
        // Given
        Cart emptyCart = new Cart(1L);
        emptyCart.setCartId(1L);
        PaymentMethod paymentMethod = PaymentMethod.CREDIT_CARD;

        // When & Then
        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> orderService.createPendingOrder(emptyCart, paymentMethod));
        assertTrue(exception.getMessage().contains("Cart is empty"));

        verify(orderRepository, never()).saveAndFlush(any(Order.class));
    }

    @Test
    void testCreatePendingOrder_MultipleItems_Success() {
        // Given
        PaymentMethod paymentMethod = PaymentMethod.PAYPAL;

        // 添加第二个商品到购物车
        CartItem item2 = new CartItem(2L, new BigDecimal("49.99"), 1);
        item2.setCartItemId(2L);
        testCart.getCartItems().add(item2);

        when(orderRepository.saveAndFlush(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setOrderId(1L);
            return order;
        });

        // When
        OrderDTO result = orderService.createPendingOrder(testCart, paymentMethod);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getUserId());
        assertEquals(paymentMethod.name(), result.getPaymentMethod());
        assertEquals("PENDING", result.getStatus());
        assertNotNull(result.getOrderItems());
        assertEquals(3, result.getOrderItems().size()); // 2+1个订单项

        verify(orderRepository, times(1)).saveAndFlush(any(Order.class));
    }

    @Test
    void testCreatePendingOrder_VerifyOrderItems() {
        // Given
        PaymentMethod paymentMethod = PaymentMethod.CREDIT_CARD;

        when(orderRepository.saveAndFlush(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setOrderId(1L);
            return order;
        });

        // When
        OrderDTO result = orderService.createPendingOrder(testCart, paymentMethod);

        // Then
        assertNotNull(result);
        assertNotNull(result.getOrderItems());
        assertEquals(2, result.getOrderItems().size());

        // 验证所有订单项都是PENDING状态
        for (OrderItemDTO item : result.getOrderItems()) {
            assertEquals("PENDING", item.getOrderStatus());
            assertEquals(1L, item.getUserId());
            assertEquals(1L, item.getGameId());
            assertEquals(new BigDecimal("99.99"), item.getUnitPrice());
        }

        verify(orderRepository, times(1)).saveAndFlush(any(Order.class));
    }

    // ==================== captureAndFulfill 方法测试 ====================

    @Test
    void testCaptureAndFulfill_Success() {
        // Given
        Long orderId = 1L;
        Long userId = 1L;

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));
        when(codeService.assignCodeToOrderItem(userId, testOrderItem.getOrderItemId(), testOrderItem.getGameId()))
            .thenReturn(testActivationCode);

        // When
        OrderDTO result = orderService.captureAndFulfill(orderId, userId);

        // Then
        assertNotNull(result);
        assertEquals("COMPLETED", result.getStatus());
        assertEquals(1L, result.getUserId());

        verify(orderRepository, times(1)).findById(orderId);
        verify(codeService, times(1)).assignCodeToOrderItem(userId, testOrderItem.getOrderItemId(), testOrderItem.getGameId());
    }

    @Test
    void testCaptureAndFulfill_OrderNotFound_ThrowsException() {
        // Given
        Long orderId = 999L;
        Long userId = 1L;

        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> orderService.captureAndFulfill(orderId, userId));
        assertTrue(exception.getMessage().contains("Order not found"));

        verify(orderRepository, times(1)).findById(orderId);
        verify(codeService, never()).assignCodeToOrderItem(anyLong(), anyLong(), anyLong());
    }

    @Test
    void testCaptureAndFulfill_WrongUser_ThrowsException() {
        // Given
        Long orderId = 1L;
        Long userId = 999L; // 不同的用户

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));

        // When & Then
        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> orderService.captureAndFulfill(orderId, userId));
        assertTrue(exception.getMessage().contains("Forbidden"));

        verify(orderRepository, times(1)).findById(orderId);
        verify(codeService, never()).assignCodeToOrderItem(anyLong(), anyLong(), anyLong());
    }

    @Test
    void testCaptureAndFulfill_AlreadyCompleted_ReturnsSame() {
        // Given
        Long orderId = 1L;
        Long userId = 1L;
        testOrder.setStatus(OrderStatus.COMPLETED);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));
        when(gameRepository.findAllById(anyList())).thenReturn(Arrays.asList(testGame));
        when(purchasedRepo.findByOrderItemId(anyLong())).thenReturn(Optional.of(testActivationCode));

        // When
        OrderDTO result = orderService.captureAndFulfill(orderId, userId);

        // Then
        assertNotNull(result);
        assertEquals("COMPLETED", result.getStatus());

        verify(orderRepository, times(1)).findById(orderId);
        verify(codeService, never()).assignCodeToOrderItem(anyLong(), anyLong(), anyLong());
    }

    @Test
    void testCaptureAndFulfill_MultipleOrderItems_Success() {
        // Given
        Long orderId = 1L;
        Long userId = 1L;

        // 添加第二个订单项
        OrderItem item2 = new OrderItem();
        item2.setOrderItemId(2L);
        item2.setOrder(testOrder);
        item2.setUserId(1L);
        item2.setOrderDate(testOrder.getOrderDate());
        item2.setOrderStatus(OrderStatus.PENDING);
        item2.setGameId(2L);
        item2.setUnitPrice(new BigDecimal("49.99"));
        testOrder.getOrderItems().add(item2);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));
        when(codeService.assignCodeToOrderItem(userId, 1L, 1L)).thenReturn(testActivationCode);
        when(codeService.assignCodeToOrderItem(userId, 2L, 2L)).thenReturn(testActivationCode);

        // When
        OrderDTO result = orderService.captureAndFulfill(orderId, userId);

        // Then
        assertNotNull(result);
        assertEquals("COMPLETED", result.getStatus());

        verify(codeService, times(2)).assignCodeToOrderItem(anyLong(), anyLong(), anyLong());
    }

    // ==================== markFailed 方法测试 ====================

    @Test
    void testMarkFailed_Success() {
        // Given
        Long orderId = 1L;
        Long userId = 1L;

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));

        // When
        orderService.markFailed(orderId, userId);

        // Then
        verify(orderRepository, times(1)).findById(orderId);
        // 验证订单状态被更新为CANCELLED
        assertEquals(OrderStatus.CANCELLED, testOrder.getStatus());
        assertEquals(OrderStatus.CANCELLED, testOrderItem.getOrderStatus());
    }

    @Test
    void testMarkFailed_OrderNotFound_ThrowsException() {
        // Given
        Long orderId = 999L;
        Long userId = 1L;

        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> orderService.markFailed(orderId, userId));

        verify(orderRepository, times(1)).findById(orderId);
    }

    @Test
    void testMarkFailed_WrongUser_ThrowsException() {
        // Given
        Long orderId = 1L;
        Long userId = 999L; // 不同的用户

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));

        // When & Then
        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> orderService.markFailed(orderId, userId));
        assertTrue(exception.getMessage().contains("Forbidden"));

        verify(orderRepository, times(1)).findById(orderId);
    }

    @Test
    void testMarkFailed_AlreadyCompleted_NoChange() {
        // Given
        Long orderId = 1L;
        Long userId = 1L;
        testOrder.setStatus(OrderStatus.COMPLETED);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));

        // When
        orderService.markFailed(orderId, userId);

        // Then
        // 状态应该保持COMPLETED，不应该改变
        assertEquals(OrderStatus.COMPLETED, testOrder.getStatus());
        verify(orderRepository, times(1)).findById(orderId);
    }

    @Test
    void testMarkFailed_MultipleOrderItems_AllCancelled() {
        // Given
        Long orderId = 1L;
        Long userId = 1L;

        // 添加第二个订单项
        OrderItem item2 = new OrderItem();
        item2.setOrderItemId(2L);
        item2.setOrder(testOrder);
        item2.setUserId(1L);
        item2.setOrderDate(testOrder.getOrderDate());
        item2.setOrderStatus(OrderStatus.PENDING);
        item2.setGameId(2L);
        item2.setUnitPrice(new BigDecimal("49.99"));
        testOrder.getOrderItems().add(item2);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));

        // When
        orderService.markFailed(orderId, userId);

        // Then
        assertEquals(OrderStatus.CANCELLED, testOrder.getStatus());
        assertEquals(OrderStatus.CANCELLED, testOrderItem.getOrderStatus());
        assertEquals(OrderStatus.CANCELLED, item2.getOrderStatus());
    }

    // ==================== findById 方法测试 ====================

    @Test
    void testFindById_Success() {
        // Given
        Long orderId = 1L;

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));
        when(gameRepository.findAllById(anyList())).thenReturn(Arrays.asList(testGame));
        when(purchasedRepo.findByOrderItemId(anyLong())).thenReturn(Optional.of(testActivationCode));

        // When
        Optional<OrderDTO> result = orderService.findById(orderId);

        // Then
        assertTrue(result.isPresent());
        assertEquals(orderId, result.get().getOrderId());
        assertEquals(1L, result.get().getUserId());
        assertEquals("PENDING", result.get().getStatus());
        assertNotNull(result.get().getOrderItems());

        verify(orderRepository, times(1)).findById(orderId);
        verify(gameRepository, times(1)).findAllById(anyList());
    }

    @Test
    void testFindById_NotFound() {
        // Given
        Long orderId = 999L;

        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        // When
        Optional<OrderDTO> result = orderService.findById(orderId);

        // Then
        assertFalse(result.isPresent());

        verify(orderRepository, times(1)).findById(orderId);
        verify(gameRepository, never()).findAllById(anyList());
    }

    @Test
    void testFindById_WithActivationCodes() {
        // Given
        Long orderId = 1L;

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));
        when(gameRepository.findAllById(anyList())).thenReturn(Arrays.asList(testGame));
        when(purchasedRepo.findByOrderItemId(anyLong())).thenReturn(Optional.of(testActivationCode));

        // When
        Optional<OrderDTO> result = orderService.findById(orderId);

        // Then
        assertTrue(result.isPresent());
        assertNotNull(result.get().getOrderItems());
        assertEquals(1, result.get().getOrderItems().size());

        OrderItemDTO orderItem = result.get().getOrderItems().get(0);
        assertEquals("TEST-CODE-123", orderItem.getActivationCode());
        assertEquals("测试游戏", orderItem.getGameTitle());
        assertEquals("http://example.com/image.jpg", orderItem.getImageUrl());

        verify(purchasedRepo, times(1)).findByOrderItemId(anyLong());
    }

    // ==================== findByUserId 方法测试 ====================

    @Test
    void testFindByUserId_Success() {
        // Given
        Long userId = 1L;
        List<Order> orders = Arrays.asList(testOrder);

        when(orderRepository.findOrdersWithItems(userId)).thenReturn(orders);
        when(gameRepository.findAllById(anyList())).thenReturn(Arrays.asList(testGame));
        when(purchasedRepo.findByOrderItemId(anyLong())).thenReturn(Optional.of(testActivationCode));

        // When
        List<OrderDTO> result = orderService.findByUserId(userId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(userId, result.get(0).getUserId());

        verify(orderRepository, times(1)).findOrdersWithItems(userId);
    }

    @Test
    void testFindByUserId_NoOrders() {
        // Given
        Long userId = 999L;

        when(orderRepository.findOrdersWithItems(userId)).thenReturn(Arrays.asList());

        // When
        List<OrderDTO> result = orderService.findByUserId(userId);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(orderRepository, times(1)).findOrdersWithItems(userId);
        verify(gameRepository, never()).findAllById(anyList());
    }

    @Test
    void testFindByUserId_MultipleOrders() {
        // Given
        Long userId = 1L;
        Order order2 = new Order();
        order2.setOrderId(2L);
        order2.setUserId(userId);
        order2.setStatus(OrderStatus.COMPLETED);
        order2.setPaymentMethod(PaymentMethod.PAYPAL);
        order2.setOrderDate(LocalDateTime.now());
        order2.setFinalAmount(new BigDecimal("149.99"));

        List<Order> orders = Arrays.asList(testOrder, order2);

        when(orderRepository.findOrdersWithItems(userId)).thenReturn(orders);
        when(gameRepository.findAllById(anyList())).thenReturn(Arrays.asList(testGame));
        when(purchasedRepo.findByOrderItemId(anyLong())).thenReturn(Optional.of(testActivationCode));

        // When
        List<OrderDTO> result = orderService.findByUserId(userId);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(userId, result.get(0).getUserId());
        assertEquals(userId, result.get(1).getUserId());

        verify(orderRepository, times(1)).findOrdersWithItems(userId);
    }

    // ==================== convertToDTO 方法测试 ====================

    @Test
    void testConvertToDTO_CompleteOrder() {
        // Given
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(gameRepository.findAllById(anyList())).thenReturn(Arrays.asList(testGame));
        when(purchasedRepo.findByOrderItemId(anyLong())).thenReturn(Optional.of(testActivationCode));

        // When
        OrderDTO result = orderService.findById(1L).orElse(null);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getOrderId());
        assertEquals(1L, result.getUserId());
        assertEquals("PENDING", result.getStatus());
        assertEquals("CREDIT_CARD", result.getPaymentMethod());
        assertEquals(new BigDecimal("199.98"), result.getFinalAmount());
        assertNotNull(result.getOrderDate());
        assertNotNull(result.getOrderItems());
        assertEquals(1, result.getOrderItems().size());

        OrderItemDTO orderItem = result.getOrderItems().get(0);
        assertEquals(1L, orderItem.getOrderItemId());
        assertEquals(1L, orderItem.getOrderId());
        assertEquals(1L, orderItem.getUserId());
        assertEquals("PENDING", orderItem.getOrderStatus());
        assertEquals(1L, orderItem.getGameId());
        assertEquals(new BigDecimal("99.99"), orderItem.getUnitPrice());
        assertEquals("TEST-CODE-123", orderItem.getActivationCode());
        assertEquals("测试游戏", orderItem.getGameTitle());
        assertEquals("http://example.com/image.jpg", orderItem.getImageUrl());
    }

    @Test
    void testConvertToDTO_OrderWithoutActivationCode() {
        // Given
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(gameRepository.findAllById(anyList())).thenReturn(Arrays.asList(testGame));
        when(purchasedRepo.findByOrderItemId(anyLong())).thenReturn(Optional.empty());

        // When
        OrderDTO result = orderService.findById(1L).orElse(null);

        // Then
        assertNotNull(result);
        assertNotNull(result.getOrderItems());
        assertEquals(1, result.getOrderItems().size());

        OrderItemDTO orderItem = result.getOrderItems().get(0);
        assertNull(orderItem.getActivationCode());
        assertEquals("测试游戏", orderItem.getGameTitle());
    }

    @Test
    void testConvertToDTO_OrderWithMissingGame() {
        // Given
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(gameRepository.findAllById(anyList())).thenReturn(Arrays.asList()); // 没有游戏信息
        when(purchasedRepo.findByOrderItemId(anyLong())).thenReturn(Optional.of(testActivationCode));

        // When
        OrderDTO result = orderService.findById(1L).orElse(null);

        // Then
        assertNotNull(result);
        assertNotNull(result.getOrderItems());
        assertEquals(1, result.getOrderItems().size());

        OrderItemDTO orderItem = result.getOrderItems().get(0);
        assertEquals("TEST-CODE-123", orderItem.getActivationCode());
        assertNull(orderItem.getGameTitle());
        assertNull(orderItem.getImageUrl());
    }

    // ==================== 集成测试场景 ====================

    @Test
    void testCompleteOrderLifecycle() {
        // 1. 创建待处理订单
        PaymentMethod paymentMethod = PaymentMethod.CREDIT_CARD;
        when(orderRepository.saveAndFlush(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setOrderId(1L);
            return order;
        });

        OrderDTO pendingOrder = orderService.createPendingOrder(testCart, paymentMethod);
        assertNotNull(pendingOrder);
        assertEquals("PENDING", pendingOrder.getStatus());

        // 2. 支付成功，完成订单
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(codeService.assignCodeToOrderItem(anyLong(), anyLong(), anyLong())).thenReturn(testActivationCode);

        OrderDTO completedOrder = orderService.captureAndFulfill(1L, 1L);
        assertNotNull(completedOrder);
        assertEquals("COMPLETED", completedOrder.getStatus());

        // 3. 查询订单
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(gameRepository.findAllById(anyList())).thenReturn(Arrays.asList(testGame));
        when(purchasedRepo.findByOrderItemId(anyLong())).thenReturn(Optional.of(testActivationCode));

        Optional<OrderDTO> foundOrder = orderService.findById(1L);
        assertTrue(foundOrder.isPresent());
        assertEquals("COMPLETED", foundOrder.get().getStatus());

        // 验证所有方法都被调用
        verify(orderRepository, times(1)).saveAndFlush(any(Order.class));
        verify(orderRepository, times(2)).findById(1L);
        verify(codeService, times(1)).assignCodeToOrderItem(anyLong(), anyLong(), anyLong());
    }

    @Test
    void testOrderFailureScenario() {
        // 1. 创建待处理订单
        PaymentMethod paymentMethod = PaymentMethod.CREDIT_CARD;
        when(orderRepository.saveAndFlush(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setOrderId(1L);
            return order;
        });

        OrderDTO pendingOrder = orderService.createPendingOrder(testCart, paymentMethod);
        assertEquals("PENDING", pendingOrder.getStatus());

        // 2. 支付失败，标记订单为失败
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        orderService.markFailed(1L, 1L);
        assertEquals(OrderStatus.CANCELLED, testOrder.getStatus());

        // 验证订单状态被正确更新
        verify(orderRepository, times(1)).findById(1L);
    }

    @Test
    void testUserOrderHistory() {
        // Given
        Long userId = 1L;
        Order order2 = new Order();
        order2.setOrderId(2L);
        order2.setUserId(userId);
        order2.setStatus(OrderStatus.COMPLETED);
        order2.setPaymentMethod(PaymentMethod.PAYPAL);
        order2.setOrderDate(LocalDateTime.now());
        order2.setFinalAmount(new BigDecimal("149.99"));

        List<Order> orders = Arrays.asList(testOrder, order2);

        when(orderRepository.findOrdersWithItems(userId)).thenReturn(orders);
        when(gameRepository.findAllById(anyList())).thenReturn(Arrays.asList(testGame));
        when(purchasedRepo.findByOrderItemId(anyLong())).thenReturn(Optional.of(testActivationCode));

        // When
        List<OrderDTO> userOrders = orderService.findByUserId(userId);

        // Then
        assertNotNull(userOrders);
        assertEquals(2, userOrders.size());
        assertEquals(userId, userOrders.get(0).getUserId());
        assertEquals(userId, userOrders.get(1).getUserId());

        verify(orderRepository, times(1)).findOrdersWithItems(userId);
    }

    @Test
    void testOrderWithMultipleItemsAndActivationCodes() {
        // Given
        Long orderId = 1L;
        Long userId = 1L;

        // 添加第二个订单项
        OrderItem item2 = new OrderItem();
        item2.setOrderItemId(2L);
        item2.setOrder(testOrder);
        item2.setUserId(1L);
        item2.setOrderDate(testOrder.getOrderDate());
        item2.setOrderStatus(OrderStatus.PENDING);
        item2.setGameId(2L);
        item2.setUnitPrice(new BigDecimal("49.99"));
        testOrder.getOrderItems().add(item2);

        // 设置第二个游戏
        Game game2 = new Game();
        game2.setGameId(2L);
        game2.setTitle("游戏2");
        game2.setImageUrl("http://example.com/game2.jpg");

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));
        when(codeService.assignCodeToOrderItem(userId, 1L, 1L)).thenReturn(testActivationCode);
        when(codeService.assignCodeToOrderItem(userId, 2L, 2L)).thenReturn(testActivationCode);

        // When
        OrderDTO result = orderService.captureAndFulfill(orderId, userId);

        // Then
        assertNotNull(result);
        assertEquals("COMPLETED", result.getStatus());
        assertEquals(2, result.getOrderItems().size());

        verify(codeService, times(2)).assignCodeToOrderItem(anyLong(), anyLong(), anyLong());
    }
}
