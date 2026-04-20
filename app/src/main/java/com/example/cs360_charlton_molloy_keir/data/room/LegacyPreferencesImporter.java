package com.example.cs360_charlton_molloy_keir.data.room;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import com.example.cs360_charlton_molloy_keir.data.room.entity.UserSettingsEntity;
import com.example.cs360_charlton_molloy_keir.util.SessionManager;
import com.example.cs360_charlton_molloy_keir.util.SmsUtil;

import java.util.List;

/** Imports legacy SMS settings into the database */
public final class LegacyPreferencesImporter {

    private static final Object IMPORT_LOCK = new Object();

    private static final String PREFS_NAME = "cs360_prefs";
    private static final String KEY_SMS_ENABLED_PREFIX = "sms_enabled_user_";
    private static final String KEY_SMS_PHONE_NUMBER_PREFIX = "sms_phone_number_user_";
    private static final String KEY_LAST_GOAL_NOTIFIED_PREFIX = "last_goal_notified_user_";

    private static final long NO_GOAL_NOTIFICATION = Long.MIN_VALUE;

    private LegacyPreferencesImporter() {
    }

    public static void importIfNeeded(@NonNull Context context, @NonNull AppDatabase database) {
        Context appContext = context.getApplicationContext();
        if (SessionManager.isUserSettingsImportComplete(appContext)) {
            return;
        }

        synchronized (IMPORT_LOCK) {
            if (SessionManager.isUserSettingsImportComplete(appContext)) {
                return;
            }

            SharedPreferences preferences =
                    appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

            database.runInTransaction(() -> importMissingUserSettings(database, preferences));

            // Remove the old keys so settings only live in one place
            if (!removeLegacyKeys(preferences, database.userDao().getAllUserIds())) {
                return;
            }

            // Only persist the completion flag after both import and cleanup succeed
            SessionManager.markUserSettingsImportComplete(appContext);
        }
    }

    private static void importMissingUserSettings(
            AppDatabase database,
            SharedPreferences preferences
    ) {
        List<Long> userIds = database.userDao().getAllUserIds();
        for (Long userId : userIds) {
            if (userId == null || userId <= 0L) {
                continue;
            }

            if (database.userSettingsDao().getByUserId(userId) != null) {
                continue;
            }

            boolean smsEnabled = preferences.getBoolean(keyForUser(KEY_SMS_ENABLED_PREFIX, userId), false);
            String phoneNumber = preferences.getString(keyForUser(KEY_SMS_PHONE_NUMBER_PREFIX, userId), "");
            long goalBits = preferences.getLong(
                    keyForUser(KEY_LAST_GOAL_NOTIFIED_PREFIX, userId),
                    NO_GOAL_NOTIFICATION
            );

            // Strip non-digit characters from legacy phone numbers so the stored
            // value matches what the repository would produce for new entries
            database.userSettingsDao().upsert(new UserSettingsEntity(
                    userId,
                    smsEnabled,
                    SmsUtil.sanitizePhoneNumber(phoneNumber),
                    goalBits == NO_GOAL_NOTIFICATION ? null : goalBits
            ));
        }
    }

    private static boolean removeLegacyKeys(SharedPreferences preferences, List<Long> userIds) {
        SharedPreferences.Editor editor = preferences.edit();
        for (Long userId : userIds) {
            if (userId == null || userId <= 0L) {
                continue;
            }
            editor.remove(keyForUser(KEY_SMS_ENABLED_PREFIX, userId));
            editor.remove(keyForUser(KEY_SMS_PHONE_NUMBER_PREFIX, userId));
            editor.remove(keyForUser(KEY_LAST_GOAL_NOTIFIED_PREFIX, userId));
        }
        return editor.commit();
    }

    private static String keyForUser(String prefix, long userId) {
        return prefix + userId;
    }
}
