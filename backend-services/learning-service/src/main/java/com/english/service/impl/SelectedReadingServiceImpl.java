package com.english.service.impl;

import com.english.dto.SelectedReadingItem;
import com.english.entity.SelectedReading;
import com.english.entity.UserSelectedReadingFavorite;
import com.english.mapper.SelectedReadingMapper;
import com.english.mapper.UserSelectedReadingFavoriteMapper;
import com.english.service.SelectedReadingService;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class SelectedReadingServiceImpl implements SelectedReadingService {
    private final SelectedReadingMapper selectedReadingMapper;
    private final UserSelectedReadingFavoriteMapper favoriteMapper;

    public SelectedReadingServiceImpl(SelectedReadingMapper selectedReadingMapper,
                                      UserSelectedReadingFavoriteMapper favoriteMapper) {
        this.selectedReadingMapper = selectedReadingMapper;
        this.favoriteMapper = favoriteMapper;
    }

    @Override
    public List<SelectedReadingItem> getSelectedReadings(Long userId) {
        List<SelectedReading> readings = selectedReadingMapper.findAllByOrderBySortOrderAscIdAsc();
        Set<Long> favoriteIds = new HashSet<>();
        if (userId != null) {
            for (UserSelectedReadingFavorite favorite : favoriteMapper.findByUserIdOrderByCreatedAtDesc(userId)) {
                favoriteIds.add(favorite.getSelectedReadingId());
            }
        }

        return readings.stream()
                .map(reading -> SelectedReadingItem.from(reading, favoriteIds.contains(reading.getId())))
                .toList();
    }

    @Override
    public UserSelectedReadingFavorite addFavorite(Long userId, Long selectedReadingId) {
        if (favoriteMapper.findByUserIdAndSelectedReadingId(userId, selectedReadingId).isPresent()) {
            return null;
        }

        UserSelectedReadingFavorite favorite = new UserSelectedReadingFavorite();
        favorite.setUserId(userId);
        favorite.setSelectedReadingId(selectedReadingId);
        return favoriteMapper.save(favorite);
    }

    @Override
    public void removeFavorite(Long userId, Long selectedReadingId) {
        favoriteMapper.deleteByUserIdAndSelectedReadingId(userId, selectedReadingId);
    }

    @Override
    public List<UserSelectedReadingFavorite> getFavorites(Long userId) {
        return favoriteMapper.findByUserIdOrderByCreatedAtDesc(userId);
    }
}
