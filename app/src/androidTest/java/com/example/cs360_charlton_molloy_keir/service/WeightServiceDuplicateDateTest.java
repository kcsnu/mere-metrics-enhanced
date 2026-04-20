package com.example.cs360_charlton_molloy_keir.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.cs360_charlton_molloy_keir.data.room.AppDatabase;
import com.example.cs360_charlton_molloy_keir.data.room.AppDatabaseProvider;
import com.example.cs360_charlton_molloy_keir.data.room.entity.DailyWeightEntity;
import com.example.cs360_charlton_molloy_keir.data.room.entity.UserEntity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Covers duplicate-date and failure cases in WeightService. */
@RunWith(AndroidJUnit4.class)
public class WeightServiceDuplicateDateTest {

    private Context appContext;
    private AppDatabase database;
    private WeightService weightService;

    @Before
    public void setUp() {
        appContext = ApplicationProvider.getApplicationContext();
        database = AppDatabaseProvider.getInstance(appContext);
        clearDatabase();
        weightService = new WeightService(appContext);
    }

    @After
    public void tearDown() {
        clearDatabase();
        weightService.close();
    }

    @Test
    public void addEntry_returnsDuplicateDateWhenUserAlreadyHasEntryForThatDay() {
        long userId = createUser("duplicate_add_user");
        database.dailyWeightDao().insert(new DailyWeightEntity(userId, "2026-03-05", 200.0));

        WeightService.AddEntryResult result = weightService.addEntry(
                userId,
                "03/05/2026",
                "199.0"
        );

        assertEquals(WeightService.AddEntryStatus.DUPLICATE_DATE, result.getStatus());
        assertEquals("03/05/2026", result.getResolvedDate());
        assertEquals(1, database.dailyWeightDao().getByUserIdNewestFirst(userId).size());
        assertEquals(
                "2026-03-05",
                database.dailyWeightDao().getByUserIdNewestFirst(userId).get(0).entryDate
        );
        assertEquals(
                200.0,
                database.dailyWeightDao().getByUserIdNewestFirst(userId).get(0).weight,
                0.0001
        );
    }

    @Test
    public void updateEntry_returnsDuplicateDateWhenMovingOntoExistingDay() {
        long userId = createUser("duplicate_update_user");
        long firstEntryId = database.dailyWeightDao().insert(
                new DailyWeightEntity(userId, "2026-03-05", 200.0)
        );
        long secondEntryId = database.dailyWeightDao().insert(
                new DailyWeightEntity(userId, "2026-03-06", 199.0)
        );

        WeightService.UpdateEntryResult result = weightService.updateEntry(
                userId,
                secondEntryId,
                "03/05/2026",
                "198.0"
        );

        assertEquals(WeightService.UpdateEntryStatus.DUPLICATE_DATE, result.getStatus());
        assertEquals(
                "2026-03-05",
                database.dailyWeightDao().getByIdForUser(userId, firstEntryId).entryDate
        );
        assertEquals(
                "2026-03-06",
                database.dailyWeightDao().getByIdForUser(userId, secondEntryId).entryDate
        );
        assertEquals(
                199.0,
                database.dailyWeightDao().getByIdForUser(userId, secondEntryId).weight,
                0.0001
        );
    }

    @Test
    public void updateEntry_allowsKeepingSameRowOnSameDate() {
        long userId = createUser("same_row_update_user");
        long entryId = database.dailyWeightDao().insert(
                new DailyWeightEntity(userId, "2026-03-05", 200.0)
        );

        WeightService.UpdateEntryResult result = weightService.updateEntry(
                userId,
                entryId,
                "03/05/2026",
                "198.5"
        );

        assertEquals(WeightService.UpdateEntryStatus.SUCCESS, result.getStatus());
        assertEquals(
                "2026-03-05",
                database.dailyWeightDao().getByIdForUser(userId, entryId).entryDate
        );
        assertEquals(
                198.5,
                database.dailyWeightDao().getByIdForUser(userId, entryId).weight,
                0.0001
        );
    }

    @Test
    public void updateEntry_returnsUpdateFailedWhenRowDoesNotExist() {
        long userId = createUser("missing_row_update_user");

        WeightService.UpdateEntryResult result = weightService.updateEntry(
                userId,
                9999L,
                "03/05/2026",
                "198.5"
        );

        assertEquals(WeightService.UpdateEntryStatus.UPDATE_FAILED, result.getStatus());
    }

    @Test
    public void updateEntry_allowsExactNoOpForSameRowSameDateAndWeight() {
        long userId = createUser("same_row_no_op_user");
        long entryId = database.dailyWeightDao().insert(
                new DailyWeightEntity(userId, "2026-03-05", 200.0)
        );

        WeightService.UpdateEntryResult result = weightService.updateEntry(
                userId,
                entryId,
                "03/05/2026",
                "200.0"
        );

        assertEquals(WeightService.UpdateEntryStatus.SUCCESS, result.getStatus());
        assertEquals(
                "2026-03-05",
                database.dailyWeightDao().getByIdForUser(userId, entryId).entryDate
        );
        assertEquals(
                200.0,
                database.dailyWeightDao().getByIdForUser(userId, entryId).weight,
                0.0001
        );
    }

    @Test
    public void addEntry_returnsSaveFailedWhenInsertFailsForNonexistentUser() {
        WeightService.AddEntryResult result = weightService.addEntry(
                9999L,
                "03/05/2026",
                "199.0"
        );

        assertEquals(WeightService.AddEntryStatus.SAVE_FAILED, result.getStatus());
        assertEquals("03/05/2026", result.getResolvedDate());
    }

    @Test
    public void serviceValidation_rejectsNaNAndInfinity() {
        long userId = createUser("non_finite_validation_user");
        long entryId = database.dailyWeightDao().insert(
                new DailyWeightEntity(userId, "2026-03-05", 200.0)
        );

        assertEquals(
                WeightService.GoalSaveStatus.NOT_NUMBER,
                weightService.saveGoal(userId, "NaN").getStatus()
        );
        assertNull(database.goalWeightDao().getByUserId(userId));

        assertEquals(
                WeightService.AddEntryStatus.WEIGHT_NOT_NUMBER,
                weightService.addEntry(userId, "03/06/2026", "Infinity").getStatus()
        );
        assertEquals(1, database.dailyWeightDao().getByUserIdNewestFirst(userId).size());

        assertEquals(
                WeightService.UpdateEntryStatus.WEIGHT_NOT_NUMBER,
                weightService.updateEntry(userId, entryId, "03/05/2026", "-Infinity").getStatus()
        );
        assertEquals(
                200.0,
                database.dailyWeightDao().getByIdForUser(userId, entryId).weight,
                0.0001
        );
    }

    private long createUser(String username) {
        return database.userDao().insert(
                new UserEntity(username, "salt-" + username, "hash-" + username)
        );
    }

    private void clearDatabase() {
        database.getOpenHelper().getWritableDatabase().execSQL("DELETE FROM `UserSettings`");
        database.getOpenHelper().getWritableDatabase().execSQL("DELETE FROM `GoalWeight`");
        database.getOpenHelper().getWritableDatabase().execSQL("DELETE FROM `DailyWeights`");
        database.getOpenHelper().getWritableDatabase().execSQL("DELETE FROM `Users`");
    }
}
