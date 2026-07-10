package com.english.controller;

import com.english.dto.ApiResult;
import com.english.dto.CreateOrderRequest;
import com.english.dto.CreateOrderTokenRequest;
import com.english.dto.CreateSeckillOrderRequest;
import com.english.dto.OrderTokenResponse;
import com.english.dto.PayOrderRequest;
import com.english.dto.SeckillOrderResultResponse;
import com.english.dto.SeckillOrderSubmitResponse;
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

    @Operation(summary = "申请下单幂等性 token", description = "为一次下单意图签发 token，前端创建订单时需要带回该 token。")
    @PostMapping("/order-tokens")
    public ApiResult<OrderTokenResponse> createOrderToken(@RequestBody CreateOrderTokenRequest request) {
        try {
            return ApiResult.success(shopService.createOrderToken(request.getUserId(), request.getProductId()));
        } catch (RuntimeException e) {
            return ApiResult.error(400, e.getMessage());
        }
    }

    @Operation(summary = "创建订单", description = "创建待支付订单并扣减库存。requestId 为后端签发的下单幂等性 token。")
    @PostMapping("/orders")
    public ApiResult<ShopOrder> createOrder(@RequestBody CreateOrderRequest request) {
        try {
            /*
             * 下单接口只创建“待支付”订单。
             * 支付动作单独走 /orders/{orderId}/pay，便于演示订单超时取消流程。
             */
            return ApiResult.success("订单已创建，请在有效期内支付", shopService.createOrder(request.getUserId(), request.getProductId(), request.getRequestId()));
        } catch (RuntimeException e) {
            return ApiResult.error(400, e.getMessage());
        }
    }

    @Operation(summary = "秒杀下单入队", description = "将下单请求写入 RabbitMQ 队列，立即返回排队状态，前端再轮询订单结果。")
    @PostMapping("/seckill-orders")
    public ApiResult<SeckillOrderSubmitResponse> submitSeckillOrder(@RequestBody CreateSeckillOrderRequest request) {
        try {
            return ApiResult.success("订单已进入排队", shopService.submitSeckillOrder(
                    request.getUserId(), request.getProductId(), request.getRequestId()));
        } catch (RuntimeException e) {
            return ApiResult.error(400, e.getMessage());
        }
    }

    @Operation(summary = "查询秒杀订单结果", description = "根据用户 ID 和 requestId 查询秒杀订单是否已创建成功。")
    @GetMapping("/orders/result")
    public ApiResult<SeckillOrderResultResponse> getSeckillOrderResult(
            @Parameter(description = "用户 ID", required = true)
            @RequestParam Long userId,
            @Parameter(description = "下单 token / requestId", required = true)
            @RequestParam String requestId) {
        try {
            return ApiResult.success(shopService.getSeckillOrderResult(userId, requestId));
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
