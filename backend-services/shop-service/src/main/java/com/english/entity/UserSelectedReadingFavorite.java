package com.english.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "user_selected_reading_favorites",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "selected_reading_id"})
)
public class UserSelectedReadingFavorite {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "selected_reading_id", nullable = false)
    private Long selectedReadingId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "selected_reading_id", insertable = false, updatable = false)
    private SelectedReading selectedReading;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public UserSelectedReadingFavorite() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getSelectedReadingId() { return selectedReadingId; }
    public void setSelectedReadingId(Long selectedReadingId) { this.selectedReadingId = selectedReadingId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public SelectedReading getSelectedReading() { return selectedReading; }
    public void setSelectedReading(SelectedReading selectedReading) { this.selectedReading = selectedReading; }
}
