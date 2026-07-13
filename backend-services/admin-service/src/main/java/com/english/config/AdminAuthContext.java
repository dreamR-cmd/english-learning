package com.english.config;

import com.english.entity.User;

public final class AdminAuthContext {
    private static final ThreadLocal<User> CURRENT_ADMIN = new ThreadLocal<>();

    private AdminAuthContext() {}

    public static void set(User user) {
        CURRENT_ADMIN.set(user);
    }

    public static User get() {
        return CURRENT_ADMIN.get();
    }

    public static void clear() {
        CURRENT_ADMIN.remove();
    }
}
