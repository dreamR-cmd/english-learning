package com.english.mapper;

import com.english.entity.UserFavorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

public interface UserFavoriteMapper extends JpaRepository<UserFavorite, Long> {
    List<UserFavorite> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<UserFavorite> findByUserIdAndReadingId(Long userId, Long readingId);

    @Transactional
    void deleteByUserIdAndReadingId(Long userId, Long readingId);
}
