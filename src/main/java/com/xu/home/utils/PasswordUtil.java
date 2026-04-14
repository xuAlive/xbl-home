package com.xu.home.utils;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public final class PasswordUtil {

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    private PasswordUtil() {
    }

    public static String encode(String rawPassword) {
        return ENCODER.encode(rawPassword);
    }

    public static boolean matches(String rawPassword, String storedPassword) {
        if (rawPassword == null || storedPassword == null || storedPassword.isBlank()) {
            return false;
        }
        if (isEncoded(storedPassword)) {
            return ENCODER.matches(rawPassword, storedPassword);
        }
        return rawPassword.equals(storedPassword);
    }

    public static boolean needsUpgrade(String storedPassword) {
        return !isEncoded(storedPassword);
    }

    private static boolean isEncoded(String password) {
        return password.startsWith("$2a$") || password.startsWith("$2b$") || password.startsWith("$2y$");
    }
}
