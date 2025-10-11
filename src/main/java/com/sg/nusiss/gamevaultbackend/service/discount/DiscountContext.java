package com.sg.nusiss.gamevaultbackend.service.discount;

import com.sg.nusiss.gamevaultbackend.entity.shopping.Game;
import java.math.BigDecimal;

public class DiscountContext {

    private IDiscountStrategy strategy;

    public DiscountContext(IDiscountStrategy strategy) {
        this.strategy = strategy;
    }

    public void setStrategy(IDiscountStrategy strategy) {
        this.strategy = strategy;
    }

    /**
     * 应用当前折扣策略
     */
    public BigDecimal applyDiscount(Game game, BigDecimal originalPrice) {
        if (strategy == null || !strategy.isApplicable(game)) {
            return BigDecimal.ZERO;
        }
        return strategy.calculateDiscount(game, originalPrice);
    }

    public String getStrategyName() {
        return strategy != null ? strategy.getStrategyName() : "NO_STRATEGY";
    }
}
