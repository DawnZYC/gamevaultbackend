package com.sg.nusiss.gamevaultbackend.repository;

import com.sg.nusiss.gamevaultbackend.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    List<OrderItem> findByUserIdOrderByOrderDateDesc(Long userId);
}




