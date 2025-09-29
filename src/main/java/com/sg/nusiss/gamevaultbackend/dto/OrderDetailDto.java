package com.sg.nusiss.gamevaultbackend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class OrderDetailDto {
    public Long orderItemId;
    public Long orderId;
    public Long userId;
    public Long gameId;
    public LocalDateTime orderDate;
    public String orderStatus;
    public BigDecimal unitPrice;
    public BigDecimal discountPrice;
    public List<String> activationCodes;
}




