package com.english.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "user_word_progress",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_word_progress_user_word", columnNames = {"user_id", "word_id"})
        }
)
public class UserWordProgress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "word_id", nullable = false)
    private Long wordId;

    @Column(name = "module_code")
    private String moduleCode;

    @Column(name = "known_count", nullable = false)
    private Integer knownCount;

    @Column(name = "review_ready", nullable = false)
    private Boolean reviewReady;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "word_id", insertable = false, updatable = false)
    private Word word;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.knownCount == null) {
            this.knownCount = 0;
        }
        if (this.reviewReady == null) {
            this.reviewReady = false;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public UserWordProgress() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getWordId() { return wordId; }
    public void setWordId(Long wordId) { this.wordId = wordId; }
    public String getModuleCode() { return moduleCode; }
    public void setModuleCode(String moduleCode) { this.moduleCode = moduleCode; }
    public Integer getKnownCount() { return knownCount; }
    public void setKnownCount(Integer knownCount) { this.knownCount = knownCount; }
    public Boolean getReviewReady() { return reviewReady; }
    public void setReviewReady(Boolean reviewReady) { this.reviewReady = reviewReady; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public Word getWord() { return word; }
    public void setWord(Word word) { this.word = word; }
}
