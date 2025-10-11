package com.sg.nusiss.gamevaultbackend.dto.library;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class OrderDetailDTO {
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




