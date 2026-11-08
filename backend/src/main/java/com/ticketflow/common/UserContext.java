package com.ticketflow.common;

import org.springframework.http.HttpStatus;

public final class UserContext {
    private static final ThreadLocal<Long> CURRENT_USER = new ThreadLocal<>();

    private UserContext() {}

    public static void setUserId(long userId) { CURRENT_USER.set(userId); }

    public static long getUserId() {
        Long userId = CURRENT_USER.get();
        if (userId == null) throw new AppException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "请先登录");
        return userId;
    }

    public static void clear() { CURRENT_USER.remove(); }
}
