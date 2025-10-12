package com.sg.nusiss.gamevaultbackend.service.shopping;

import com.sg.nusiss.gamevaultbackend.dto.library.OrderDTO;
import com.sg.nusiss.gamevaultbackend.dto.library.OrderItemDTO;
import com.sg.nusiss.gamevaultbackend.dto.shopping.*;
import com.sg.nusiss.gamevaultbackend.entity.ENUM.OrderStatus;
import com.sg.nusiss.gamevaultbackend.entity.ENUM.PaymentMethod;
import com.sg.nusiss.gamevaultbackend.entity.ENUM.CartStatus;
import com.sg.nusiss.gamevaultbackend.entity.shopping.*;
import com.sg.nusiss.gamevaultbackend.repository.shopping.CartRepository;
import com.sg.nusiss.gamevaultbackend.repository.shopping.GameRepository;
import com.sg.nusiss.gamevaultbackend.repository.shopping.OrderRepository;
import com.sg.nusiss.gamevaultbackend.service.discount.IDiscountStrategy;
import com.sg.nusiss.gamevaultbackend.service.discount.NoDiscountStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final GameRepository gameRepository;
    private final OrderRepository orderRepository;

    private IDiscountStrategy discountStrategy = new NoDiscountStrategy();

    @Transactional
    public CartDTO getCart(Long userId) {
        Cart cart = getOrCreate(userId);
        return toDTO(cart);
    }

    @Transactional
    public CartDTO addGame(Long userId, Long gameId, int quantity) {
        if (quantity < 1) quantity = 1;
        Cart cart = getOrCreate(userId);

        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new NoSuchElementException("Game not found: " + gameId));

        Optional<CartItem> existing = cart.getCartItems().stream()
                .filter(ci -> ci.getGameId().equals(gameId))
                .findFirst();

        if (existing.isPresent()) {
            existing.get().setQuantity(existing.get().getQuantity() + quantity);
        } else {
            CartItem item = new CartItem(gameId, game.getCurrentPrice(), quantity);
            item.setCart(cart);
            cart.getCartItems().add(item);
        }

        cart.setLastModifiedDate(LocalDateTime.now());
        cartRepository.save(cart);
        return toDTO(cart);
    }

    @Transactional
    public CartDTO removeGame(Long userId, Long gameId) {
        Cart cart = getOrCreate(userId);
        cart.removeGame(gameId);
        cartRepository.save(cart);
        return toDTO(cart);
    }

    /** 🔄 更新购物车商品数量 */
    @Transactional
    public CartDTO updateQuantity(Long userId, Long gameId, int quantity) {
        if (quantity < 1) {
            throw new IllegalArgumentException("Quantity must be at least 1");
        }
        
        Cart cart = getOrCreate(userId);
        
        Optional<CartItem> existing = cart.getCartItems().stream()
                .filter(ci -> ci.getGameId().equals(gameId))
                .findFirst();
        
        if (existing.isEmpty()) {
            throw new NoSuchElementException("Game not found in cart: " + gameId);
        }
        
        existing.get().setQuantity(quantity);
        cart.setLastModifiedDate(LocalDateTime.now());
        cartRepository.save(cart);
        return toDTO(cart);
    }

    @Transactional
    public CartDTO clearCart(Long userId) {
        Cart cart = getOrCreate(userId);
        cart.clear();
        cartRepository.save(cart);
        return toDTO(cart);
    }

    public BigDecimal calculateTotalAmount(Long userId) {
        return cartRepository.sumTotalByUserId(userId); // 使用自定义方法
    }

    public void setDiscountStrategy(IDiscountStrategy strategy) {
        this.discountStrategy = strategy;
    }

    @Transactional
    public boolean applyDiscounts(Long userId) {
        Cart cart = getOrCreate(userId);
        BigDecimal total = cart.calculateTotalAmount();
        BigDecimal discountTotal = BigDecimal.ZERO;

        for (CartItem ci : cart.getCartItems()) {
            Game g = gameRepository.findById(ci.getGameId())
                    .orElseThrow(() -> new NoSuchElementException("Game not found: " + ci.getGameId()));

            if (discountStrategy != null && discountStrategy.isApplicable(g)) {
                BigDecimal subtotal = ci.calculateSubtotal(); // 单项小计 = 单价 * 数量
                BigDecimal d = discountStrategy.calculateDiscount(g, subtotal);
                if (d == null) d = BigDecimal.ZERO;
                if (d.signum() < 0) d = BigDecimal.ZERO;
                if (d.compareTo(subtotal) > 0) d = subtotal;  // 不超过该项小计
                discountTotal = discountTotal.add(d);
            }
        }

        if (discountTotal.compareTo(total) > 0) discountTotal = total; // 再保险
        cart.setDiscountAmount(discountTotal);
        cart.setFinalAmount(total.subtract(discountTotal));
        cartRepository.save(cart);
        return discountTotal.signum() > 0;
    }


    public BigDecimal calculateFinalAmount(Long userId) {
        Cart cart = getOrCreate(userId);
        return cart.calculateFinalAmount();
    }

    // 计算购物车内游戏物品总价，生成订单
    @Transactional
    public OrderDTO checkout(Long userId, PaymentMethod paymentMethod) {
        Cart cart = getOrCreate(userId);
        if (cart.isEmpty()) {
            throw new IllegalStateException("Cart is empty, cannot checkout");
        }

        Order order = new Order();
        order.setUserId(userId);
        order.setOrderDate(LocalDateTime.now());
        order.setStatus(OrderStatus.PENDING);
        order.setPaymentMethod(paymentMethod);

        BigDecimal finalAmount = BigDecimal.ZERO;

        for (CartItem ci : cart.getCartItems()) {
            // 一个 CartItem.quantity = n -> 生成 n 条 OrderItem
            for (int i = 0; i < ci.getQuantity(); i++) {
                OrderItem oi = new OrderItem();
                oi.setOrder(order);
                oi.setUserId(userId);
                oi.setOrderDate(order.getOrderDate());
                oi.setOrderStatus(OrderStatus.PENDING);
                oi.setGameId(ci.getGameId());
                oi.setUnitPrice(ci.getPrice());

                order.getOrderItems().add(oi);
                finalAmount = finalAmount.add(ci.getPrice());
            }
        }

        order.setFinalAmount(finalAmount);
        Order saved = orderRepository.save(order);

        // 订单创建后立即清空购物车，避免重复购买
        cart.getCartItems().clear();
        cart.setStatus(CartStatus.ACTIVE); // 重置为活跃状态，但内容已清空
        cart.setPaymentMethod(paymentMethod);
        cart.setLastModifiedDate(LocalDateTime.now());
        cartRepository.save(cart);

        return convertToDTO(saved);
    }


    private OrderDTO convertToDTO(Order order) {
        OrderDTO dto = new OrderDTO();
        dto.setOrderId(order.getOrderId());
        dto.setUserId(order.getUserId());
        dto.setOrderDate(order.getOrderDate());
        dto.setStatus(order.getStatus().name());
        dto.setPaymentMethod(order.getPaymentMethod().name());
        dto.setFinalAmount(order.getFinalAmount());

        List<OrderItemDTO> itemDTOs = order.getOrderItems().stream().map(oi -> {
            OrderItemDTO oid = new OrderItemDTO();
            oid.setOrderItemId(oi.getOrderItemId());
            oid.setOrderId(order.getOrderId());
            oid.setUserId(oi.getUserId());
            oid.setOrderDate(oi.getOrderDate());
            oid.setOrderStatus(oi.getOrderStatus().name());
            oid.setGameId(oi.getGameId());
            oid.setUnitPrice(oi.getUnitPrice());
            return oid;
        }).toList();

        dto.setOrderItems(itemDTOs);
        return dto;
    }



    // ——— 内部方法 ———
    private Cart getOrCreate(Long userId) {
        return cartRepository.findByUserId(userId).orElseGet(() -> {
            Cart c = new Cart(userId);
            return cartRepository.save(c);
        });
    }

    private CartDTO toDTO(Cart cart) {
        CartDTO dto = new CartDTO();
        dto.setCartId(cart.getCartId());
        dto.setUserId(cart.getUserId());
        dto.setStatus(cart.getStatus() != null ? cart.getStatus().name() : null);
        dto.setCreatedDate(cart.getCreatedDate());
        dto.setLastModifiedDate(cart.getLastModifiedDate());
        dto.setPaymentMethod(cart.getPaymentMethod() != null ? cart.getPaymentMethod().name() : null);
        dto.setDiscountAmount(cart.getDiscountAmount());
        dto.setFinalAmount(cart.getFinalAmount());

        List<Long> ids = cart.getCartItems().stream().map(CartItem::getGameId).toList();
        Map<Long, Game> gameMap = gameRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Game::getGameId, g -> g));

        List<CartItemDTO> items = cart.getCartItems().stream().map(ci -> {
            CartItemDTO cid = new CartItemDTO();
            cid.setCartItemId(ci.getCartItemId());
            cid.setUnitPrice(ci.getPrice());
            cid.setQuantity(ci.getQuantity());
            cid.setSubtotal(ci.calculateSubtotal());

            Game g = gameMap.get(ci.getGameId());
            GameDTO gd = new GameDTO();
            if (g != null) {
                gd.setGameId(g.getGameId());
                gd.setTitle(g.getTitle());
                gd.setDeveloper(g.getDeveloper());
                gd.setDescription(g.getDescription());
                gd.setPrice(g.getPrice());
                gd.setDiscountPrice(g.getDiscountPrice());
                gd.setGenre(g.getGenre());
                gd.setPlatform(g.getPlatform());
                gd.setReleaseDate(g.getReleaseDate());
                gd.setIsActive(g.getIsActive());
                gd.setImageUrl(g.getImageUrl());
            } else {
                gd.setGameId(ci.getGameId());
            }
            cid.setGame(gd);
            return cid;
        }).toList();

        dto.setCartItems(items);
        return dto;
    }

    public Cart getCartEntity(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("Cart not found for user " + userId));
    }

    @Transactional
    public void markCheckedOut(Long userId, PaymentMethod method) {
        Cart cart = getOrCreate(userId); // 获取或创建用户的购物车
        if (cart.isEmpty()) {
            throw new IllegalStateException("Cart is empty, cannot checkout");
        }
        cart.setStatus(CartStatus.CHECKED_OUT);
        cart.setPaymentMethod(method);
        cart.setLastModifiedDate(LocalDateTime.now());

        // 可选：清空购物车条目
        cart.getCartItems().clear();

        cartRepository.save(cart);
    }

}
