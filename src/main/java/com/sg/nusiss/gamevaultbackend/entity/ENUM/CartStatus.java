package com.sg.nusiss.gamevaultbackend.entity.ENUM;

/**
 * 购物车状态枚举
 */
public enum CartStatus {
    ACTIVE,        // 使用中
    CHECKED_OUT,   // 已结算生成订单
    CANCELLED      // 已取消
}
