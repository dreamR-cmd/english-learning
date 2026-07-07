package com.english.service;

import com.english.entity.AdminPermission;
import com.english.entity.AdminRole;
import com.english.entity.ExamModule;
import com.english.entity.ShopOrder;
import com.english.entity.User;

import java.util.List;

public interface AdminService {
    List<ShopOrder> getOrders();
    ShopOrder updateOrderStatus(Long orderId, String status);

    List<ExamModule> getModules();
    ExamModule saveModule(ExamModule module);
    void deleteModule(Long moduleId);

    List<User> getUsers();
    User updateUserRole(Long userId, Long roleId);
    void deleteUser(Long userId);

    List<AdminRole> getRoles();
    AdminRole saveRole(AdminRole role);
    void deleteRole(Long roleId);
    List<AdminPermission> getPermissions();
    List<AdminPermission> getRolePermissions(Long roleId);
    void assignRolePermissions(Long roleId, List<Long> permissionIds);
}
