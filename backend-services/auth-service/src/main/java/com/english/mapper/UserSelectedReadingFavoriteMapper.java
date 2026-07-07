package com.english.mapper;

import com.english.entity.UserSelectedReadingFavorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface UserSelectedReadingFavoriteMapper extends JpaRepository<UserSelectedReadingFavorite, Long> {
    List<UserSelectedReadingFavorite> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<UserSelectedReadingFavorite> findByUserIdAndSelectedReadingId(Long userId, Long selectedReadingId);
    boolean existsByUserIdAndSelectedReadingId(Long userId, Long selectedReadingId);

    @Transactional
    void deleteByUserIdAndSelectedReadingId(Long userId, Long selectedReadingId);
}
