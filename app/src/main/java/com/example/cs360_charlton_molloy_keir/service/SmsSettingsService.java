package com.example.cs360_charlton_molloy_keir.service;

import android.content.Context;

import com.example.cs360_charlton_molloy_keir.data.UserPreferencesRepository;
import com.example.cs360_charlton_molloy_keir.util.SmsUtil;

/** Handles SMS settings, phone validation, and alert-enablement rules */
public class SmsSettingsService {

    public enum SavePhoneStatus {
        SUCCESS,
        MISSING_PHONE,
        INVALID_PHONE
    }

    public enum EnableAlertsStatus {
        ENABLED,
        PERMISSION_MISSING,
        MISSING_PHONE,
        INVALID_PHONE
    }

    /** Screen state used by the SMS settings UI */
    public static final class SmsScreenState {
        private final boolean enabledForAccount;
        private final boolean hasPermission;

        private SmsScreenState(boolean enabledForAccount, boolean hasPermission) {
            this.enabledForAccount = enabledForAccount;
            this.hasPermission = hasPermission;
        }

        public boolean isSwitchChecked() {
            return enabledForAccount && hasPermission;
        }

        public boolean isEffectiveOn() {
            return isSwitchChecked();
        }
    }

    /** Result of saving a phone number */
    public static final class SavePhoneResult {
        private final SavePhoneStatus status;
        private final String sanitizedPhoneNumber;

        private SavePhoneResult(SavePhoneStatus status, String sanitizedPhoneNumber) {
            this.status = status;
            this.sanitizedPhoneNumber = sanitizedPhoneNumber;
        }

        public SavePhoneStatus getStatus() {
            return status;
        }

        public String getSanitizedPhoneNumber() {
            return sanitizedPhoneNumber;
        }
    }

    /** Result of trying to enable SMS alerts */
    public static final class EnableAlertsResult {
        private final EnableAlertsStatus status;
        private final String sanitizedPhoneNumber;

        private EnableAlertsResult(EnableAlertsStatus status, String sanitizedPhoneNumber) {
            this.status = status;
            this.sanitizedPhoneNumber = sanitizedPhoneNumber;
        }

        public EnableAlertsStatus getStatus() {
            return status;
        }

        public String getSanitizedPhoneNumber() {
            return sanitizedPhoneNumber;
        }
    }

    private final UserPreferencesRepository userPreferencesRepository;

    public SmsSettingsService(Context context) {
        this.userPreferencesRepository = new UserPreferencesRepository(context);
    }

    public long getLoggedInUserId() {
        return userPreferencesRepository.getLoggedInUserId();
    }

    public String getSavedPhoneNumber(long userId) {
        return userPreferencesRepository.getSmsPhoneNumber(userId);
    }

    public SmsScreenState getScreenState(long userId, boolean hasPermission) {
        boolean enabledForAccount = userPreferencesRepository.isSmsEnabled(userId);
        return new SmsScreenState(enabledForAccount, hasPermission);
    }

    public SavePhoneResult savePhoneNumber(long userId, String rawPhoneNumber) {
        String sanitizedPhoneNumber = SmsUtil.sanitizePhoneNumber(rawPhoneNumber);

        if (sanitizedPhoneNumber.isEmpty()) {
            return new SavePhoneResult(SavePhoneStatus.MISSING_PHONE, sanitizedPhoneNumber);
        }

        if (!SmsUtil.isValidDestinationPhoneNumber(sanitizedPhoneNumber)) {
            return new SavePhoneResult(SavePhoneStatus.INVALID_PHONE, sanitizedPhoneNumber);
        }

        userPreferencesRepository.setSmsPhoneNumber(userId, sanitizedPhoneNumber);
        return new SavePhoneResult(SavePhoneStatus.SUCCESS, sanitizedPhoneNumber);
    }

    public void disableAlerts(long userId) {
        userPreferencesRepository.setSmsEnabled(userId, false);
    }

    public void handlePermissionDenied(long userId) {
        disableAlerts(userId);
    }

    public EnableAlertsResult enableAlertsIfPhoneValid(
            long userId,
            String typedPhoneNumber,
            boolean hasPermission
    ) {
        if (!hasPermission) {
            disableAlerts(userId);
            return new EnableAlertsResult(EnableAlertsStatus.PERMISSION_MISSING, "");
        }

        String sanitizedPhoneNumber = SmsUtil.sanitizePhoneNumber(
                userPreferencesRepository.getSmsPhoneNumber(userId)
        );

        // Prefer the saved number so returning users do not need to enter it again
        if (sanitizedPhoneNumber.isEmpty()) {
            sanitizedPhoneNumber = SmsUtil.sanitizePhoneNumber(typedPhoneNumber);
        }

        if (sanitizedPhoneNumber.isEmpty()) {
            disableAlerts(userId);
            return new EnableAlertsResult(EnableAlertsStatus.MISSING_PHONE, sanitizedPhoneNumber);
        }

        if (!SmsUtil.isValidDestinationPhoneNumber(sanitizedPhoneNumber)) {
            disableAlerts(userId);
            return new EnableAlertsResult(EnableAlertsStatus.INVALID_PHONE, sanitizedPhoneNumber);
        }

        userPreferencesRepository.setSmsPhoneNumber(userId, sanitizedPhoneNumber);
        userPreferencesRepository.setSmsEnabled(userId, true);
        return new EnableAlertsResult(EnableAlertsStatus.ENABLED, sanitizedPhoneNumber);
    }
}