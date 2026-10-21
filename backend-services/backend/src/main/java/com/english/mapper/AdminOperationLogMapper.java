package com.english.mapper;

import com.english.entity.AdminOperationLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdminOperationLogMapper extends JpaRepository<AdminOperationLog, Long> {
    List<AdminOperationLog> findTop200ByOrderByCreatedAtDesc();
}
