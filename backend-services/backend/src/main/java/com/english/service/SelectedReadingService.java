package com.english.service;

import com.english.dto.SelectedReadingItem;
import com.english.entity.UserSelectedReadingFavorite;

import java.util.List;

public interface SelectedReadingService {
    List<SelectedReadingItem> getSelectedReadings(Long userId);
    UserSelectedReadingFavorite addFavorite(Long userId, Long selectedReadingId);
    void removeFavorite(Long userId, Long selectedReadingId);
    List<UserSelectedReadingFavorite> getFavorites(Long userId);
}
