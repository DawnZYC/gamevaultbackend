package com.sg.nusiss.gamevaultbackend.repository.shopping;

import com.sg.nusiss.gamevaultbackend.entity.shopping.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
}
