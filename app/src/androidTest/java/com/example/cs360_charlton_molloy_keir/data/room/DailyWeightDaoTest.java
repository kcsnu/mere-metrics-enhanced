package com.example.cs360_charlton_molloy_keir.data.room;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

import android.database.sqlite.SQLiteConstraintException;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.cs360_charlton_molloy_keir.data.room.entity.DailyWeightEntity;
import com.example.cs360_charlton_molloy_keir.data.room.entity.GoalWeightEntity;
import com.example.cs360_charlton_molloy_keir.data.room.entity.UserEntity;
import com.example.cs360_charlton_molloy_keir.data.room.entity.UserSettingsEntity;
import com.example.cs360_charlton_molloy_keir.support.AnalyticsScenarioFixtures;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Verifies DAO-level enforcement of one daily weight row per user/date. */
@RunWith(AndroidJUnit4.class)
public class DailyWeightDaoTest {

    private AppDatabase database;

    @Before
    public void setUp() {
        database = Room.inMemoryDatabaseBuilder(
                        ApplicationProvider.getApplicationContext(),
                        AppDatabase.class
                )
                .allowMainThreadQueries()
                .build();
    }

    @After
    public void tearDown() {
        if (database != null) {
            database.close();
        }
    }

    @Test
    public void insert_returnsMinusOneWhenDateAlreadyExistsForSameUser() {
        long userId = database.userDao().insert(
                new UserEntity("alice", "salt-a", "hash-a")
        );

        long firstInsertId = database.dailyWeightDao().insert(
                new DailyWeightEntity(userId, "2026-03-05", 200.0)
        );
        long duplicateInsertId = database.dailyWeightDao().insert(
                new DailyWeightEntity(userId, "2026-03-05", 199.0)
        );

        assertEquals(1L, firstInsertId);
        assertEquals(-1L, duplicateInsertId);
        assertEquals(1, database.dailyWeightDao().getByUserIdNewestFirst(userId).size());
        assertEquals(
                200.0,
                database.dailyWeightDao().getByUserIdNewestFirst(userId).get(0).weight,
                0.0001
        );
    }

    @Test
    public void insert_allowsSameDateForDifferentUsers() {
        long firstUserId = database.userDao().insert(
                new UserEntity("alice", "salt-a", "hash-a")
        );
        long secondUserId = database.userDao().insert(
                new UserEntity("bob", "salt-b", "hash-b")
        );

        database.dailyWeightDao().insert(
                new DailyWeightEntity(firstUserId, "2026-03-05", 200.0)
        );
        database.dailyWeightDao().insert(
                new DailyWeightEntity(secondUserId, "2026-03-05", 180.0)
        );

        assertEquals(1, database.dailyWeightDao().getByUserIdNewestFirst(firstUserId).size());
        assertEquals(1, database.dailyWeightDao().getByUserIdNewestFirst(secondUserId).size());
    }

    @Test
    public void insert_stillThrowsForeignKeyViolationForMissingUser() {
        assertThrows(
                SQLiteConstraintException.class,
                () -> database.dailyWeightDao().insert(
                        new DailyWeightEntity(9999L, "2026-03-05", 200.0)
                )
        );
    }

    @Test
    public void update_returnsZeroWhenMovingOntoExistingDateForSameUser() {
        long userId = database.userDao().insert(
                new UserEntity("alice", "salt-a", "hash-a")
        );

        long firstEntryId = database.dailyWeightDao().insert(
                new DailyWeightEntity(userId, "2026-03-05", 200.0)
        );
        long secondEntryId = database.dailyWeightDao().insert(
                new DailyWeightEntity(userId, "2026-03-06", 199.0)
        );

        DailyWeightEntity updatedRow = new DailyWeightEntity(
                secondEntryId,
                userId,
                "2026-03-05",
                199.0
        );

        assertEquals(0, database.dailyWeightDao().update(updatedRow));

        assertEquals(
                "2026-03-05",
                database.dailyWeightDao().getByIdForUser(userId, firstEntryId).entryDate
        );
        assertEquals(
                "2026-03-06",
                database.dailyWeightDao().getByIdForUser(userId, secondEntryId).entryDate
        );
    }

    @Test
    public void update_allowsKeepingSameRowOnSameDate() {
        long userId = database.userDao().insert(
                new UserEntity("same-row", "salt-same", "hash-same")
        );

        long entryId = database.dailyWeightDao().insert(
                new DailyWeightEntity(userId, "2026-03-05", 200.0)
        );

        DailyWeightEntity updatedRow = new DailyWeightEntity(
                entryId,
                userId,
                "2026-03-05",
                198.5
        );

        assertEquals(1, database.dailyWeightDao().update(updatedRow));
        assertEquals(
                198.5,
                database.dailyWeightDao().getByIdForUser(userId, entryId).weight,
                0.0001
        );
    }

