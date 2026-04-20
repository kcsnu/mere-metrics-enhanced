package com.example.cs360_charlton_molloy_keir.util;

/** Shared string helpers used across service, data, and utility layers */
public final class StringUtil {

    private StringUtil() {
    }

    public static String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    public static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
