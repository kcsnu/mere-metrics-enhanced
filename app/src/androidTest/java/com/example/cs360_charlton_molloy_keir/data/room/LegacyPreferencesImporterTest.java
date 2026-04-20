package com.example.cs360_charlton_molloy_keir.data.room;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.cs360_charlton_molloy_keir.data.room.entity.UserEntity;
import com.example.cs360_charlton_molloy_keir.data.room.entity.UserSettingsEntity;
import com.example.cs360_charlton_molloy_keir.util.SessionManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Verifies one-time import of legacy user settings from SharedPreferences into Room. */
@RunWith(AndroidJUnit4.class)
public class LegacyPreferencesImporterTest {

    private static final String PREFS_NAME = "cs360_prefs";
    private static final String KEY_CURRENT_USER_ID = "current_user_id";
    private static final String KEY_USER_SETTINGS_IMPORT_COMPLETE = "user_settings_import_complete";
    private static final String KEY_SMS_ENABLED_PREFIX = "sms_enabled_user_";
    private static final String KEY_SMS_PHONE_NUMBER_PREFIX = "sms_phone_number_user_";
    private static final String KEY_LAST_GOAL_NOTIFIED_PREFIX = "last_goal_notified_user_";

    private Context appContext;
    private SharedPreferences preferences;
    private AppDatabase database;

    @Before
    public void setUp() {
        appContext = ApplicationProvider.getApplicationContext();
        preferences = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        preferences.edit().clear().commit();

        database = Room.inMemoryDatabaseBuilder(appContext, AppDatabase.class)
                .allowMainThreadQueries()
                .build();
    }

    @After
    public void tearDown() {
        if (database != null) {
            database.close();
        }
        preferences.edit().clear().commit();
    }

    @Test
    public void importIfNeeded_copiesLegacySettingsIntoRoom_andMarksComplete() {
        long firstUserId = createUser("import_user_one");
        long secondUserId = createUser("import_user_two");

        preferences.edit()
                .putLong(KEY_CURRENT_USER_ID, firstUserId)
                .putBoolean(keyForUser(KEY_SMS_ENABLED_PREFIX, firstUserId), true)
                .putString(keyForUser(KEY_SMS_PHONE_NUMBER_PREFIX, firstUserId), " (555) 123-4567 ")
                .putLong(
                        keyForUser(KEY_LAST_GOAL_NOTIFIED_PREFIX, firstUserId),
                        Double.doubleToLongBits(150.0)
                )
                .commit();

        LegacyPreferencesImporter.importIfNeeded(appContext, database);

        UserSettingsEntity firstUserSettings = database.userSettingsDao().getByUserId(firstUserId);
        assertNotNull(firstUserSettings);
        assertTrue(firstUserSettings.smsEnabled);
        assertEquals("5551234567", firstUserSettings.smsPhoneNumber);
        assertEquals(Long.valueOf(Double.doubleToLongBits(150.0)), firstUserSettings.lastGoalNotifiedBits);

        UserSettingsEntity secondUserSettings = database.userSettingsDao().getByUserId(secondUserId);
        assertNotNull(secondUserSettings);
        assertFalse(secondUserSettings.smsEnabled);
        assertEquals("", secondUserSettings.smsPhoneNumber);
        assertNull(secondUserSettings.lastGoalNotifiedBits);

        assertTrue(SessionManager.isUserSettingsImportComplete(appContext));
        assertFalse(preferences.contains(keyForUser(KEY_SMS_ENABLED_PREFIX, firstUserId)));
        assertFalse(preferences.contains(keyForUser(KEY_SMS_PHONE_NUMBER_PREFIX, firstUserId)));
        assertFalse(preferences.contains(keyForUser(KEY_LAST_GOAL_NOTIFIED_PREFIX, firstUserId)));
        assertTrue(preferences.contains(KEY_CURRENT_USER_ID));
        assertEquals(firstUserId, preferences.getLong(KEY_CURRENT_USER_ID, SessionManager.NO_LOGGED_IN_USER));
        assertEquals(firstUserId, SessionManager.getLoggedInUserId(appContext));
    }

    @Test
    public void importIfNeeded_isSafeToRerunWhenFlagIsMissing() {
        long userId = createUser("rerun_user");

        preferences.edit()
                .putLong(KEY_CURRENT_USER_ID, userId)
                .putBoolean(keyForUser(KEY_SMS_ENABLED_PREFIX, userId), true)
                .putString(keyForUser(KEY_SMS_PHONE_NUMBER_PREFIX, userId), "555-111-2222")
                .putLong(
                        keyForUser(KEY_LAST_GOAL_NOTIFIED_PREFIX, userId),
                        Double.doubleToLongBits(140.0)
                )
                .commit();

        LegacyPreferencesImporter.importIfNeeded(appContext, database);

        database.userSettingsDao().upsert(new UserSettingsEntity(
                userId,
                false,
                "5559990000",
                Double.doubleToLongBits(135.0)
        ));

        preferences.edit().remove(KEY_USER_SETTINGS_IMPORT_COMPLETE).commit();
        LegacyPreferencesImporter.importIfNeeded(appContext, database);

        UserSettingsEntity settings = database.userSettingsDao().getByUserId(userId);
        assertNotNull(settings);
        assertFalse(settings.smsEnabled);
        assertEquals("5559990000", settings.smsPhoneNumber);
        assertEquals(Long.valueOf(Double.doubleToLongBits(135.0)), settings.lastGoalNotifiedBits);
        assertTrue(SessionManager.isUserSettingsImportComplete(appContext));
        assertFalse(preferences.contains(keyForUser(KEY_SMS_ENABLED_PREFIX, userId)));
        assertFalse(preferences.contains(keyForUser(KEY_SMS_PHONE_NUMBER_PREFIX, userId)));
        assertFalse(preferences.contains(keyForUser(KEY_LAST_GOAL_NOTIFIED_PREFIX, userId)));
        assertTrue(preferences.contains(KEY_CURRENT_USER_ID));
        assertEquals(userId, preferences.getLong(KEY_CURRENT_USER_ID, SessionManager.NO_LOGGED_IN_USER));
        assertEquals(userId, SessionManager.getLoggedInUserId(appContext));
    }

    private long createUser(String username) {
        return database.userDao().insert(
                new UserEntity(username, "salt-" + username, "hash-" + username)
        );
    }

    private static String keyForUser(String prefix, long userId) {
        return prefix + userId;
    }
}
