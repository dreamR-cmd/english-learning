package com.english.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "shop_orders", indexes = {
        // 用户订单页会频繁按 userId + status 查询，建立组合索引能减少全表扫描。
        @Index(name = "idx_shop_order_user_status", columnList = "user_id,status"),
        // 订单号对外展示，必须唯一。
        @Index(name = "idx_shop_order_order_no", columnList = "order_no", unique = true)
})
public class ShopOrder {
    // 待支付：已占库存，等待用户支付，30 分钟超时后会取消并回补库存。
    public static final String STATUS_PENDING = "pending";
    // 已支付：订单完成，不再参与超时取消。
    public static final String STATUS_PAID = "paid";
    // 已取消：超时未支付或后续扩展的主动取消，库存已回补。
    public static final String STATUS_CANCELED = "canceled";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_no", nullable = false, unique = true)
    private String orderNo;
    // 当前项目没有复杂的用户订单关联查询，这里直接保存用户 id。
    @Column(name = "user_id", nullable = false)
    private Long userId;
    // 冗余商品快照字段：即使商品后续改名/改价，历史订单仍保留下单时的信息。
    @Column(name = "product_id", nullable = false)
    private Long productId;
    @Column(name = "product_name", nullable = false)
    private String productName;
    private String icon;
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;
    @Column(nullable = false)
    private String status;
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    @Column(name = "paid_at")
    private LocalDateTime paidAt;
    @Column(name = "expire_at")
    private LocalDateTime expireAt;
    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;

    @PrePersist
    public void prePersist() {
        // JPA 新增记录前自动填默认值，避免业务层遗漏 createdAt/status。
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (status == null) status = STATUS_PENDING;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
    public LocalDateTime getExpireAt() { return expireAt; }
    public void setExpireAt(LocalDateTime expireAt) { this.expireAt = expireAt; }
    public LocalDateTime getCanceledAt() { return canceledAt; }
    public void setCanceledAt(LocalDateTime canceledAt) { this.canceledAt = canceledAt; }
}
