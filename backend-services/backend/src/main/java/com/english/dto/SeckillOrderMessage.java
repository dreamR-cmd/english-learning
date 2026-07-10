package com.english.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

public class SeckillOrderMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long userId;
    private Long productId;
    private String requestId;
    private LocalDateTime createdAt;

    public SeckillOrderMessage() {}

    public SeckillOrderMessage(Long userId, Long productId, String requestId) {
        this.userId = userId;
        this.productId = productId;
        this.requestId = requestId;
        this.createdAt = LocalDateTime.now();
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
