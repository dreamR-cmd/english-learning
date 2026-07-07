package com.english.service.impl;

import com.english.entity.AdminPermission;
import com.english.entity.AdminRole;
import com.english.entity.AdminRolePermission;
import com.english.entity.ExamModule;
import com.english.entity.ShopOrder;
import com.english.entity.User;
import com.english.mapper.AdminPermissionMapper;
import com.english.mapper.AdminRoleMapper;
import com.english.mapper.AdminRolePermissionMapper;
import com.english.mapper.ExamModuleMapper;
import com.english.mapper.ShopOrderMapper;
import com.english.mapper.UserMapper;
import com.english.service.AdminService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class AdminServiceImpl implements AdminService {
    private final ShopOrderMapper orderMapper;
    private final ExamModuleMapper moduleMapper;
    private final UserMapper userMapper;
    private final AdminRoleMapper roleMapper;
    private final AdminPermissionMapper permissionMapper;
    private final AdminRolePermissionMapper rolePermissionMapper;

    public AdminServiceImpl(ShopOrderMapper orderMapper,
                            ExamModuleMapper moduleMapper,
                            UserMapper userMapper,
                            AdminRoleMapper roleMapper,
                            AdminPermissionMapper permissionMapper,
                            AdminRolePermissionMapper rolePermissionMapper) {
        this.orderMapper = orderMapper;
        this.moduleMapper = moduleMapper;
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.permissionMapper = permissionMapper;
        this.rolePermissionMapper = rolePermissionMapper;
    }

    @Override
    public List<ShopOrder> getOrders() {
        return orderMapper.findAllByOrderByCreatedAtDesc();
    }

    @Override
    public ShopOrder updateOrderStatus(Long orderId, String status) {
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
            existing.setCode(module.getCode());
            existing.setDescription(module.getDescription());
            existing.setIcon(module.getIcon());
            existing.setRoutePath(module.getRoutePath());
            existing.setSortOrder(module.getSortOrder());
            return moduleMapper.save(existing);
        }
        return moduleMapper.save(module);
    }

    @Override
    public void deleteModule(Long moduleId) {
        moduleMapper.deleteById(moduleId);
    }

    @Override
    public List<User> getUsers() {
        List<User> users = userMapper.findAll();
        users.forEach(user -> user.setPassword(null));
        return users;
    }

    @Override
    public User updateUserRole(Long userId, Long roleId) {
        roleMapper.findById(roleId).orElseThrow(() -> new RuntimeException("角色不存在"));
        User user = userMapper.findById(userId).orElseThrow(() -> new RuntimeException("用户不存在"));
        user.setRoleId(roleId);
        User saved = userMapper.save(user);
        saved.setPassword(null);
        return saved;
    }

    @Override
    public void deleteUser(Long userId) {
        userMapper.deleteById(userId);
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
            existing.setCode(role.getCode());
            existing.setName(role.getName());
            existing.setDescription(role.getDescription());
            return roleMapper.save(existing);
        }
        return roleMapper.save(role);
    }

    @Override
    public void deleteRole(Long roleId) {
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
    public void assignRolePermissions(Long roleId, List<Long> permissionIds) {
        roleMapper.findById(roleId).orElseThrow(() -> new RuntimeException("角色不存在"));
        Set<Long> uniqueIds = new HashSet<>(permissionIds);
        rolePermissionMapper.deleteByRoleId(roleId);
        for (Long permissionId : uniqueIds) {
            permissionMapper.findById(permissionId).orElseThrow(() -> new RuntimeException("权限不存在"));
            AdminRolePermission relation = new AdminRolePermission();
            relation.setRoleId(roleId);
            relation.setPermissionId(permissionId);
            rolePermissionMapper.save(relation);
        }
    }
}
