package com.english.controller;

import com.english.config.AdminAudit;
import com.english.dto.AdminConfirmRequest;
import com.english.dto.ApiResult;
import com.english.entity.AdminOperationLog;
import com.english.entity.AdminPermission;
import com.english.entity.AdminPermissionChangeLog;
import com.english.entity.AdminRole;
import com.english.entity.ExamModule;
import com.english.entity.ShopOrder;
import com.english.entity.User;
import com.english.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Tag(name = "Admin Management")
@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/orders")
    public ApiResult<List<ShopOrder>> getOrders() {
        return ApiResult.success(adminService.getOrders());
    }

    @Operation(summary = "Update order status")
    @PutMapping("/orders/{orderId}/status")
    @AdminAudit(module = "ORDER", action = "UPDATE_STATUS", targetId = "#orderId")
    public ApiResult<ShopOrder> updateOrderStatus(@PathVariable Long orderId, @RequestBody Map<String, String> body) {
        return ApiResult.success(adminService.updateOrderStatus(orderId, body.get("status"), body.get("confirmText")));
    }

    @GetMapping("/modules")
    public ApiResult<List<ExamModule>> getModules() {
        return ApiResult.success(adminService.getModules());
    }

    @PostMapping("/modules")
    @AdminAudit(module = "MODULE", action = "CREATE")
    public ApiResult<ExamModule> createModule(@RequestBody ExamModule module) {
        module.setId(null);
        module.setSystemBuiltin(false);
        return ApiResult.success(adminService.saveModule(module));
    }

    @PutMapping("/modules/{moduleId}")
    @AdminAudit(module = "MODULE", action = "UPDATE", targetId = "#moduleId")
    public ApiResult<ExamModule> updateModule(@PathVariable Long moduleId, @RequestBody ExamModule module) {
        module.setId(moduleId);
        return ApiResult.success(adminService.saveModule(module));
    }

    @DeleteMapping("/modules/{moduleId}")
    @AdminAudit(module = "MODULE", action = "DELETE", targetId = "#moduleId")
    public ApiResult<Void> deleteModule(@PathVariable Long moduleId,
                                        @RequestBody(required = false) AdminConfirmRequest request) {
        adminService.deleteModule(moduleId, request == null ? null : request.getConfirmText());
        return ApiResult.success("删除成功", null);
    }

    @GetMapping("/users")
    public ApiResult<List<User>> getUsers() {
        return ApiResult.success(adminService.getUsers());
    }

    @PutMapping("/users/{userId}/role")
    @AdminAudit(module = "USER", action = "UPDATE_ROLE", targetId = "#userId")
    public ApiResult<User> updateUserRole(@PathVariable Long userId, @RequestBody Map<String, Object> body) {
        Long roleId = body.get("roleId") == null ? null : Long.valueOf(String.valueOf(body.get("roleId")));
        String confirmText = body.get("confirmText") == null ? null : String.valueOf(body.get("confirmText"));
        return ApiResult.success(adminService.updateUserRole(userId, roleId, confirmText));
    }

    @DeleteMapping("/users/{userId}")
    @AdminAudit(module = "USER", action = "DISABLE", targetId = "#userId")
    public ApiResult<Void> deleteUser(@PathVariable Long userId,
                                      @RequestBody(required = false) AdminConfirmRequest request) {
        adminService.deleteUser(userId, request == null ? null : request.getConfirmText());
        return ApiResult.success("用户已禁用", null);
    }

    @GetMapping("/roles")
    public ApiResult<List<AdminRole>> getRoles() {
        return ApiResult.success(adminService.getRoles());
    }

    @PostMapping("/roles")
    @AdminAudit(module = "ROLE", action = "CREATE")
    public ApiResult<AdminRole> createRole(@RequestBody AdminRole role) {
        role.setId(null);
        role.setSystemBuiltin(false);
        return ApiResult.success(adminService.saveRole(role));
    }

    @PutMapping("/roles/{roleId}")
    @AdminAudit(module = "ROLE", action = "UPDATE", targetId = "#roleId")
    public ApiResult<AdminRole> updateRole(@PathVariable Long roleId, @RequestBody AdminRole role) {
        role.setId(roleId);
        return ApiResult.success(adminService.saveRole(role));
    }

    @DeleteMapping("/roles/{roleId}")
    @AdminAudit(module = "ROLE", action = "DELETE", targetId = "#roleId")
    public ApiResult<Void> deleteRole(@PathVariable Long roleId,
                                      @RequestBody(required = false) AdminConfirmRequest request) {
        adminService.deleteRole(roleId, request == null ? null : request.getConfirmText());
        return ApiResult.success("删除成功", null);
    }

    @GetMapping("/permissions")
    public ApiResult<List<AdminPermission>> getPermissions() {
        return ApiResult.success(adminService.getPermissions());
    }

    @GetMapping("/roles/{roleId}/permissions")
    public ApiResult<List<AdminPermission>> getRolePermissions(@PathVariable Long roleId) {
        return ApiResult.success(adminService.getRolePermissions(roleId));
    }

    @PutMapping("/roles/{roleId}/permissions")
    @AdminAudit(module = "ROLE", action = "ASSIGN_PERMISSIONS", targetId = "#roleId")
    public ApiResult<Void> assignRolePermissions(@PathVariable Long roleId, @RequestBody Map<String, Object> body) {
        Object rawPermissionIds = body.get("permissionIds");
        List<Long> permissionIds = rawPermissionIds instanceof List<?> values
                ? values.stream().map(value -> Long.valueOf(String.valueOf(value))).toList()
                : List.of();
        String confirmText = body.get("confirmText") == null ? null : String.valueOf(body.get("confirmText"));
        adminService.assignRolePermissions(roleId, permissionIds, confirmText);
        return ApiResult.success("分配成功", null);
    }

    @GetMapping("/audit/operations")
    public ApiResult<List<AdminOperationLog>> getOperationLogs() {
        return ApiResult.success(adminService.getOperationLogs());
    }

    @GetMapping("/audit/permission-changes")
    public ApiResult<List<AdminPermissionChangeLog>> getPermissionChangeLogs() {
        return ApiResult.success(adminService.getPermissionChangeLogs());
    }
}
