package com.english.mapper;

import com.english.entity.AdminPermission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AdminPermissionMapper extends JpaRepository<AdminPermission, Long> {
    Optional<AdminPermission> findByCode(String code);
    List<AdminPermission> findAllByOrderBySortOrderAscIdAsc();
}
