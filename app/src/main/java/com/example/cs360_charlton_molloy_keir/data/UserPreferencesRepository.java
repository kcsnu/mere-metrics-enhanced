package com.example.cs360_charlton_molloy_keir.data;

import android.content.Context;

import com.example.cs360_charlton_molloy_keir.data.room.AppDatabase;
import com.example.cs360_charlton_molloy_keir.data.room.AppDatabaseProvider;
import com.example.cs360_charlton_molloy_keir.data.room.entity.UserSettingsEntity;
import com.example.cs360_charlton_molloy_keir.util.SessionManager;
import com.example.cs360_charlton_molloy_keir.util.SmsUtil;

/** Access to session state and per-user SMS settings */
public class UserPreferencesRepository {

    private final Context applicationContext;
    private final AppDatabase database;

    public UserPreferencesRepository(Context context) {
        this.applicationContext = context.getApplicationContext();
        this.database = AppDatabaseProvider.getInstance(applicationContext);
    }

    public void storeLoggedInUser(long userId) {
        SessionManager.storeLoggedInUser(applicationContext, userId);
    }

    public long getLoggedInUserId() {
        return SessionManager.getLoggedInUserId(applicationContext);
    }

    public boolean isSmsEnabled(long userId) {
        return getUserSettings(userId).smsEnabled;
    }

    public void setSmsEnabled(long userId, boolean enabled) {
        if (userId <= 0L) {
            return;
        }

        UserSettingsEntity settings = getStoredUserSettings(userId);
        if (settings == null) {
            database.userSettingsDao().upsert(new UserSettingsEntity(userId, enabled, "", null));
            return;
        }

        database.userSettingsDao().updateSmsEnabled(userId, enabled);
    }

    public String getSmsPhoneNumber(long userId) {
        return getUserSettings(userId).smsPhoneNumber;
    }

    public void setSmsPhoneNumber(long userId, String phoneNumber) {
        if (userId <= 0L) {
            return;
        }

        String sanitizedPhoneNumber = SmsUtil.sanitizePhoneNumber(phoneNumber);
        UserSettingsEntity settings = getStoredUserSettings(userId);
        if (settings == null) {
            database.userSettingsDao().upsert(
                    new UserSettingsEntity(userId, false, sanitizedPhoneNumber, null)
            );
            return;
        }

        database.userSettingsDao().updateSmsPhoneNumber(userId, sanitizedPhoneNumber);
    }

    public boolean wasGoalAlreadyNotified(long userId, double goalWeight) {
        Long storedGoalBits = getUserSettings(userId).lastGoalNotifiedBits;
        return storedGoalBits != null && storedGoalBits == Double.doubleToLongBits(goalWeight);
    }

    public void markGoalNotified(long userId, double goalWeight) {
        if (userId <= 0L) {
            return;
        }

        Long goalBits = Double.doubleToLongBits(goalWeight);
        UserSettingsEntity settings = getStoredUserSettings(userId);
        if (settings == null) {
            database.userSettingsDao().upsert(new UserSettingsEntity(userId, false, "", goalBits));
            return;
        }

        database.userSettingsDao().updateLastGoalNotifiedBits(userId, goalBits);
    }

    public void clearGoalNotified(long userId) {
        if (userId <= 0L) {
            return;
        }

        UserSettingsEntity settings = getStoredUserSettings(userId);
        if (settings == null) {
            database.userSettingsDao().upsert(new UserSettingsEntity(userId, false, "", null));
            return;
        }

        database.userSettingsDao().clearLastGoalNotifiedBits(userId);
    }

    private UserSettingsEntity getUserSettings(long userId) {
        if (userId <= 0L) {
            return new UserSettingsEntity(userId, false, "", null);
        }

        UserSettingsEntity settings = getStoredUserSettings(userId);
        if (settings != null) {
            return settings;
        }

        return new UserSettingsEntity(userId, false, "", null);
    }

    private UserSettingsEntity getStoredUserSettings(long userId) {
        if (userId <= 0L) {
            return null;
        }

        return database.userSettingsDao().getByUserId(userId);
    }
}
