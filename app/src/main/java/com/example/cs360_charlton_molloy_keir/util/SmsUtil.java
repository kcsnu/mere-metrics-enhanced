package com.example.cs360_charlton_molloy_keir.util;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.telephony.SmsManager;

import androidx.core.content.ContextCompat;

/** SMS helpers for permission checks, phone validation, and sending messages */
public final class SmsUtil {

    private static final int MAX_SMS_CHARS = 140;
    private static final int MIN_PHONE_DIGITS = 10;
    private static final int MAX_PHONE_DIGITS = 15;

    private SmsUtil() {
    }

    public static boolean hasSendSmsPermission(Context context) {
        return context != null
                && ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static String sanitizePhoneNumber(String rawPhoneNumber) {
        if (rawPhoneNumber == null) {
            return "";
        }

        String trimmedNumber = rawPhoneNumber.trim();
        if (trimmedNumber.isEmpty()) {
            return "";
        }

        StringBuilder sanitizedNumber = new StringBuilder();
        for (int i = 0; i < trimmedNumber.length(); i++) {
            char currentChar = trimmedNumber.charAt(i);

            if (Character.isDigit(currentChar)) {
                sanitizedNumber.append(currentChar);
            } else if (currentChar == '+' && sanitizedNumber.length() == 0) {
                sanitizedNumber.append(currentChar);
            }
        }

        return sanitizedNumber.toString();
    }

    public static boolean isValidDestinationPhoneNumber(String sanitizedPhoneNumber) {
        if (sanitizedPhoneNumber == null) {
            return false;
        }

        String trimmedNumber = sanitizedPhoneNumber.trim();
        if (trimmedNumber.isEmpty()) {
            return false;
        }

        int digitCount = 0;
        for (int i = 0; i < trimmedNumber.length(); i++) {
            char currentChar = trimmedNumber.charAt(i);

            if (Character.isDigit(currentChar)) {
                digitCount++;
            } else if (currentChar == '+' && i == 0) {
                // Allow a leading plus sign for international numbers
            } else {
                return false;
            }
        }

        return digitCount >= MIN_PHONE_DIGITS && digitCount <= MAX_PHONE_DIGITS;
    }

    public static boolean trySendSms(Context context, String phoneNumber, String message) {
        if (!hasSendSmsPermission(context)) {
            return false;
        }

        String sanitizedPhoneNumber = sanitizePhoneNumber(phoneNumber);
        if (!isValidDestinationPhoneNumber(sanitizedPhoneNumber)) {
            return false;
        }

        String normalizedMessage = normalizeMessageBody(message);
        if (normalizedMessage.isEmpty()) {
            return false;
        }

        try {
            SmsManager smsManager = context.getSystemService(SmsManager.class);
            if (smsManager == null) {
                return false;
            }

            smsManager.sendTextMessage(sanitizedPhoneNumber, null, normalizedMessage, null, null);
            return true;
        } catch (IllegalArgumentException | SecurityException | UnsupportedOperationException e) {
            return false;
        }
    }

    private static String normalizeMessageBody(String message) {
        String safeMessage = message == null ? "" : message.trim();
        if (safeMessage.isEmpty()) {
            return "";
        }

        // Keep the message short to reduce the chance of multipart SMS delivery
        if (safeMessage.length() > MAX_SMS_CHARS) {
            return safeMessage.substring(0, MAX_SMS_CHARS);
        }

        return safeMessage;
    }
}
