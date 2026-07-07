package com.english.service.impl;

import com.english.dto.LoginUserInfo;
import com.english.entity.AdminPermission;
import com.english.entity.AdminRole;
import com.english.entity.AdminRolePermission;
import com.english.entity.User;
import com.english.mapper.AdminPermissionMapper;
import com.english.mapper.AdminRoleMapper;
import com.english.mapper.AdminRolePermissionMapper;
import com.english.mapper.UserMapper;
import com.english.service.AdminTokenService;
import com.english.service.AuthService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuthServiceImpl implements AuthService {
    private static final String ROLE_USER = "USER";

    private final UserMapper userMapper;
    private final AdminRoleMapper roleMapper;
    private final AdminPermissionMapper permissionMapper;
    private final AdminRolePermissionMapper rolePermissionMapper;
    private final AdminTokenService tokenService;
    private final PasswordEncoder passwordEncoder;

    public AuthServiceImpl(UserMapper userMapper,
                           AdminRoleMapper roleMapper,
                           AdminPermissionMapper permissionMapper,
                           AdminRolePermissionMapper rolePermissionMapper,
                           AdminTokenService tokenService,
                           PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.permissionMapper = permissionMapper;
        this.rolePermissionMapper = rolePermissionMapper;
        this.tokenService = tokenService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public LoginUserInfo login(String username, String password) {
        User user = userMapper.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        if (!passwordMatches(password, user.getPassword())) {
            throw new RuntimeException("密码错误");
        }
        upgradeLegacyPasswordIfNecessary(user, password);
        return toLoginUserInfo(user);
    }

    @Override
    public LoginUserInfo register(String username, String password, String nickname) {
        if (userMapper.findByUsername(username).isPresent()) {
            throw new RuntimeException("用户名已存在");
        }
        User user = new User(username, passwordEncoder.encode(password), nickname == null ? username : nickname);
        roleMapper.findByCode(ROLE_USER)
                .ifPresent(role -> user.setRoleId(role.getId()));
        return toLoginUserInfo(userMapper.save(user));
    }

    private boolean passwordMatches(String rawPassword, String storedPassword) {
        if (storedPassword == null) {
            return false;
        }
        if (isArgon2Password(storedPassword)) {
            return passwordEncoder.matches(rawPassword, storedPassword);
        }
        return storedPassword.equals(rawPassword);
    }

    private void upgradeLegacyPasswordIfNecessary(User user, String rawPassword) {
        if (isArgon2Password(user.getPassword())) {
            return;
        }
        user.setPassword(passwordEncoder.encode(rawPassword));
        userMapper.save(user);
    }

    private boolean isArgon2Password(String password) {
        return password != null && password.startsWith("$argon2");
    }

    private LoginUserInfo toLoginUserInfo(User user) {
        AdminRole role = user.getRoleId() == null ? null : roleMapper.findById(user.getRoleId()).orElse(null);
        List<AdminPermission> permissions = role == null
                ? List.of()
                : permissionMapper.findAllById(rolePermissionMapper.findByRoleId(role.getId()).stream()
                        .map(AdminRolePermission::getPermissionId)
                        .toList());
        LoginUserInfo info = LoginUserInfo.from(user, role, permissions);
        info.setToken(tokenService.createToken(user));
        return info;
    }
}
