package com.english.config;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component
@RequestScope
public class CurrentUser {
        private final Long userId;

    public CurrentUser(jakarta.servlet.http.HttpServletRequest request) {
        String raw = request.getHeader("X-User-Id");
        this.userId = raw == null || raw.isBlank() ? null : Long.valueOf(raw);
    }

    public Long getUserId() {
        if (userId == null) {
            throw new RuntimeException("未登录");
        }
        return userId;
    }
}
