package com.pandanav.learning.auth;

public final class UserContextHolder {

    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();

    private UserContextHolder() {
    }

    public static void setUserId(Long userId) {
        USER_ID.set(userId);
    }

    public static Long getUserId() {
        return USER_ID.get();
    }

    public static Long getRequiredUserId() {
        Long userId = USER_ID.get();
        if (userId == null) {
            throw new UnauthorizedException("Unauthorized.");
        }
        return userId;
    }

    public static void clear() {
        USER_ID.remove();
    }
}
