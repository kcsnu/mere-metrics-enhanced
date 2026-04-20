package com.example.cs360_charlton_molloy_keir.util;

import java.util.Locale;

/** Shared formatting helpers for weight display values */
public final class FormatUtil {

    private FormatUtil() {
    }

    public static String formatWeight(double weight) {
        return String.format(Locale.US, "%.1f", weight);
    }
}
