package com.sg.nusiss.gamevaultbackend.dto.shopping;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CartDTO {
    private Long cartId;
    private Long userId;
    private List<CartItemDTO> cartItems;
    private String status;
    private LocalDateTime createdDate;
    private LocalDateTime lastModifiedDate;
    private String paymentMethod;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
}
