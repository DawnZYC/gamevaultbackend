package com.sg.nusiss.gamevaultbackend.service.discount;

import com.sg.nusiss.gamevaultbackend.entity.shopping.Game;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class PercentageDiscount implements IDiscountStrategy {

    private final int percent; // 0-100

    public PercentageDiscount(int percent) {
        this.percent = Math.max(0, Math.min(100, percent));
    }

    @Override
    public BigDecimal calculateDiscount(Game game, BigDecimal originalPrice) {
        if (originalPrice == null) return BigDecimal.ZERO;
        return originalPrice.multiply(BigDecimal.valueOf(percent))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    @Override
    public boolean isApplicable(Game game) {
        return game != null && game.getIsActive(); // 仅对上架游戏生效
    }

    @Override
    public String getStrategyName() {
        return "PERCENTAGE_" + percent;
    }
}
