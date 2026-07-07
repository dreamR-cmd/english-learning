package com.english.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "shop_products")
public class ShopProduct {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;
    private String category;
    // 商品详情文案可能较长，显式放大列长度，避免默认 varchar(255) 不够。
    @Column(length = 1000)
    private String description;
    // 金额使用 BigDecimal，避免 double/float 的二进制精度问题。
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;
    @Column(name = "original_price", precision = 10, scale = 2)
    private BigDecimal originalPrice;
    private String icon;
    private String tag;
    private String tone;
    // 多个卖点用 “|” 分隔，前端按分隔符转成数组展示。
    @Column(length = 1000)
    private String points;
    // 数据库库存是最终库存来源；Redis 库存是高并发下的快速扣减缓存。
    @Column(nullable = false)
    private Integer stock;
    @Column(name = "sort_order")
    private Integer sortOrder;
    @Column(nullable = false)
    private Boolean active = true;
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        // 新增商品时补齐通用默认值，降低初始化代码的重复度。
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (active == null) active = true;
        if (stock == null) stock = 0;
        if (sortOrder == null) sortOrder = 0;
    }

    @PreUpdate
    public void preUpdate() {
        // 更新商品信息或库存时自动维护更新时间。
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public BigDecimal getOriginalPrice() { return originalPrice; }
    public void setOriginalPrice(BigDecimal originalPrice) { this.originalPrice = originalPrice; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag; }
    public String getTone() { return tone; }
    public void setTone(String tone) { this.tone = tone; }
    public String getPoints() { return points; }
    public void setPoints(String points) { this.points = points; }
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
