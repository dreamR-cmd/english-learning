package com.english.mapper;

import com.english.entity.ShopOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ShopOrderMapper extends JpaRepository<ShopOrder, Long> {
    // “全部订单”列表。
    List<ShopOrder> findByUserIdOrderByCreatedAtDesc(Long userId);

    // “待支付 / 已支付”等状态筛选。
    List<ShopOrder> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, String status);

    // 支付时必须同时校验订单归属，避免用户支付或查看别人的订单。
    Optional<ShopOrder> findByIdAndUserId(Long id, Long userId);
}
