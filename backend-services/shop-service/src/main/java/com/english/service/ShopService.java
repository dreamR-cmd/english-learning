package com.english.service;

import com.english.entity.ShopOrder;
import com.english.entity.ShopProduct;
import com.english.dto.OrderTokenResponse;
import com.english.dto.SeckillOrderResultResponse;
import com.english.dto.SeckillOrderSubmitResponse;

import java.util.List;

public interface ShopService {
    List<ShopProduct> getProducts();
    OrderTokenResponse createOrderToken(Long userId, Long productId);
    ShopOrder createOrder(Long userId, Long productId, String requestId);
    SeckillOrderSubmitResponse submitSeckillOrder(Long userId, Long productId, String requestId);
    void consumeSeckillOrder(Long userId, Long productId, String requestId);
    SeckillOrderResultResponse getSeckillOrderResult(Long userId, String requestId);
    List<ShopOrder> getOrders(Long userId, String status);
    ShopOrder payOrder(Long userId, Long orderId);
    void cancelExpiredOrder(Long orderId);
}
