package com.sg.nusiss.gamevaultbackend.service.discount;

public class DiscountFactory {

    public static IDiscountStrategy createDiscount(String type) {
        if (type == null) return new NoDiscountStrategy();

        switch (type.toUpperCase()) {
            case "PERCENTAGE_10":
                return new PercentageDiscount(10);
            case "PERCENTAGE_20":
                return new PercentageDiscount(20);
            case "PERCENTAGE_50":
                return new PercentageDiscount(50);
            case "NO_DISCOUNT":
            default:
                return new NoDiscountStrategy();
        }
    }
}
