// src/main/java/com/gamevault/storeservice/repository/CartRepository.java
package com.sg.nusiss.gamevaultbackend.repository.shopping;

import com.sg.nusiss.gamevaultbackend.entity.shopping.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long>, CartRepositoryCustom {
    Optional<Cart> findByUserId(Long userId);
}

/* 放在同一文件，包可见即可 */
interface CartRepositoryCustom {
    List<Cart> findActiveCartsWithItems(Long userId);
    BigDecimal sumTotalByUserId(Long userId);
}