    @Test
    public void deletingUser_cascadesToOwnedWeightsGoalAndSettings() {
        long deletedUserId = database.userDao().insert(
                new UserEntity("cascade-user", "salt-c", "hash-c")
        );
        long survivingUserId = database.userDao().insert(
                new UserEntity("surviving-user", "salt-s", "hash-s")
        );

        database.goalWeightDao().upsert(new GoalWeightEntity(deletedUserId, 180.0));
        database.userSettingsDao().upsert(
                new UserSettingsEntity(deletedUserId, true, "5551112222", null)
        );
        database.dailyWeightDao().insert(
                new DailyWeightEntity(deletedUserId, "2026-03-05", 200.0)
        );
        database.dailyWeightDao().insert(
                new DailyWeightEntity(survivingUserId, "2026-03-05", 175.0)
        );

        database.getOpenHelper().getWritableDatabase().execSQL(
                "DELETE FROM `Users` WHERE `id` = " + deletedUserId
        );

        assertEquals(0, database.dailyWeightDao().getByUserIdNewestFirst(deletedUserId).size());
        assertNull(database.goalWeightDao().getByUserId(deletedUserId));
        assertNull(database.userSettingsDao().getByUserId(deletedUserId));
        assertEquals(1, database.dailyWeightDao().getByUserIdNewestFirst(survivingUserId).size());
    }

    @Test
    public void getByUserIdOldestFirst_returnsAscendingIsoDateOrder() {
        long userId = AnalyticsScenarioFixtures.createUser(database, "ordered-oldest");
        AnalyticsScenarioFixtures.insertIsoWeights(
                database,
                userId,
                AnalyticsScenarioFixtures.orderedIsoOrderingScenario()
        );

        assertEquals(
                "2026-03-05",
                database.dailyWeightDao().getByUserIdOldestFirst(userId).get(0).entryDate
        );
        assertEquals(
                "2026-03-06",
                database.dailyWeightDao().getByUserIdOldestFirst(userId).get(1).entryDate
        );
        assertEquals(
                "2026-03-08",
                database.dailyWeightDao().getByUserIdOldestFirst(userId).get(2).entryDate
        );
    }

    @Test
    public void getByUserIdNewestFirst_returnsDescendingIsoDateOrder() {
        long userId = AnalyticsScenarioFixtures.createUser(database, "ordered-newest");
        AnalyticsScenarioFixtures.insertIsoWeights(
                database,
                userId,
                AnalyticsScenarioFixtures.orderedIsoOrderingScenario()
        );

        assertEquals(
                "2026-03-08",
                database.dailyWeightDao().getByUserIdNewestFirst(userId).get(0).entryDate
        );
        assertEquals(
                "2026-03-06",
                database.dailyWeightDao().getByUserIdNewestFirst(userId).get(1).entryDate
        );
        assertEquals(
                "2026-03-05",
                database.dailyWeightDao().getByUserIdNewestFirst(userId).get(2).entryDate
        );
    }

    @Test
    public void getByUserIdBetweenDates_usesInclusiveBoundsAndStaysUserScoped() {
        long firstUserId = database.userDao().insert(
                new UserEntity("bounded-a", "salt-a", "hash-a")
        );
        long secondUserId = database.userDao().insert(
                new UserEntity("bounded-b", "salt-b", "hash-b")
        );

        database.dailyWeightDao().insert(
                new DailyWeightEntity(firstUserId, "2026-03-05", 200.0)
        );
        database.dailyWeightDao().insert(
                new DailyWeightEntity(firstUserId, "2026-03-06", 199.0)
        );
        database.dailyWeightDao().insert(
                new DailyWeightEntity(firstUserId, "2026-03-07", 198.0)
        );
        database.dailyWeightDao().insert(
                new DailyWeightEntity(secondUserId, "2026-03-06", 180.0)
        );

        assertEquals(
                2,
                database.dailyWeightDao().getByUserIdBetweenDates(
                        firstUserId,
                        "2026-03-05",
                        "2026-03-06"
                ).size()
        );
        assertEquals(
                "2026-03-05",
                database.dailyWeightDao().getByUserIdBetweenDates(
                        firstUserId,
                        "2026-03-05",
                        "2026-03-06"
                ).get(0).entryDate
        );
        assertEquals(
                "2026-03-06",
                database.dailyWeightDao().getByUserIdBetweenDates(
                        firstUserId,
                        "2026-03-05",
                        "2026-03-06"
                ).get(1).entryDate
        );
        assertEquals(
                1,
                database.dailyWeightDao().getByUserIdBetweenDates(
                        secondUserId,
                        "2026-03-05",
                        "2026-03-06"
                ).size()
        );
    }

