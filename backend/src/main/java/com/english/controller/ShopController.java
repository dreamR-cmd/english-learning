package com.english.controller;

import com.english.dto.ApiResult;
import com.english.dto.CreateOrderRequest;
import com.english.dto.PayOrderRequest;
import com.english.entity.ShopOrder;
import com.english.entity.ShopProduct;
import com.english.service.ShopService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shop")
public class ShopController {
    private final ShopService shopService;

    public ShopController(ShopService shopService) {
        this.shopService = shopService;
    }

    @GetMapping("/products")
    public ApiResult<List<ShopProduct>> getProducts() {
        // 商品页使用：返回商品基础信息和当前可售库存。
        return ApiResult.success(shopService.getProducts());
    }

    @PostMapping("/orders")
    public ApiResult<ShopOrder> createOrder(@RequestBody CreateOrderRequest request) {
        try {
            /*
             * 下单接口只创建“待支付”订单。
             * 支付动作单独走 /orders/{orderId}/pay，便于演示订单超时取消流程。
             */
            return ApiResult.success("订单已创建，请在 30 分钟内支付", shopService.createOrder(request.getUserId(), request.getProductId()));
        } catch (RuntimeException e) {
            return ApiResult.error(400, e.getMessage());
        }
    }

    @GetMapping("/orders")
    public ApiResult<List<ShopOrder>> getOrders(@RequestParam Long userId,
                                                @RequestParam(required = false, defaultValue = "all") String status) {
        try {
            // status=all 查询全部；status=pending/paid 查询指定状态。
            return ApiResult.success(shopService.getOrders(userId, status));
        } catch (RuntimeException e) {
            return ApiResult.error(400, e.getMessage());
        }
    }

    @PostMapping("/orders/{orderId}/pay")
    public ApiResult<ShopOrder> payOrder(@PathVariable Long orderId, @RequestBody PayOrderRequest request) {
        try {
            // 当前是模拟支付：把 pending 订单改为 paid；真实支付可在这里接第三方支付回调。
            return ApiResult.success("支付成功", shopService.payOrder(request.getUserId(), orderId));
        } catch (RuntimeException e) {
            return ApiResult.error(400, e.getMessage());
        }
    }
}
