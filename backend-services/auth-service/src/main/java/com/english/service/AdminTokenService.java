package com.english.service;

import com.english.entity.AdminPermission;
import com.english.entity.AdminRole;
import com.english.entity.AdminRolePermission;
import com.english.entity.User;
import com.english.mapper.AdminPermissionMapper;
import com.english.mapper.AdminRoleMapper;
import com.english.mapper.AdminRolePermissionMapper;
import com.english.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

@Service
public class AdminTokenService {
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final long TOKEN_TTL_SECONDS = 24 * 60 * 60;

    private final UserMapper userMapper;
    private final AdminRoleMapper roleMapper;
    private final AdminPermissionMapper permissionMapper;
    private final AdminRolePermissionMapper rolePermissionMapper;
    private final String secret;

    public AdminTokenService(UserMapper userMapper,
                             AdminRoleMapper roleMapper,
                             AdminPermissionMapper permissionMapper,
                             AdminRolePermissionMapper rolePermissionMapper,
                             @Value("${admin.token.secret:english-learning-admin-secret}") String secret) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.permissionMapper = permissionMapper;
        this.rolePermissionMapper = rolePermissionMapper;
        this.secret = secret;
    }

    public String createToken(User user) {
        long expiresAt = Instant.now().getEpochSecond() + TOKEN_TTL_SECONDS;
        String payload = user.getId() + ":" + expiresAt;
        return base64Url(payload) + "." + sign(payload);
    }

    public User validateToken(String token) {
        if (token == null || token.isBlank()) {
            throw new RuntimeException("未登录");
        }
        String[] parts = token.split("\\.");
        if (parts.length != 2) {
            throw new RuntimeException("无效 token");
        }
        String payload = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
        if (!sign(payload).equals(parts[1])) {
            throw new RuntimeException("token 签名无效");
        }
        String[] fields = payload.split(":");
        if (fields.length != 2) {
            throw new RuntimeException("token 内容无效");
        }
        long expiresAt = Long.parseLong(fields[1]);
        if (Instant.now().getEpochSecond() > expiresAt) {
            throw new RuntimeException("登录已过期");
        }
        Long userId = Long.valueOf(fields[0]);
        return userMapper.findById(userId).orElseThrow(() -> new RuntimeException("用户不存在"));
    }

    public boolean hasPermission(User user, String permissionCode) {
        if (user.getRoleId() == null) {
            return false;
        }
        AdminRole role = roleMapper.findById(user.getRoleId()).orElse(null);
        if (role == null || !"ADMIN".equals(role.getCode())) {
            return false;
        }
        List<Long> permissionIds = rolePermissionMapper.findByRoleId(role.getId()).stream()
                .map(AdminRolePermission::getPermissionId)
                .toList();
        if (permissionIds.isEmpty()) {
            return false;
        }
        return permissionMapper.findAllById(permissionIds).stream()
                .map(AdminPermission::getCode)
                .anyMatch(permissionCode::equals);
    }

    private String base64Url(String payload) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] signature = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
        } catch (Exception error) {
            throw new RuntimeException("token 签名失败", error);
        }
    }
}
