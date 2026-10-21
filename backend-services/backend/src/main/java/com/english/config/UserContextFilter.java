package com.english.config;

import com.english.entity.User;
import com.english.service.AdminTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class UserContextFilter extends OncePerRequestFilter {
    public static final String USER_ID_HEADER = "X-User-Id";

    private static final List<String> EXCLUDE_PATHS = List.of(
            "/api/auth/login",
            "/api/auth/register",
            "/actuator/health",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/v3/**"
    );

    private final AdminTokenService tokenService;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public UserContextFilter(AdminTokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        HttpServletRequest sanitized = new HeaderOverrideRequest(request, USER_ID_HEADER, null);
        if (shouldSkip(sanitized)) {
            filterChain.doFilter(sanitized, response);
            return;
        }

        String authorization = sanitized.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "未登录");
            return;
        }

        try {
            User user = tokenService.validateToken(authorization.substring(7));
            filterChain.doFilter(new HeaderOverrideRequest(sanitized, USER_ID_HEADER, String.valueOf(user.getId())), response);
        } catch (RuntimeException error) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, error.getMessage());
        }
    }

    private boolean shouldSkip(HttpServletRequest request) {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI();
        if (!path.startsWith("/api/")) {
            return true;
        }
        return EXCLUDE_PATHS.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
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

    private static final class HeaderOverrideRequest extends HttpServletRequestWrapper {
        private final String headerName;
        private final String headerValue;

        private HeaderOverrideRequest(HttpServletRequest request, String headerName, String headerValue) {
            super(request);
            this.headerName = headerName;
            this.headerValue = headerValue;
        }

        @Override
        public String getHeader(String name) {
            if (headerName.equalsIgnoreCase(name)) {
                return headerValue;
            }
            return super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            if (headerName.equalsIgnoreCase(name)) {
                return headerValue == null
                        ? Collections.emptyEnumeration()
                        : Collections.enumeration(List.of(headerValue));
            }
            return super.getHeaders(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            Set<String> names = new LinkedHashSet<>();
            Enumeration<String> original = super.getHeaderNames();
            while (original.hasMoreElements()) {
                String name = original.nextElement();
                if (!headerName.equalsIgnoreCase(name)) {
                    names.add(name);
                }
            }
            if (headerValue != null) {
                names.add(headerName);
            }
            return Collections.enumeration(new ArrayList<>(names));
        }
    }
}
