package com.english.mapper;

import com.english.entity.AdminRolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface AdminRolePermissionMapper extends JpaRepository<AdminRolePermission, Long> {
    List<AdminRolePermission> findByRoleId(Long roleId);
    List<AdminRolePermission> findByRoleIdIn(List<Long> roleIds);

    @Transactional
    void deleteByRoleId(Long roleId);
}
