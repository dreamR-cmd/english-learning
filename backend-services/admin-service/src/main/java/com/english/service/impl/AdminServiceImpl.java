package com.english.service.impl;

import com.english.config.AdminAuthContext;
import com.english.entity.AdminOperationLog;
import com.english.entity.AdminPermission;
import com.english.entity.AdminPermissionChangeLog;
import com.english.entity.AdminRole;
import com.english.entity.AdminRolePermission;
import com.english.entity.ExamModule;
import com.english.entity.ShopOrder;
import com.english.entity.User;
import com.english.mapper.AdminOperationLogMapper;
import com.english.mapper.AdminPermissionChangeLogMapper;
import com.english.mapper.AdminPermissionMapper;
import com.english.mapper.AdminRoleMapper;
import com.english.mapper.AdminRolePermissionMapper;
import com.english.mapper.ExamModuleMapper;
import com.english.mapper.ShopOrderMapper;
import com.english.mapper.UserMapper;
import com.english.service.AdminService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class AdminServiceImpl implements AdminService {
    private static final String CONFIRM_TEXT = "CONFIRM";
    private static final Set<String> BUILTIN_ROLE_CODES = Set.of("ADMIN", "USER");
    private static final Set<String> BUILTIN_MODULE_CODES = Set.of(
            "shop", "selected-readings", "cet4", "cet6", "toefl", "ielts", "kaoyan", "gre"
    );

    private final ShopOrderMapper orderMapper;
    private final ExamModuleMapper moduleMapper;
    private final UserMapper userMapper;
    private final AdminRoleMapper roleMapper;
    private final AdminPermissionMapper permissionMapper;
    private final AdminRolePermissionMapper rolePermissionMapper;
    private final AdminOperationLogMapper operationLogMapper;
    private final AdminPermissionChangeLogMapper permissionChangeLogMapper;
    private final ObjectMapper objectMapper;

    public AdminServiceImpl(ShopOrderMapper orderMapper,
                            ExamModuleMapper moduleMapper,
                            UserMapper userMapper,
                            AdminRoleMapper roleMapper,
                            AdminPermissionMapper permissionMapper,
                            AdminRolePermissionMapper rolePermissionMapper,
                            AdminOperationLogMapper operationLogMapper,
                            AdminPermissionChangeLogMapper permissionChangeLogMapper,
                            ObjectMapper objectMapper) {
        this.orderMapper = orderMapper;
        this.moduleMapper = moduleMapper;
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.permissionMapper = permissionMapper;
        this.rolePermissionMapper = rolePermissionMapper;
        this.operationLogMapper = operationLogMapper;
        this.permissionChangeLogMapper = permissionChangeLogMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<ShopOrder> getOrders() {
        return orderMapper.findAllByOrderByCreatedAtDesc();
    }

    @Override
    public ShopOrder updateOrderStatus(Long orderId, String status, String confirmText) {
        requireConfirm(confirmText);
        ShopOrder order = orderMapper.findById(orderId)
                .orElseThrow(() -> new RuntimeException("订单不存在"));
        order.setStatus(status);
        if (ShopOrder.STATUS_PAID.equals(status) && order.getPaidAt() == null) {
            order.setPaidAt(LocalDateTime.now());
        }
        if (ShopOrder.STATUS_CANCELED.equals(status) && order.getCanceledAt() == null) {
            order.setCanceledAt(LocalDateTime.now());
        }
        return orderMapper.save(order);
    }

    @Override
    public List<ExamModule> getModules() {
        return moduleMapper.findAllByOrderBySortOrderAscIdAsc();
    }

    @Override
    public ExamModule saveModule(ExamModule module) {
        if (module.getId() != null) {
            ExamModule existing = moduleMapper.findById(module.getId())
                    .orElseThrow(() -> new RuntimeException("模块不存在"));
            existing.setName(module.getName());
            existing.setDescription(module.getDescription());
            existing.setIcon(module.getIcon());
            existing.setRoutePath(module.getRoutePath());
            existing.setSortOrder(module.getSortOrder());
            if (!Boolean.TRUE.equals(existing.getSystemBuiltin())) {
                existing.setCode(module.getCode());
            }
            return moduleMapper.save(existing);
        }
        module.setSystemBuiltin(false);
        return moduleMapper.save(module);
    }

    @Override
    public void deleteModule(Long moduleId, String confirmText) {
        requireConfirm(confirmText);
        ExamModule module = moduleMapper.findById(moduleId)
                .orElseThrow(() -> new RuntimeException("模块不存在"));
        if (Boolean.TRUE.equals(module.getSystemBuiltin()) || BUILTIN_MODULE_CODES.contains(module.getCode())) {
            throw new RuntimeException("系统内置模块不允许删除");
        }
        moduleMapper.deleteById(moduleId);
    }

    @Override
    public List<User> getUsers() {
        List<User> users = userMapper.findAll();
        users.forEach(user -> user.setPassword(null));
        return users;
    }

    @Override
    public User updateUserRole(Long userId, Long roleId, String confirmText) {
        requireConfirm(confirmText);
        roleMapper.findById(roleId).orElseThrow(() -> new RuntimeException("角色不存在"));
        User user = userMapper.findById(userId).orElseThrow(() -> new RuntimeException("用户不存在"));
        user.setRoleId(roleId);
        User saved = userMapper.save(user);
        saved.setPassword(null);
        return saved;
    }

    @Override
    public void deleteUser(Long userId, String confirmText) {
        requireConfirm(confirmText);
        User user = userMapper.findById(userId).orElseThrow(() -> new RuntimeException("用户不存在"));
        user.setEnabled(false);
        userMapper.save(user);
    }

    @Override
    public List<AdminRole> getRoles() {
        return roleMapper.findAll();
    }

    @Override
    public AdminRole saveRole(AdminRole role) {
        if (role.getId() != null) {
            AdminRole existing = roleMapper.findById(role.getId())
                    .orElseThrow(() -> new RuntimeException("角色不存在"));
            existing.setName(role.getName());
            existing.setDescription(role.getDescription());
            if (!Boolean.TRUE.equals(existing.getSystemBuiltin())) {
                existing.setCode(role.getCode());
            }
            return roleMapper.save(existing);
        }
        role.setSystemBuiltin(false);
        return roleMapper.save(role);
    }

    @Override
    public void deleteRole(Long roleId, String confirmText) {
        requireConfirm(confirmText);
        AdminRole role = roleMapper.findById(roleId)
                .orElseThrow(() -> new RuntimeException("角色不存在"));
        if (Boolean.TRUE.equals(role.getSystemBuiltin()) || BUILTIN_ROLE_CODES.contains(role.getCode())) {
            throw new RuntimeException("系统内置角色不允许删除");
        }
        if (userMapper.countByRoleId(roleId) > 0) {
            throw new RuntimeException("该角色仍有用户绑定，不能删除");
        }
        rolePermissionMapper.deleteByRoleId(roleId);
        roleMapper.deleteById(roleId);
    }

    @Override
    public List<AdminPermission> getPermissions() {
        return permissionMapper.findAllByOrderBySortOrderAscIdAsc();
    }

    @Override
    public List<AdminPermission> getRolePermissions(Long roleId) {
        List<Long> permissionIds = rolePermissionMapper.findByRoleId(roleId).stream()
                .map(AdminRolePermission::getPermissionId)
                .toList();
        if (permissionIds.isEmpty()) {
            return List.of();
        }
        return permissionMapper.findAllById(permissionIds);
    }

    @Override
    @Transactional
    public void assignRolePermissions(Long roleId, List<Long> permissionIds, String confirmText) {
        requireConfirm(confirmText);
        AdminRole role = roleMapper.findById(roleId).orElseThrow(() -> new RuntimeException("角色不存在"));
        List<Long> beforeIds = rolePermissionMapper.findByRoleId(roleId).stream()
                .map(AdminRolePermission::getPermissionId)
                .sorted()
                .toList();
        Set<Long> uniqueIds = new HashSet<>(permissionIds);
        List<Long> afterIds = uniqueIds.stream().sorted().toList();

        rolePermissionMapper.deleteByRoleId(roleId);
        for (Long permissionId : uniqueIds) {
            permissionMapper.findById(permissionId).orElseThrow(() -> new RuntimeException("权限不存在"));
            AdminRolePermission relation = new AdminRolePermission();
            relation.setRoleId(roleId);
            relation.setPermissionId(permissionId);
            rolePermissionMapper.save(relation);
        }
        savePermissionChangeLog(role, beforeIds, afterIds);
    }

    @Override
    public List<AdminOperationLog> getOperationLogs() {
        return operationLogMapper.findTop200ByOrderByCreatedAtDesc();
    }

    @Override
    public List<AdminPermissionChangeLog> getPermissionChangeLogs() {
        return permissionChangeLogMapper.findTop200ByOrderByCreatedAtDesc();
    }

    private void requireConfirm(String confirmText) {
        if (!CONFIRM_TEXT.equals(confirmText)) {
            throw new RuntimeException("敏感操作需要二次确认");
        }
    }

    private void savePermissionChangeLog(AdminRole role, List<Long> beforeIds, List<Long> afterIds) {
        try {
            Set<Long> before = new HashSet<>(beforeIds);
            Set<Long> after = new HashSet<>(afterIds);
            List<Long> added = new ArrayList<>(after);
            added.removeAll(before);
            Collections.sort(added);
            List<Long> removed = new ArrayList<>(before);
            removed.removeAll(after);
            Collections.sort(removed);

            User admin = AdminAuthContext.get();
            AdminPermissionChangeLog log = new AdminPermissionChangeLog();
            log.setAdminId(admin == null ? null : admin.getId());
            log.setAdminUsername(admin == null ? null : admin.getUsername());
            log.setRoleId(role.getId());
            log.setRoleCode(role.getCode());
            log.setBeforePermissionIds(objectMapper.writeValueAsString(beforeIds));
            log.setAfterPermissionIds(objectMapper.writeValueAsString(afterIds));
            log.setAddedPermissionIds(objectMapper.writeValueAsString(added));
            log.setRemovedPermissionIds(objectMapper.writeValueAsString(removed));
            log.setCreatedAt(LocalDateTime.now());
            permissionChangeLogMapper.save(log);
        } catch (Exception ignored) {
            // Permission-change audit must not block the permission update itself.
        }
    }
}
