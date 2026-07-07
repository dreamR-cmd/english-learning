package com.english.mapper;

import com.english.entity.WrongRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WrongRecordMapper extends JpaRepository<WrongRecord, Long> {
    List<WrongRecord> findByUserIdOrderByCreatedAtDesc(Long userId);
    void deleteByIdAndUserId(Long id, Long userId);
}
