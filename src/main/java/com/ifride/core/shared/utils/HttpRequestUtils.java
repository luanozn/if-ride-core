package com.ifride.core.shared.utils;

import jakarta.servlet.http.HttpServletRequest;

public class HttpRequestUtils {

    public static String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");

        if (xfHeader == null || xfHeader.isEmpty()) {
            return request.getRemoteAddr();
        }

        String ip = xfHeader.split(",")[0].trim();

        if ("0:0:0:0:0:0:0:1".equals(ip)) {
            return "127.0.0.1";
        }

        return ip;
    }
}