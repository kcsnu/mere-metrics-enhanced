package com.example.cs360_charlton_molloy_keir.util;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Locale;

/** Date helpers for parsing, normalization, and epoch-day conversion */
public final class DateUtil {

    private static final String DISPLAY_DATE_PATTERN = "MM/dd/yyyy";
    private static final String STORAGE_DATE_PATTERN = "yyyy-MM-dd";
    // Keep parsing strict so impossible dates are rejected.
    private static final DateTimeFormatter DISPLAY_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("MM/dd/uuuu", Locale.US)
                    .withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter STORAGE_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("uuuu-MM-dd", Locale.US)
                    .withResolverStyle(ResolverStyle.STRICT);

    private DateUtil() {
    }

    public static String getTodayDate() {
        return formatDate(LocalDate.now(ZoneId.systemDefault()));
    }

    public static boolean isValidDate(String dateText) {
        return parseDate(dateText) != null;
    }

    /** Parses an MM/dd/yyyy string into a LocalDate, or null for invalid input */
    public static LocalDate parseDate(String dateText) {
        String normalizedText = StringUtil.safeTrim(dateText);
        if (normalizedText.length() != DISPLAY_DATE_PATTERN.length()) {
            return null;
        }

        try {
            return LocalDate.parse(normalizedText, DISPLAY_DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /** Parses a stored yyyy-MM-dd string into a LocalDate, or null for invalid input */
    public static LocalDate parseStorageDate(String dateText) {
        String normalizedText = StringUtil.safeTrim(dateText);
        if (normalizedText.length() != STORAGE_DATE_PATTERN.length()) {
            return null;
        }

        try {
            return LocalDate.parse(normalizedText, STORAGE_DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /** Normalizes user input into the standard MM/dd/yyyy format */
    public static String normalizeDate(String dateText) {
        LocalDate parsedDate = parseDate(dateText);
        if (parsedDate == null) {
            return null;
        }

        return formatDate(parsedDate);
    }

    public static String formatDate(LocalDate date) {
        return DISPLAY_DATE_FORMATTER.format(date);
    }

    /** Formats a LocalDate into the Room storage pattern yyyy-MM-dd */
    public static String formatStorageDate(LocalDate date) {
        return STORAGE_DATE_FORMATTER.format(date);
    }

    /** Converts a display date into the Room storage pattern, or null for invalid input */
    public static String toStorageDate(String displayDateText) {
        LocalDate parsedDate = parseDate(displayDateText);
        if (parsedDate == null) {
            return null;
        }

        return formatStorageDate(parsedDate);
    }

    /** Converts a stored yyyy-MM-dd date into the display pattern, or null for invalid input */
    public static String toDisplayDate(String storageDateText) {
        LocalDate parsedDate = parseStorageDate(storageDateText);
        if (parsedDate == null) {
            return null;
        }

        return formatDate(parsedDate);
    }

    /** Converts a normalized date into a numeric day value for binary-search lookups */
    public static long toEpochDay(LocalDate date) {
        return date.toEpochDay();
    }
}
