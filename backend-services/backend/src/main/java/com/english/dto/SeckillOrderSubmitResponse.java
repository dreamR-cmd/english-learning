package com.english.dto;

public class SeckillOrderSubmitResponse {
    private String status;
    private String requestId;
    private Long orderId;
    private String message;

    public SeckillOrderSubmitResponse() {}

    public SeckillOrderSubmitResponse(String status, String requestId, Long orderId, String message) {
        this.status = status;
        this.requestId = requestId;
        this.orderId = orderId;
        this.message = message;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
