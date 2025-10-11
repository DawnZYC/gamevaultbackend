package com.sg.nusiss.gamevaultbackend.dto.library;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderItemDTO {
    public Long orderItemId;
    public Long orderId;
    public Long userId;
    public Long gameId;
    public BigDecimal unitPrice;
    public BigDecimal discountPrice;
    public LocalDateTime orderDate;
    public String orderStatus;

    public String activationCode;
    public String gameTitle;
    public String imageUrl;
}




