package com.english.mapper;

import com.english.entity.AdminPermissionChangeLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdminPermissionChangeLogMapper extends JpaRepository<AdminPermissionChangeLog, Long> {
    List<AdminPermissionChangeLog> findTop200ByOrderByCreatedAtDesc();
}
