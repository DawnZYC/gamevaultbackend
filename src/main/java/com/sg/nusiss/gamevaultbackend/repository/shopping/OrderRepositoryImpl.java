package com.sg.nusiss.gamevaultbackend.repository.shopping;

import com.sg.nusiss.gamevaultbackend.entity.shopping.Order;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class OrderRepositoryImpl implements OrderRepositoryCustom {

    @PersistenceContext
    private EntityManager em;

    @Override
    public List<Order> findOrdersWithItems(Long userId) {
        return em.createQuery(
                "SELECT DISTINCT o FROM Order o JOIN FETCH o.orderItems WHERE o.userId = :uid",
                Order.class
        ).setParameter("uid", userId).getResultList();
    }
}
