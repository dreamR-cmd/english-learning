package com.english.service;

import com.english.entity.ShopOrder;
import com.english.entity.ShopProduct;

import java.util.List;

public interface ShopService {
    List<ShopProduct> getProducts();
    ShopOrder createOrder(Long userId, Long productId);
    List<ShopOrder> getOrders(Long userId, String status);
    ShopOrder payOrder(Long userId, Long orderId);
    void cancelExpiredOrder(Long orderId);
}
