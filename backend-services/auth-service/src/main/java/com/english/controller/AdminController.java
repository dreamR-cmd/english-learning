package com.english.controller;

import com.english.dto.ApiResult;
import com.english.entity.AdminPermission;
import com.english.entity.AdminRole;
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

@Tag(name = "后台认证管理接口", description = "后台用户、角色和权限管理接口")
@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @Operation(summary = "查询全部用户", description = "后台查询全部用户，密码字段不会返回。")
    @GetMapping("/users")
    public ApiResult<List<User>> getUsers() {
        return ApiResult.success(adminService.getUsers());
    }

    @Operation(summary = "分配用户角色", description = "给用户分配角色。请求体字段：roleId 角色 ID。")
    @PutMapping("/users/{userId}/role")
    public ApiResult<User> updateUserRole(@PathVariable Long userId, @RequestBody Map<String, Long> body) {
        return ApiResult.success(adminService.updateUserRole(userId, body.get("roleId")));
    }

    @Operation(summary = "删除用户", description = "后台删除用户。")
    @DeleteMapping("/users/{userId}")
    public ApiResult<Void> deleteUser(@PathVariable Long userId) {
        adminService.deleteUser(userId);
        return ApiResult.success("删除成功", null);
    }

    @Operation(summary = "查询角色", description = "后台查询全部角色。")
    @GetMapping("/roles")
    public ApiResult<List<AdminRole>> getRoles() {
        return ApiResult.success(adminService.getRoles());
    }

    @Operation(summary = "创建角色", description = "后台创建角色。")
    @PostMapping("/roles")
    public ApiResult<AdminRole> createRole(@RequestBody AdminRole role) {
        role.setId(null);
        return ApiResult.success(adminService.saveRole(role));
    }

    @Operation(summary = "更新角色", description = "后台更新角色。")
    @PutMapping("/roles/{roleId}")
    public ApiResult<AdminRole> updateRole(@PathVariable Long roleId, @RequestBody AdminRole role) {
        role.setId(roleId);
        return ApiResult.success(adminService.saveRole(role));
    }

    @Operation(summary = "删除角色", description = "后台删除角色，并删除角色权限关系。")
    @DeleteMapping("/roles/{roleId}")
    public ApiResult<Void> deleteRole(@PathVariable Long roleId) {
        adminService.deleteRole(roleId);
        return ApiResult.success("删除成功", null);
    }

    @Operation(summary = "查询权限", description = "查询所有后台权限菜单。")
    @GetMapping("/permissions")
    public ApiResult<List<AdminPermission>> getPermissions() {
        return ApiResult.success(adminService.getPermissions());
    }

    @Operation(summary = "查询角色权限", description = "查询指定角色已分配的权限。")
    @GetMapping("/roles/{roleId}/permissions")
    public ApiResult<List<AdminPermission>> getRolePermissions(@PathVariable Long roleId) {
        return ApiResult.success(adminService.getRolePermissions(roleId));
    }

    @Operation(summary = "分配角色权限", description = "给角色分配权限。请求体字段：permissionIds 权限 ID 数组。")
    @PutMapping("/roles/{roleId}/permissions")
    public ApiResult<Void> assignRolePermissions(@PathVariable Long roleId, @RequestBody Map<String, List<Long>> body) {
        adminService.assignRolePermissions(roleId, body.getOrDefault("permissionIds", List.of()));
        return ApiResult.success("分配成功", null);
    }
}
