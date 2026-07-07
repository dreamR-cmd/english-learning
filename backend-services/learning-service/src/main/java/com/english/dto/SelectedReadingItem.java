package com.english.dto;

import com.english.entity.SelectedReading;

public class SelectedReadingItem {
    private Long id;
    private String title;
    private String source;
    private String level;
    private String type;
    private String suggestedFor;
    private String description;
    private String content;
    private String url;
    private Boolean favorited;

    public static SelectedReadingItem from(SelectedReading reading, boolean favorited) {
        SelectedReadingItem item = new SelectedReadingItem();
        item.setId(reading.getId());
        item.setTitle(reading.getTitle());
        item.setSource(reading.getSource());
        item.setLevel(reading.getLevel());
        item.setType(reading.getType());
        item.setSuggestedFor(reading.getSuggestedFor());
        item.setDescription(reading.getDescription());
        item.setContent(reading.getContent());
        item.setUrl(reading.getUrl());
        item.setFavorited(favorited);
        return item;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getSuggestedFor() { return suggestedFor; }
    public void setSuggestedFor(String suggestedFor) { this.suggestedFor = suggestedFor; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public Boolean getFavorited() { return favorited; }
    public void setFavorited(Boolean favorited) { this.favorited = favorited; }
}
