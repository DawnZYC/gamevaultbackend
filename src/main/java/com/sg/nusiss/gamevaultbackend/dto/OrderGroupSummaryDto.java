package com.sg.nusiss.gamevaultbackend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class OrderGroupSummaryDto {
    public Long orderId;
    public LocalDateTime createdAt;
    public String status;
    public BigDecimal total;
}




