package com.english.controller;

import com.english.dto.ApiResult;
import com.english.entity.ShopOrder;
import com.english.service.ShopService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Tag(name = "后台商城管理接口", description = "后台订单管理接口")
@RestController
@RequestMapping("/api/admin/shop")
public class AdminShopController {
    private final ShopService shopService;

    public AdminShopController(ShopService shopService) {
        this.shopService = shopService;
    }

    @Operation(summary = "查询全部订单", description = "后台查询所有用户订单。")
    @GetMapping("/orders")
    public ApiResult<List<ShopOrder>> getOrders() {
        return ApiResult.success(shopService.getAdminOrders());
    }

    @Operation(summary = "更新订单状态", description = "后台更新订单状态，status 可用 pending、paid、canceled。")
    @PutMapping("/orders/{orderId}/status")
    public ApiResult<ShopOrder> updateOrderStatus(@PathVariable Long orderId, @RequestBody Map<String, String> body) {
        return ApiResult.success(shopService.updateAdminOrderStatus(orderId, body.get("status")));
    }
}
