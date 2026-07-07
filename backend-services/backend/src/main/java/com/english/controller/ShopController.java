package com.english.controller;

import com.english.dto.ApiResult;
import com.english.dto.CreateOrderRequest;
import com.english.dto.PayOrderRequest;
import com.english.entity.ShopOrder;
import com.english.entity.ShopProduct;
import com.english.service.ShopService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "商城接口", description = "课程商品、订单创建、订单查询和模拟支付接口")
@RestController
@RequestMapping("/api/shop")
public class ShopController {
    private final ShopService shopService;

    public ShopController(ShopService shopService) {
        this.shopService = shopService;
    }

    @Operation(summary = "查询商品列表", description = "返回商城商品基础信息、价格、标签和当前可售库存。")
    @GetMapping("/products")
    public ApiResult<List<ShopProduct>> getProducts() {
        // 商品页使用：返回商品基础信息和当前可售库存。
        return ApiResult.success(shopService.getProducts());
    }

    @Operation(summary = "创建订单", description = "创建待支付订单并扣减库存。请求体字段：userId 用户 ID，productId 商品 ID。")
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

    @Operation(summary = "查询用户订单", description = "查询指定用户的订单列表；status=all 查询全部，status=pending 查询待支付，status=paid 查询已支付。")
    @GetMapping("/orders")
    public ApiResult<List<ShopOrder>> getOrders(
            @Parameter(description = "用户 ID", required = true)
            @RequestParam Long userId,
            @Parameter(description = "订单状态：all、pending、paid")
            @RequestParam(required = false, defaultValue = "all") String status) {
        try {
            // status=all 查询全部；status=pending/paid 查询指定状态。
            return ApiResult.success(shopService.getOrders(userId, status));
        } catch (RuntimeException e) {
            return ApiResult.error(400, e.getMessage());
        }
    }

    @Operation(summary = "支付订单", description = "模拟支付指定订单，将待支付订单更新为已支付。请求体字段：userId 用户 ID。")
    @PostMapping("/orders/{orderId}/pay")
    public ApiResult<ShopOrder> payOrder(
            @Parameter(description = "订单 ID", required = true)
            @PathVariable Long orderId,
            @RequestBody PayOrderRequest request) {
        try {
            // 当前是模拟支付：把 pending 订单改为 paid；真实支付可在这里接第三方支付回调。
            return ApiResult.success("支付成功", shopService.payOrder(request.getUserId(), orderId));
        } catch (RuntimeException e) {
            return ApiResult.error(400, e.getMessage());
        }
    }
}
