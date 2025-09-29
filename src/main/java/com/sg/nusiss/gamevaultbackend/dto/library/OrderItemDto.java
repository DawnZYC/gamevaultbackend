package com.sg.nusiss.gamevaultbackend.dto.library;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class OrderItemDto {
    public Long orderItemId;
    public Long orderId;
    public Long userId;
    public Long gameId;
    public BigDecimal unitPrice;
    public BigDecimal discountPrice;
    public LocalDateTime orderDate;
    public String orderStatus;
}




