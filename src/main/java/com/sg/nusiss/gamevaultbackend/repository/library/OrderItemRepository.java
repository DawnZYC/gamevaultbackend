package com.sg.nusiss.gamevaultbackend.repository.library;

import com.sg.nusiss.gamevaultbackend.entity.library.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    List<OrderItem> findByUserIdOrderByOrderDateDesc(Long userId);
}




