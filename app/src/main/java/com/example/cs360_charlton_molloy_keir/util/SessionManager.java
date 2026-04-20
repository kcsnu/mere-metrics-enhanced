package com.example.cs360_charlton_molloy_keir.util;

import android.content.Context;
import android.content.SharedPreferences;

/** Stores session state plus the one-time legacy-settings import flag in SharedPreferences. */
public final class SessionManager {

    private static final String PREFS_NAME = "cs360_prefs";
    private static final String KEY_USER_ID = "current_user_id";
    private static final String KEY_USER_SETTINGS_IMPORT_COMPLETE = "user_settings_import_complete";

    public static final long NO_LOGGED_IN_USER = -1L;

    private SessionManager() {
    }

    private static SharedPreferences getPreferences(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static void storeLoggedInUser(Context context, long userId) {
        getPreferences(context).edit()
                .putLong(KEY_USER_ID, userId)
                .apply();
    }

    public static long getLoggedInUserId(Context context) {
        return getPreferences(context).getLong(KEY_USER_ID, NO_LOGGED_IN_USER);
    }

    public static boolean isUserSettingsImportComplete(Context context) {
        return getPreferences(context).getBoolean(KEY_USER_SETTINGS_IMPORT_COMPLETE, false);
    }

    public static boolean markUserSettingsImportComplete(Context context) {
        return getPreferences(context).edit()
                .putBoolean(KEY_USER_SETTINGS_IMPORT_COMPLETE, true)
                .commit();
    }
}
