package com.english.dto;

import com.english.entity.ShopOrder;

public class SeckillOrderResultResponse {
    private String status;
    private String message;
    private ShopOrder order;

    public SeckillOrderResultResponse() {}

    public SeckillOrderResultResponse(String status, String message, ShopOrder order) {
        this.status = status;
        this.message = message;
        this.order = order;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public ShopOrder getOrder() { return order; }
    public void setOrder(ShopOrder order) { this.order = order; }
}
