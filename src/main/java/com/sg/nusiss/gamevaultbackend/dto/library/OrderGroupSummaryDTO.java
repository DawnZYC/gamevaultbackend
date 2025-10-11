package com.sg.nusiss.gamevaultbackend.dto.library;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class OrderGroupSummaryDTO {
    public Long orderId;
    public LocalDateTime createdAt;
    public String status;
    public BigDecimal total;
}




