package com.english.mapper;

import com.english.entity.ShopOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ShopOrderMapper extends JpaRepository<ShopOrder, Long> {
    List<ShopOrder> findAllByOrderByCreatedAtDesc();

    List<ShopOrder> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<ShopOrder> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, String status);

    Optional<ShopOrder> findByIdAndUserId(Long id, Long userId);

    Optional<ShopOrder> findByUserIdAndRequestId(Long userId, String requestId);
}
