package com.sg.nusiss.gamevaultbackend.repository.shopping;

import com.sg.nusiss.gamevaultbackend.entity.shopping.Cart;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public class CartRepositoryImpl implements CartRepositoryCustom {

    @PersistenceContext
    private EntityManager em;

    @Override
    public List<Cart> findActiveCartsWithItems(Long userId) {
        return em.createQuery(
                        "select c from Cart c join fetch c.cartItems " +
                                "where c.userId = :uid and c.status = 'ACTIVE'", Cart.class)
                .setParameter("uid", userId)
                .getResultList();
    }

    @Override
    public BigDecimal sumTotalByUserId(Long userId) {
        BigDecimal sum = em.createQuery(
                        "select coalesce(sum(ci.price * ci.quantity), 0) " +
                                "from Cart c join c.cartItems ci " +
                                "where c.userId = :uid and c.status = 'ACTIVE'", BigDecimal.class)
                .setParameter("uid", userId)
                .getSingleResult();
        return sum;
    }
}
