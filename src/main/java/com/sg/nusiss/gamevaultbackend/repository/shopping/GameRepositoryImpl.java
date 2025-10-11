package com.sg.nusiss.gamevaultbackend.repository.shopping;

import com.sg.nusiss.gamevaultbackend.entity.shopping.Game;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class GameRepositoryImpl implements GameRepositoryCustom {

    @PersistenceContext
    private EntityManager em;

    @Override
    public List<Game> findTopDiscountedGames(int limit) {
        return em.createQuery(
                        "SELECT g FROM Game g " +
                                "WHERE g.discountPrice IS NOT NULL " +
                                "AND g.discountPrice < g.price " +
                                "ORDER BY (g.price - g.discountPrice) DESC",
                        Game.class
                )
                .setMaxResults(limit)
                .getResultList();
    }
}
