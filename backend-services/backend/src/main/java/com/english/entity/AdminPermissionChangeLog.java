package com.english.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "admin_permission_change_logs")
public class AdminPermissionChangeLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long adminId;
    private String adminUsername;
    @Column(nullable = false)
    private Long roleId;
    private String roleCode;
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String beforePermissionIds;
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String afterPermissionIds;
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String addedPermissionIds;
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String removedPermissionIds;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getAdminId() { return adminId; }
    public void setAdminId(Long adminId) { this.adminId = adminId; }
    public String getAdminUsername() { return adminUsername; }
    public void setAdminUsername(String adminUsername) { this.adminUsername = adminUsername; }
    public Long getRoleId() { return roleId; }
    public void setRoleId(Long roleId) { this.roleId = roleId; }
    public String getRoleCode() { return roleCode; }
    public void setRoleCode(String roleCode) { this.roleCode = roleCode; }
    public String getBeforePermissionIds() { return beforePermissionIds; }
    public void setBeforePermissionIds(String beforePermissionIds) { this.beforePermissionIds = beforePermissionIds; }
    public String getAfterPermissionIds() { return afterPermissionIds; }
    public void setAfterPermissionIds(String afterPermissionIds) { this.afterPermissionIds = afterPermissionIds; }
    public String getAddedPermissionIds() { return addedPermissionIds; }
    public void setAddedPermissionIds(String addedPermissionIds) { this.addedPermissionIds = addedPermissionIds; }
    public String getRemovedPermissionIds() { return removedPermissionIds; }
    public void setRemovedPermissionIds(String removedPermissionIds) { this.removedPermissionIds = removedPermissionIds; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
