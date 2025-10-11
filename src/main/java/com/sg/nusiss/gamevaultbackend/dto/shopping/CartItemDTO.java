package com.sg.nusiss.gamevaultbackend.dto.shopping;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class CartItemDTO {
    private Long cartItemId;
    private GameDTO game;          // 购物车条目里嵌套 GameDTO
    private BigDecimal unitPrice;
    private Integer quantity;
    private BigDecimal subtotal;
}
