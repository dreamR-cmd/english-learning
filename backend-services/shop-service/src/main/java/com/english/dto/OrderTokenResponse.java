package com.english.dto;

public class OrderTokenResponse {
    private String token;
    private long expireSeconds;

    public OrderTokenResponse(String token, long expireSeconds) {
        this.token = token;
        this.expireSeconds = expireSeconds;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public long getExpireSeconds() { return expireSeconds; }
    public void setExpireSeconds(long expireSeconds) { this.expireSeconds = expireSeconds; }
}
