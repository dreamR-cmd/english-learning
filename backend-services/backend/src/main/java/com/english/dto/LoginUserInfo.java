package com.english.dto;

import com.english.entity.AdminPermission;
import com.english.entity.AdminRole;
import com.english.entity.User;

import java.util.List;

public class LoginUserInfo {
    private Long id;
    private String username;
    private String nickname;
    private Integer dailyWordTarget;
    private Long roleId;
    private String roleCode;
    private String roleName;
    private String token;
    private List<String> permissions;
    private List<AdminPermission> permissionItems;

    public static LoginUserInfo from(User user, AdminRole role, List<AdminPermission> permissions) {
        LoginUserInfo info = new LoginUserInfo();
        info.setId(user.getId());
        info.setUsername(user.getUsername());
        info.setNickname(user.getNickname());
        info.setDailyWordTarget(user.getDailyWordTarget());
        info.setRoleId(user.getRoleId());
        if (role != null) {
            info.setRoleCode(role.getCode());
            info.setRoleName(role.getName());
        }
        info.setPermissionItems(permissions);
        info.setPermissions(permissions.stream().map(AdminPermission::getCode).toList());
        return info;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public Integer getDailyWordTarget() { return dailyWordTarget; }
    public void setDailyWordTarget(Integer dailyWordTarget) { this.dailyWordTarget = dailyWordTarget; }
    public Long getRoleId() { return roleId; }
    public void setRoleId(Long roleId) { this.roleId = roleId; }
    public String getRoleCode() { return roleCode; }
    public void setRoleCode(String roleCode) { this.roleCode = roleCode; }
    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public List<String> getPermissions() { return permissions; }
    public void setPermissions(List<String> permissions) { this.permissions = permissions; }
    public List<AdminPermission> getPermissionItems() { return permissionItems; }
    public void setPermissionItems(List<AdminPermission> permissionItems) { this.permissionItems = permissionItems; }
}
