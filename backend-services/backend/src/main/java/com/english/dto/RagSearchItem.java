package com.english.dto;

public class RagSearchItem {
    private String documentId;
    private String title;
    private String source;
    private String snippet;
    private int score;

    public RagSearchItem() {}

    public RagSearchItem(String documentId, String title, String source, String snippet, int score) {
        this.documentId = documentId;
        this.title = title;
        this.source = source;
        this.snippet = snippet;
        this.score = score;
    }

    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getSnippet() { return snippet; }
    public void setSnippet(String snippet) { this.snippet = snippet; }
    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
}
