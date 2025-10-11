package com.sg.nusiss.gamevaultbackend.repository.library;

import com.sg.nusiss.gamevaultbackend.entity.shopping.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long>, OrderItemRepositoryCustom {

    /** 按用户查询所有订单项 */
    List<OrderItem> findByUserId(Long userId);

    /** 按用户查询最近订单项，按日期降序排列 */
    List<OrderItem> findByUserIdOrderByOrderDateDesc(Long userId);

    /** 按订单ID查询订单项列表 */
    List<OrderItem> findByOrder_OrderId(Long orderId);
}

/** 自定义扩展接口，用于实现复杂逻辑（如限制条数） */
interface OrderItemRepositoryCustom {
    List<OrderItem> findRecentItemsByUser(Long userId, int limit);
}
