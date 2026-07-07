package com.english.mapper;

import com.english.entity.AdminRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdminRoleMapper extends JpaRepository<AdminRole, Long> {
    Optional<AdminRole> findByCode(String code);
}
