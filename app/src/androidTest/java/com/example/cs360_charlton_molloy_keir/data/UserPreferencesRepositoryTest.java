package com.example.cs360_charlton_molloy_keir.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.cs360_charlton_molloy_keir.data.room.AppDatabase;
import com.example.cs360_charlton_molloy_keir.data.room.AppDatabaseProvider;
import com.example.cs360_charlton_molloy_keir.data.room.entity.UserEntity;
import com.example.cs360_charlton_molloy_keir.data.room.entity.UserSettingsEntity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Verifies targeted UserSettings DAO updates preserve other fields and create rows when needed. */
@RunWith(AndroidJUnit4.class)
public class UserPreferencesRepositoryTest {

    private static final String PREFS_NAME = "cs360_prefs";

    private Context appContext;
    private SharedPreferences preferences;
    private AppDatabase database;
    private UserPreferencesRepository repository;

    @Before
    public void setUp() {
        appContext = ApplicationProvider.getApplicationContext();
        preferences = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        preferences.edit().clear().commit();
        database = AppDatabaseProvider.getInstance(appContext);
        clearDatabase();
        repository = new UserPreferencesRepository(appContext);
    }

    @After
    public void tearDown() {
        clearDatabase();
        preferences.edit().clear().commit();
    }

    @Test
    public void targetedUpdates_preserveUnchangedFieldsWhenSettingsAlreadyExist() {
        long userId = createUser("preserve-user");
        Long initialGoalBits = Double.doubleToLongBits(160.0);

        database.userSettingsDao().upsert(
                new UserSettingsEntity(userId, false, "5551112222", initialGoalBits)
        );

        repository.setSmsEnabled(userId, true);
        UserSettingsEntity settings = database.userSettingsDao().getByUserId(userId);
        assertNotNull(settings);
        assertTrue(settings.smsEnabled);
        assertEquals("5551112222", settings.smsPhoneNumber);
        assertEquals(initialGoalBits, settings.lastGoalNotifiedBits);

        repository.setSmsPhoneNumber(userId, "(555) 999-0000");
        settings = database.userSettingsDao().getByUserId(userId);
        assertNotNull(settings);
        assertTrue(settings.smsEnabled);
        assertEquals("5559990000", settings.smsPhoneNumber);
        assertEquals(initialGoalBits, settings.lastGoalNotifiedBits);

        Long updatedGoalBits = Double.doubleToLongBits(150.0);
        repository.markGoalNotified(userId, 150.0);
        settings = database.userSettingsDao().getByUserId(userId);
        assertNotNull(settings);
        assertTrue(settings.smsEnabled);
        assertEquals("5559990000", settings.smsPhoneNumber);
        assertEquals(updatedGoalBits, settings.lastGoalNotifiedBits);

        repository.clearGoalNotified(userId);
        settings = database.userSettingsDao().getByUserId(userId);
        assertNotNull(settings);
        assertTrue(settings.smsEnabled);
        assertEquals("5559990000", settings.smsPhoneNumber);
        assertNull(settings.lastGoalNotifiedBits);
    }

    @Test
    public void targetedUpdates_createDefaultRowWhenSettingsDoNotExist() {
        long userId = createUser("fallback-user");

        repository.setSmsEnabled(userId, true);
        UserSettingsEntity settings = database.userSettingsDao().getByUserId(userId);
        assertNotNull(settings);
        assertTrue(settings.smsEnabled);
        assertEquals("", settings.smsPhoneNumber);
        assertNull(settings.lastGoalNotifiedBits);

        deleteUserSettings(userId);
        repository.setSmsPhoneNumber(userId, "(555) 222-3333");
        settings = database.userSettingsDao().getByUserId(userId);
        assertNotNull(settings);
        assertFalse(settings.smsEnabled);
        assertEquals("5552223333", settings.smsPhoneNumber);
        assertNull(settings.lastGoalNotifiedBits);

        deleteUserSettings(userId);
        Long goalBits = Double.doubleToLongBits(145.0);
        repository.markGoalNotified(userId, 145.0);
        settings = database.userSettingsDao().getByUserId(userId);
        assertNotNull(settings);
        assertFalse(settings.smsEnabled);
        assertEquals("", settings.smsPhoneNumber);
        assertEquals(goalBits, settings.lastGoalNotifiedBits);

        deleteUserSettings(userId);
        repository.clearGoalNotified(userId);
        settings = database.userSettingsDao().getByUserId(userId);
        assertNotNull(settings);
        assertFalse(settings.smsEnabled);
        assertEquals("", settings.smsPhoneNumber);
        assertNull(settings.lastGoalNotifiedBits);
    }

    private long createUser(String username) {
        return database.userDao().insert(new UserEntity(username, "salt-" + username, "hash-" + username));
    }

    private void deleteUserSettings(long userId) {
        database.getOpenHelper().getWritableDatabase()
                .execSQL("DELETE FROM `UserSettings` WHERE `user_id` = " + userId);
    }

    private void clearDatabase() {
        database.getOpenHelper().getWritableDatabase().execSQL("DELETE FROM `UserSettings`");
        database.getOpenHelper().getWritableDatabase().execSQL("DELETE FROM `GoalWeight`");
        database.getOpenHelper().getWritableDatabase().execSQL("DELETE FROM `DailyWeights`");
        database.getOpenHelper().getWritableDatabase().execSQL("DELETE FROM `Users`");
    }
}
