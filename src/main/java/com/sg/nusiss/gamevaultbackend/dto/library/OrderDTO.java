package com.sg.nusiss.gamevaultbackend.dto.library;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderDTO {
    private Long orderId;
    private Long userId;
    private LocalDateTime orderDate;
    private String status;
    private String paymentMethod;
    private BigDecimal finalAmount;
    private List<OrderItemDTO> orderItems;
}
