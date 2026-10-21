package com.english.config;

import com.english.entity.User;
import com.english.service.AdminTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class AdminAuthFilter extends OncePerRequestFilter {
    private final AdminTokenService tokenService;

    public AdminAuthFilter(AdminTokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/admin/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        String token = authorization != null && authorization.startsWith("Bearer ")
                ? authorization.substring(7)
                : null;

        User user;
        try {
            user = tokenService.validateToken(token);
        } catch (Exception error) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, error.getMessage());
            return;
        }

        String requiredPermission = resolvePermission(request);
        if (requiredPermission != null && !tokenService.hasPermission(user, requiredPermission)) {
            writeError(response, HttpServletResponse.SC_FORBIDDEN, "没有后台权限");
            return;
        }

        try {
            AdminAuthContext.set(user);
            filterChain.doFilter(request, response);
        } finally {
            AdminAuthContext.clear();
        }
    }

    private String resolvePermission(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String method = request.getMethod();
        Map<String, String> mapping = new LinkedHashMap<>();
        mapping.put("/api/admin/orders", "ORDER_MANAGE");
        mapping.put("/api/admin/modules", "MODULE_MANAGE");
        mapping.put("/api/admin/users", "USER_MANAGE");
        mapping.put("/api/admin/roles", "ROLE_MANAGE");
        mapping.put("/api/admin/permissions", "PERMISSION_MANAGE");
        mapping.put("/api/admin/audit", "AUDIT_LOGS");
        for (Map.Entry<String, String> entry : mapping.entrySet()) {
            if (uri.startsWith(entry.getKey())) {
                if (uri.contains("/roles/") && uri.endsWith("/permissions") && !"GET".equals(method)) {
                    return "ROLE_MANAGE";
                }
                return entry.getValue();
            }
        }
        return "ADMIN_DASHBOARD";
    }

    private void writeError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        String body = "{\"code\":" + status + ",\"message\":\"" + escape(message) + "\"}";
        response.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));
    }

    private String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
