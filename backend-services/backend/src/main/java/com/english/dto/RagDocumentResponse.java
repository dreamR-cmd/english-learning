package com.english.dto;

import java.time.LocalDateTime;

public class RagDocumentResponse {
    private String id;
    private String title;
    private String source;
    private LocalDateTime createdAt;

    public RagDocumentResponse() {}

    public RagDocumentResponse(String id, String title, String source, LocalDateTime createdAt) {
        this.id = id;
        this.title = title;
        this.source = source;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
