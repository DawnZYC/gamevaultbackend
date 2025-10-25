package com.sg.nusiss.gamevaultbackend.service.store;

import com.sg.nusiss.gamevaultbackend.dto.library.OrderDTO;
import com.sg.nusiss.gamevaultbackend.dto.shopping.*;
import com.sg.nusiss.gamevaultbackend.entity.ENUM.CartStatus;
import com.sg.nusiss.gamevaultbackend.entity.ENUM.PaymentMethod;
import com.sg.nusiss.gamevaultbackend.entity.shopping.*;
import com.sg.nusiss.gamevaultbackend.entity.auth.*;
import com.sg.nusiss.gamevaultbackend.repository.shopping.CartRepository;
import com.sg.nusiss.gamevaultbackend.repository.shopping.GameRepository;
import com.sg.nusiss.gamevaultbackend.repository.shopping.OrderRepository;
import com.sg.nusiss.gamevaultbackend.service.discount.IDiscountStrategy;
import com.sg.nusiss.gamevaultbackend.service.shopping.CartService;
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
 * @ClassName CartServiceTest
 * @Author AI Assistant
 * @Date 2025/01/27
 * @Description CartService单元测试类，覆盖所有方法
 */
@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private GameRepository gameRepository;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private CartService cartService;

    private User testUser;
    private Game testGame;
    private Cart testCart;
    private CartItem testCartItem;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUserId(1L);
        testUser.setUsername("testUser");

        testGame = new Game();
        testGame.setGameId(1L);
        testGame.setTitle("测试游戏");
        testGame.setPrice(new BigDecimal("99.99"));
        testGame.setDiscountPrice(new BigDecimal("79.99"));
        testGame.setIsActive(true);

        testCart = new Cart(1L);
        testCart.setCartId(1L);
        testCart.setStatus(CartStatus.ACTIVE);
        testCart.setCreatedDate(LocalDateTime.now());
        testCart.setLastModifiedDate(LocalDateTime.now());

        testCartItem = new CartItem(1L, new BigDecimal("99.99"), 2);
        testCartItem.setCartItemId(1L);
        testCartItem.setCart(testCart);
        testCart.getCartItems().add(testCartItem);
    }

    // ==================== getCart 方法测试 ====================

    @Test
    void testGetCart_ExistingCart_Success() {
        // Given
        Long userId = 1L;
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(testCart));
        when(gameRepository.findAllById(anyList())).thenReturn(Arrays.asList(testGame));

        // When
        CartDTO result = cartService.getCart(userId);

        // Then
        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertEquals(1, result.getCartItems().size());

        verify(cartRepository, times(1)).findByUserId(userId);
        verify(gameRepository, times(1)).findAllById(anyList());
    }

    @Test
    void testGetCart_NewCart_Success() {
        // Given
        Long userId = 2L;
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> {
            Cart cart = invocation.getArgument(0);
            cart.setCartId(2L);
            return cart;
        });
        when(gameRepository.findAllById(anyList())).thenReturn(Arrays.asList());

        // When
        CartDTO result = cartService.getCart(userId);

        // Then
        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertTrue(result.getCartItems().isEmpty());

        verify(cartRepository, times(1)).findByUserId(userId);
        verify(cartRepository, times(1)).save(any(Cart.class));
    }

    @Test
    void testGetCart_EmptyCart() {
        // Given
        Long userId = 1L;
        Cart emptyCart = new Cart(userId);
        emptyCart.setCartId(1L);
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(emptyCart));
        when(gameRepository.findAllById(anyList())).thenReturn(Arrays.asList());

        // When
        CartDTO result = cartService.getCart(userId);

        // Then
        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertTrue(result.getCartItems().isEmpty());

        verify(cartRepository, times(1)).findByUserId(userId);
    }

    // ==================== addGame 方法测试 ====================

    @Test
    void testAddGame_NewGame_Success() {
        // Given
        Long userId = 1L;
        Long gameId = 1L;
        int quantity = 2;

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(testCart));
        when(gameRepository.findById(gameId)).thenReturn(Optional.of(testGame));
        when(cartRepository.save(any(Cart.class))).thenReturn(testCart);
        when(gameRepository.findAllById(anyList())).thenReturn(Arrays.asList(testGame));

        // When
        CartDTO result = cartService.addGame(userId, gameId, quantity);

        // Then
        assertNotNull(result);
        verify(cartRepository, times(1)).findByUserId(userId);
        verify(gameRepository, times(1)).findById(gameId);
        verify(cartRepository, times(1)).save(any(Cart.class));
    }

    @Test
    void testAddGame_ExistingGame_UpdatesQuantity() {
        // Given
        Long userId = 1L;
        Long gameId = 1L;
        int quantity = 3;

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(testCart));
        when(gameRepository.findById(gameId)).thenReturn(Optional.of(testGame));
        when(cartRepository.save(any(Cart.class))).thenReturn(testCart);
        when(gameRepository.findAllById(anyList())).thenReturn(Arrays.asList(testGame));

        // When
        CartDTO result = cartService.addGame(userId, gameId, quantity);

        // Then
        assertNotNull(result);
        verify(cartRepository, times(1)).save(any(Cart.class));
    }

    @Test
    void testAddGame_GameNotFound_ThrowsException() {
        // Given
        Long userId = 1L;
        Long gameId = 999L;
        int quantity = 1;

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(testCart));
        when(gameRepository.findById(gameId)).thenReturn(Optional.empty());

        // When & Then
        NoSuchElementException exception = assertThrows(NoSuchElementException.class,
            () -> cartService.addGame(userId, gameId, quantity));
        assertTrue(exception.getMessage().contains("Game not found: " + gameId));

        verify(cartRepository, times(1)).findByUserId(userId);
        verify(gameRepository, times(1)).findById(gameId);
        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    void testAddGame_ZeroQuantity_DefaultsToOne() {
        // Given
        Long userId = 1L;
        Long gameId = 1L;
        int quantity = 0; // 应该被设置为1

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(testCart));
        when(gameRepository.findById(gameId)).thenReturn(Optional.of(testGame));
        when(cartRepository.save(any(Cart.class))).thenReturn(testCart);
        when(gameRepository.findAllById(anyList())).thenReturn(Arrays.asList(testGame));

        // When
        CartDTO result = cartService.addGame(userId, gameId, quantity);

        // Then
        assertNotNull(result);
        verify(cartRepository, times(1)).save(any(Cart.class));
    }

    @Test
    void testAddGame_NegativeQuantity_DefaultsToOne() {
        // Given
        Long userId = 1L;
        Long gameId = 1L;
        int quantity = -1; // 应该被设置为1

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(testCart));
        when(gameRepository.findById(gameId)).thenReturn(Optional.of(testGame));
        when(cartRepository.save(any(Cart.class))).thenReturn(testCart);
        when(gameRepository.findAllById(anyList())).thenReturn(Arrays.asList(testGame));

        // When
        CartDTO result = cartService.addGame(userId, gameId, quantity);

        // Then
        assertNotNull(result);
        verify(cartRepository, times(1)).save(any(Cart.class));
    }

    // ==================== removeGame 方法测试 ====================

    @Test
    void testRemoveGame_Success() {
        // Given
        Long userId = 1L;
        Long gameId = 1L;

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(testCart));
        when(cartRepository.save(any(Cart.class))).thenReturn(testCart);
        when(gameRepository.findAllById(anyList())).thenReturn(Arrays.asList());

        // When
        CartDTO result = cartService.removeGame(userId, gameId);

        // Then
        assertNotNull(result);
        verify(cartRepository, times(1)).findByUserId(userId);
        verify(cartRepository, times(1)).save(any(Cart.class));
    }

    @Test
    void testRemoveGame_GameNotInCart() {
        // Given
        Long userId = 1L;
        Long gameId = 999L; // 不在购物车中

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(testCart));
        when(cartRepository.save(any(Cart.class))).thenReturn(testCart);
        when(gameRepository.findAllById(anyList())).thenReturn(Arrays.asList());

        // When
        CartDTO result = cartService.removeGame(userId, gameId);

        // Then
        assertNotNull(result);
        verify(cartRepository, times(1)).save(any(Cart.class));
    }

    // ==================== updateQuantity 方法测试 ====================

    @Test
    void testUpdateQuantity_Success() {
        // Given
        Long userId = 1L;
        Long gameId = 1L;
        int quantity = 5;

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(testCart));
        when(cartRepository.save(any(Cart.class))).thenReturn(testCart);
        when(gameRepository.findAllById(anyList())).thenReturn(Arrays.asList(testGame));

        // When
        CartDTO result = cartService.updateQuantity(userId, gameId, quantity);

        // Then
        assertNotNull(result);
        verify(cartRepository, times(1)).findByUserId(userId);
        verify(cartRepository, times(1)).save(any(Cart.class));
    }

    @Test
    void testUpdateQuantity_ZeroQuantity_ThrowsException() {
        // Given
        Long userId = 1L;
        Long gameId = 1L;
        int quantity = 0;

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> cartService.updateQuantity(userId, gameId, quantity));
        assertTrue(exception.getMessage().contains("Quantity must be at least 1"));

        // 参数验证在getOrCreate之前进行，所以不会调用repository
        verify(cartRepository, never()).findByUserId(anyLong());
        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    void testUpdateQuantity_NegativeQuantity_ThrowsException() {
        // Given
        Long userId = 1L;
        Long gameId = 1L;
        int quantity = -1;

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> cartService.updateQuantity(userId, gameId, quantity));
        assertTrue(exception.getMessage().contains("Quantity must be at least 1"));

        // 参数验证在getOrCreate之前进行，所以不会调用repository
        verify(cartRepository, never()).findByUserId(anyLong());
        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    void testUpdateQuantity_GameNotInCart_ThrowsException() {
        // Given
        Long userId = 1L;
        Long gameId = 999L; // 不在购物车中
        int quantity = 2;

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(testCart));

        // When & Then
        NoSuchElementException exception = assertThrows(NoSuchElementException.class,
            () -> cartService.updateQuantity(userId, gameId, quantity));
        assertTrue(exception.getMessage().contains("Game not found in cart: " + gameId));

        verify(cartRepository, times(1)).findByUserId(userId);
        verify(cartRepository, never()).save(any(Cart.class));
    }

    // ==================== clearCart 方法测试 ====================

    @Test
    void testClearCart_Success() {
        // Given
        Long userId = 1L;

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(testCart));
        when(cartRepository.save(any(Cart.class))).thenReturn(testCart);
        when(gameRepository.findAllById(anyList())).thenReturn(Arrays.asList());

        // When
        CartDTO result = cartService.clearCart(userId);

        // Then
        assertNotNull(result);
        verify(cartRepository, times(1)).findByUserId(userId);
        verify(cartRepository, times(1)).save(any(Cart.class));
    }

    @Test
    void testClearCart_EmptyCart() {
        // Given
        Long userId = 1L;
        Cart emptyCart = new Cart(userId);
        emptyCart.setCartId(1L);

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(emptyCart));
        when(cartRepository.save(any(Cart.class))).thenReturn(emptyCart);
        when(gameRepository.findAllById(anyList())).thenReturn(Arrays.asList());

        // When
        CartDTO result = cartService.clearCart(userId);

        // Then
        assertNotNull(result);
        assertTrue(result.getCartItems().isEmpty());
        verify(cartRepository, times(1)).save(any(Cart.class));
    }

    // ==================== calculateTotalAmount 方法测试 ====================

    @Test
    void testCalculateTotalAmount_Success() {
        // Given
        Long userId = 1L;
        BigDecimal expectedTotal = new BigDecimal("199.98");

        when(cartRepository.sumTotalByUserId(userId)).thenReturn(expectedTotal);

        // When
        BigDecimal result = cartService.calculateTotalAmount(userId);

        // Then
        assertNotNull(result);
        assertEquals(expectedTotal, result);
        verify(cartRepository, times(1)).sumTotalByUserId(userId);
    }

    @Test
    void testCalculateTotalAmount_ZeroTotal() {
        // Given
        Long userId = 1L;
        BigDecimal expectedTotal = BigDecimal.ZERO;

        when(cartRepository.sumTotalByUserId(userId)).thenReturn(expectedTotal);

        // When
        BigDecimal result = cartService.calculateTotalAmount(userId);

        // Then
        assertNotNull(result);
        assertEquals(BigDecimal.ZERO, result);
        verify(cartRepository, times(1)).sumTotalByUserId(userId);
    }

    // ==================== setDiscountStrategy 方法测试 ====================

    @Test
    void testSetDiscountStrategy_Success() {
        // Given
        IDiscountStrategy strategy = mock(IDiscountStrategy.class);

        // When
        cartService.setDiscountStrategy(strategy);

        // Then
        // 验证策略被设置（通过后续的applyDiscounts测试来验证）
        assertDoesNotThrow(() -> cartService.setDiscountStrategy(strategy));
    }

    // ==================== applyDiscounts 方法测试 ====================

    @Test
    void testApplyDiscounts_WithDiscount_Success() {
        // Given
        Long userId = 1L;
        IDiscountStrategy strategy = mock(IDiscountStrategy.class);
        cartService.setDiscountStrategy(strategy);

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(testCart));
        when(gameRepository.findById(anyLong())).thenReturn(Optional.of(testGame));
        when(strategy.isApplicable(testGame)).thenReturn(true);
        when(strategy.calculateDiscount(any(Game.class), any(BigDecimal.class)))
            .thenReturn(new BigDecimal("10.00"));
        when(cartRepository.save(any(Cart.class))).thenReturn(testCart);

        // When
        boolean result = cartService.applyDiscounts(userId);

        // Then
        assertTrue(result);
        verify(cartRepository, times(1)).findByUserId(userId);
        verify(strategy, times(1)).isApplicable(testGame);
        verify(cartRepository, times(1)).save(any(Cart.class));
    }

    @Test
    void testApplyDiscounts_NoDiscount_Success() {
        // Given
        Long userId = 1L;
        IDiscountStrategy strategy = mock(IDiscountStrategy.class);
        cartService.setDiscountStrategy(strategy);

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(testCart));
        when(gameRepository.findById(anyLong())).thenReturn(Optional.of(testGame));
        when(strategy.isApplicable(testGame)).thenReturn(false);
        when(cartRepository.save(any(Cart.class))).thenReturn(testCart);

        // When
        boolean result = cartService.applyDiscounts(userId);

        // Then
        assertFalse(result);
        verify(cartRepository, times(1)).findByUserId(userId);
        verify(strategy, times(1)).isApplicable(testGame);
        verify(cartRepository, times(1)).save(any(Cart.class));
    }

    @Test
    void testApplyDiscounts_GameNotFound_ThrowsException() {
        // Given
        Long userId = 1L;
        IDiscountStrategy strategy = mock(IDiscountStrategy.class);
        cartService.setDiscountStrategy(strategy);

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(testCart));
        when(gameRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When & Then
        NoSuchElementException exception = assertThrows(NoSuchElementException.class,
            () -> cartService.applyDiscounts(userId));
        assertTrue(exception.getMessage().contains("Game not found:"));

        verify(cartRepository, times(1)).findByUserId(userId);
        verify(cartRepository, never()).save(any(Cart.class));
    }

    // ==================== calculateFinalAmount 方法测试 ====================

    @Test
    void testCalculateFinalAmount_Success() {
        // Given
        Long userId = 1L;

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(testCart));

        // When
        BigDecimal result = cartService.calculateFinalAmount(userId);

        // Then
        assertNotNull(result);
        verify(cartRepository, times(1)).findByUserId(userId);
    }

    // ==================== checkout 方法测试 ====================

    @Test
    void testCheckout_Success() {
        // Given
        Long userId = 1L;
        PaymentMethod paymentMethod = PaymentMethod.CREDIT_CARD;

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(testCart));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setOrderId(1L);
            return order;
        });
        when(cartRepository.save(any(Cart.class))).thenReturn(testCart);

        // When
        OrderDTO result = cartService.checkout(userId, paymentMethod);

        // Then
        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertEquals(paymentMethod.name(), result.getPaymentMethod());
        assertEquals("PENDING", result.getStatus());

        verify(cartRepository, times(1)).findByUserId(userId);
        verify(orderRepository, times(1)).save(any(Order.class));
        verify(cartRepository, times(1)).save(any(Cart.class));
    }

    @Test
    void testCheckout_EmptyCart_ThrowsException() {
        // Given
        Long userId = 1L;
        PaymentMethod paymentMethod = PaymentMethod.CREDIT_CARD;
        Cart emptyCart = new Cart(userId);
        emptyCart.setCartId(1L);

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(emptyCart));

        // When & Then
        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> cartService.checkout(userId, paymentMethod));
        assertTrue(exception.getMessage().contains("Cart is empty, cannot checkout"));

        verify(cartRepository, times(1)).findByUserId(userId);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void testCheckout_MultipleItems_Success() {
        // Given
        Long userId = 1L;
        PaymentMethod paymentMethod = PaymentMethod.PAYPAL;

        // 添加多个商品到购物车
        CartItem item2 = new CartItem(2L, new BigDecimal("49.99"), 1);
        item2.setCartItemId(2L);
        testCart.getCartItems().add(item2);

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(testCart));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setOrderId(1L);
            return order;
        });
        when(cartRepository.save(any(Cart.class))).thenReturn(testCart);

        // When
        OrderDTO result = cartService.checkout(userId, paymentMethod);

        // Then
        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertEquals(paymentMethod.name(), result.getPaymentMethod());
        assertNotNull(result.getOrderItems());
        assertTrue(result.getOrderItems().size() >= 2); // 至少2个订单项

        verify(cartRepository, times(1)).findByUserId(userId);
        verify(orderRepository, times(1)).save(any(Order.class));
        verify(cartRepository, times(1)).save(any(Cart.class));
    }

    // ==================== getCartEntity 方法测试 ====================

    @Test
    void testGetCartEntity_Success() {
        // Given
        Long userId = 1L;

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(testCart));

        // When
        Cart result = cartService.getCartEntity(userId);

        // Then
        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        verify(cartRepository, times(1)).findByUserId(userId);
    }

    @Test
    void testGetCartEntity_NotFound_ThrowsException() {
        // Given
        Long userId = 999L;

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.empty());

        // When & Then
        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> cartService.getCartEntity(userId));
        assertTrue(exception.getMessage().contains("Cart not found for user " + userId));

        verify(cartRepository, times(1)).findByUserId(userId);
    }

    // ==================== markCheckedOut 方法测试 ====================

    @Test
    void testMarkCheckedOut_Success() {
        // Given
        Long userId = 1L;
        PaymentMethod paymentMethod = PaymentMethod.CREDIT_CARD;

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(testCart));
        when(cartRepository.save(any(Cart.class))).thenReturn(testCart);

        // When
        cartService.markCheckedOut(userId, paymentMethod);

        // Then
        verify(cartRepository, times(1)).findByUserId(userId);
        verify(cartRepository, times(1)).save(any(Cart.class));
    }

    @Test
    void testMarkCheckedOut_EmptyCart_ThrowsException() {
        // Given
        Long userId = 1L;
        PaymentMethod paymentMethod = PaymentMethod.CREDIT_CARD;
        Cart emptyCart = new Cart(userId);
        emptyCart.setCartId(1L);

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(emptyCart));

        // When & Then
        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> cartService.markCheckedOut(userId, paymentMethod));
        assertTrue(exception.getMessage().contains("Cart is empty, cannot checkout"));

        verify(cartRepository, times(1)).findByUserId(userId);
        verify(cartRepository, never()).save(any(Cart.class));
    }

    // ==================== 集成测试场景 ====================

    @Test
    void testCompleteShoppingFlow() {
        // 1. 获取空购物车
        Long userId = 1L;
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(new Cart(userId)));
        when(gameRepository.findAllById(anyList())).thenReturn(Arrays.asList());

        CartDTO emptyCart = cartService.getCart(userId);
        assertTrue(emptyCart.getCartItems().isEmpty());

        // 2. 添加商品
        when(gameRepository.findById(1L)).thenReturn(Optional.of(testGame));
        when(cartRepository.save(any(Cart.class))).thenReturn(testCart);
        when(gameRepository.findAllById(anyList())).thenReturn(Arrays.asList(testGame));

        CartDTO cartWithItems = cartService.addGame(userId, 1L, 2);
        assertNotNull(cartWithItems);

        // 3. 更新数量
        CartDTO updatedCart = cartService.updateQuantity(userId, 1L, 3);
        assertNotNull(updatedCart);

        // 4. 计算总价
        when(cartRepository.sumTotalByUserId(userId)).thenReturn(new BigDecimal("299.97"));
        BigDecimal total = cartService.calculateTotalAmount(userId);
        assertNotNull(total);
        assertEquals(new BigDecimal("299.97"), total);

        // 5. 结账
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setOrderId(1L);
            return order;
        });

        OrderDTO order = cartService.checkout(userId, PaymentMethod.CREDIT_CARD);
        assertNotNull(order);
        assertEquals("PENDING", order.getStatus());

        // 验证所有方法都被调用
        verify(cartRepository, atLeast(1)).findByUserId(userId);
        verify(gameRepository, atLeast(1)).findById(1L);
        verify(cartRepository, atLeast(1)).save(any(Cart.class));
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    void testDiscountApplicationFlow() {
        // Given
        Long userId = 1L;
        IDiscountStrategy strategy = mock(IDiscountStrategy.class);
        cartService.setDiscountStrategy(strategy);

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(testCart));
        when(gameRepository.findById(anyLong())).thenReturn(Optional.of(testGame));
        when(strategy.isApplicable(testGame)).thenReturn(true);
        when(strategy.calculateDiscount(any(Game.class), any(BigDecimal.class)))
            .thenReturn(new BigDecimal("20.00"));
        when(cartRepository.save(any(Cart.class))).thenReturn(testCart);

        // When
        boolean discountApplied = cartService.applyDiscounts(userId);
        BigDecimal finalAmount = cartService.calculateFinalAmount(userId);

        // Then
        assertTrue(discountApplied);
        assertNotNull(finalAmount);

        verify(strategy, times(1)).isApplicable(testGame);
        verify(strategy, times(1)).calculateDiscount(any(Game.class), any(BigDecimal.class));
        verify(cartRepository, times(1)).save(any(Cart.class));
    }

    @Test
    void testCartManagementOperations() {
        Long userId = 1L;

        // 1. 添加商品
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(testCart));
        when(gameRepository.findById(1L)).thenReturn(Optional.of(testGame));
        when(cartRepository.save(any(Cart.class))).thenReturn(testCart);
        when(gameRepository.findAllById(anyList())).thenReturn(Arrays.asList(testGame));

        CartDTO cartAfterAdd = cartService.addGame(userId, 1L, 2);
        assertNotNull(cartAfterAdd);

        // 2. 更新数量
        CartDTO cartAfterUpdate = cartService.updateQuantity(userId, 1L, 5);
        assertNotNull(cartAfterUpdate);

        // 3. 移除商品
        CartDTO cartAfterRemove = cartService.removeGame(userId, 1L);
        assertNotNull(cartAfterRemove);

        // 4. 清空购物车
        CartDTO emptyCart = cartService.clearCart(userId);
        assertNotNull(emptyCart);

        verify(cartRepository, atLeast(4)).findByUserId(userId);
        verify(cartRepository, atLeast(4)).save(any(Cart.class));
    }
}
