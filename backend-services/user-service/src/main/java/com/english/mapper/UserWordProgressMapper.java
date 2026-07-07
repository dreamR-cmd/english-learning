package com.english.mapper;

import com.english.entity.UserWordProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserWordProgressMapper extends JpaRepository<UserWordProgress, Long> {
    Optional<UserWordProgress> findByUserIdAndWordId(Long userId, Long wordId);

    List<UserWordProgress> findByUserIdAndModuleCode(Long userId, String moduleCode);

    List<UserWordProgress> findByUserIdAndWordIdIn(Long userId, List<Long> wordIds);

    List<UserWordProgress> findByUserIdAndReviewReadyTrue(Long userId);

    List<UserWordProgress> findByUserIdAndReviewReadyTrueOrderByUpdatedAtDesc(Long userId);
}
