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

public class AdminAuthFilter extends OncePerRequestFilter {
    private final AdminTokenService tokenService;

    public AdminAuthFilter(AdminTokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/admin/shop/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String authorization = request.getHeader("Authorization");
            String token = authorization != null && authorization.startsWith("Bearer ")
                    ? authorization.substring(7)
                    : null;
            User user = tokenService.validateToken(token);
            String requiredPermission = resolvePermission(request);
            if (requiredPermission != null && !tokenService.hasPermission(user, requiredPermission)) {
                writeError(response, HttpServletResponse.SC_FORBIDDEN, "没有后台权限");
                return;
            }
            filterChain.doFilter(request, response);
        } catch (Exception error) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, error.getMessage());
        }
    }

    private String resolvePermission(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri.startsWith("/api/admin/shop/orders")) {
            return "ORDER_MANAGE";
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
