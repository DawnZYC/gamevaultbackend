package com.sg.nusiss.gamevaultbackend.repository;

import com.sg.nusiss.gamevaultbackend.entity.PurchasedGameActivationCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PurchasedGameActivationCodeRepository extends JpaRepository<PurchasedGameActivationCode, Long> {
    List<PurchasedGameActivationCode> findByUserId(Long userId);
    List<PurchasedGameActivationCode> findByOrderItemId(Long orderItemId);
}




