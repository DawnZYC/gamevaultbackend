package com.sg.nusiss.gamevaultbackend.repository.library;

import com.sg.nusiss.gamevaultbackend.entity.shopping.OrderItem;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public class OrderItemRepositoryImpl implements OrderItemRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<OrderItem> findRecentItemsByUser(Long userId, int limit) {
        return entityManager.createQuery(
                        "SELECT oi FROM OrderItem oi WHERE oi.userId = :userId ORDER BY oi.orderDate DESC",
                        OrderItem.class)
                .setParameter("userId", userId)
                .setMaxResults(limit)
                .getResultList();
    }
}
