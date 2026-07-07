package com.english.mapper;

import com.english.entity.SelectedReading;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SelectedReadingMapper extends JpaRepository<SelectedReading, Long> {
    List<SelectedReading> findAllByOrderBySortOrderAscIdAsc();
    Optional<SelectedReading> findByTitle(String title);
}
