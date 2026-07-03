package com.resky.yuaicodemother.utils;

import jakarta.servlet.http.HttpServletRequest;

public final class ClientIpUtils {
    private ClientIpUtils() {}

    public static String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        return realIp == null || realIp.isBlank() ? request.getRemoteAddr() : realIp.trim();
    }
}
