package com.english.mapper;

import com.english.entity.UserDailyWordAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface UserDailyWordAssignmentMapper extends JpaRepository<UserDailyWordAssignment, Long> {
    List<UserDailyWordAssignment> findByUserIdAndPracticeDateOrderByIdAsc(Long userId, LocalDate practiceDate);

    void deleteByIdIn(List<Long> ids);
}