    @Test
    public void getByUserIdBetweenDates_supportsOptionalBoundsWithoutLosingOrdering() {
        long firstUserId = database.userDao().insert(
                new UserEntity("optional-bounds-a", "salt-a", "hash-a")
        );
        long secondUserId = database.userDao().insert(
                new UserEntity("optional-bounds-b", "salt-b", "hash-b")
        );

        database.dailyWeightDao().insert(
                new DailyWeightEntity(firstUserId, "2026-03-05", 200.0)
        );
        database.dailyWeightDao().insert(
                new DailyWeightEntity(firstUserId, "2026-03-06", 199.0)
        );
        database.dailyWeightDao().insert(
                new DailyWeightEntity(firstUserId, "2026-03-07", 198.0)
        );
        database.dailyWeightDao().insert(
                new DailyWeightEntity(secondUserId, "2026-03-06", 180.0)
        );

        assertEquals(
                2,
                database.dailyWeightDao().getByUserIdBetweenDates(
                        firstUserId,
                        null,
                        "2026-03-06"
                ).size()
        );
        assertEquals(
                "2026-03-05",
                database.dailyWeightDao().getByUserIdBetweenDates(
                        firstUserId,
                        null,
                        "2026-03-06"
                ).get(0).entryDate
        );
        assertEquals(
                "2026-03-06",
                database.dailyWeightDao().getByUserIdBetweenDates(
                        firstUserId,
                        null,
                        "2026-03-06"
                ).get(1).entryDate
        );
        assertEquals(
                2,
                database.dailyWeightDao().getByUserIdBetweenDates(
                        firstUserId,
                        "2026-03-06",
                        null
                ).size()
        );
        assertEquals(
                "2026-03-06",
                database.dailyWeightDao().getByUserIdBetweenDates(
                        firstUserId,
                        "2026-03-06",
                        null
                ).get(0).entryDate
        );
        assertEquals(
                "2026-03-07",
                database.dailyWeightDao().getByUserIdBetweenDates(
                        firstUserId,
                        "2026-03-06",
                        null
                ).get(1).entryDate
        );
        assertEquals(
                1,
                database.dailyWeightDao().getByUserIdBetweenDates(
                        secondUserId,
                        "2026-03-06",
                        null
                ).size()
        );
    }

    @Test
    public void queriesStayUserScopedAndPreserveIsoOrderingAcrossUsers() {
        long firstUserId = database.userDao().insert(
                new UserEntity("scoped-ordering-a", "salt-a", "hash-a")
        );
        long secondUserId = database.userDao().insert(
                new UserEntity("scoped-ordering-b", "salt-b", "hash-b")
        );

        database.dailyWeightDao().insert(
                new DailyWeightEntity(firstUserId, "2026-03-07", 198.0)
        );
        database.dailyWeightDao().insert(
                new DailyWeightEntity(firstUserId, "2026-03-05", 200.0)
        );
        database.dailyWeightDao().insert(
                new DailyWeightEntity(firstUserId, "2026-03-06", 199.0)
        );
        database.dailyWeightDao().insert(
                new DailyWeightEntity(secondUserId, "2026-03-07", 180.0)
        );
        database.dailyWeightDao().insert(
                new DailyWeightEntity(secondUserId, "2026-03-05", 182.0)
        );

        assertEquals(
                3,
                database.dailyWeightDao().getByUserIdOldestFirst(firstUserId).size()
        );
        assertEquals(
                2,
                database.dailyWeightDao().getByUserIdOldestFirst(secondUserId).size()
        );
        assertEquals(
                "2026-03-05",
                database.dailyWeightDao().getByUserIdOldestFirst(firstUserId).get(0).entryDate
        );
        assertEquals(
                "2026-03-07",
                database.dailyWeightDao().getByUserIdNewestFirst(firstUserId).get(0).entryDate
        );
        assertEquals(
                "2026-03-05",
                database.dailyWeightDao().getByUserIdOldestFirst(secondUserId).get(0).entryDate
        );
        assertEquals(
                "2026-03-07",
                database.dailyWeightDao().getByUserIdNewestFirst(secondUserId).get(0).entryDate
        );
    }
}
