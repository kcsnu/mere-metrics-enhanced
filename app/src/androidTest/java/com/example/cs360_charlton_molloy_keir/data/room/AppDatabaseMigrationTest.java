package com.example.cs360_charlton_molloy_keir.data.room;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.cs360_charlton_molloy_keir.support.LegacyMigrationFixtures;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Verifies Room migrations preserve data across both helper and early Room database states. */
@RunWith(AndroidJUnit4.class)
public class AppDatabaseMigrationTest {

    private static final String TEST_DB = "room-migration-test";
    private static final String STALE_ROOM_V3_IDENTITY_HASH =
            "6b1bd36734ef7c13c9024f36b08df686";

    private Context appContext;

    @Before
    public void setUp() {
        appContext = ApplicationProvider.getApplicationContext();
        appContext.deleteDatabase(TEST_DB);
    }

    @After
    public void tearDown() {
        appContext.deleteDatabase(TEST_DB);
    }

    @Test
    public void migrate2To4_preservesData_convertsDates_andKeepsHighestIdPerUserDay() {
        SQLiteDatabase legacyDatabase = appContext.openOrCreateDatabase(
                TEST_DB,
                Context.MODE_PRIVATE,
                null
        );

        LegacyMigrationFixtures.createVersion2HelperSchema(legacyDatabase);
        LegacyMigrationFixtures.insertVersion2SeedData(legacyDatabase);
        legacyDatabase.setVersion(2);
        legacyDatabase.close();

        AppDatabase migratedDatabase = Room.databaseBuilder(
                        appContext,
                        AppDatabase.class,
                        TEST_DB
                )
                .addMigrations(
                        RoomMigrations.MIGRATION_2_3,
                        RoomMigrations.MIGRATION_3_4
                )
                .allowMainThreadQueries()
                .build();

        migratedDatabase.getOpenHelper().getWritableDatabase();

        assertEquals(2, longForQuery(migratedDatabase, "SELECT COUNT(*) FROM `Users`"));
        assertEquals(2, longForQuery(migratedDatabase, "SELECT COUNT(*) FROM `GoalWeight`"));
        assertEquals(3, longForQuery(migratedDatabase, "SELECT COUNT(*) FROM `DailyWeights`"));
        assertEquals(0, longForQuery(migratedDatabase, "SELECT COUNT(*) FROM `UserSettings`"));

        assertEquals(
                "2026-03-05",
                stringForQuery(
                        migratedDatabase,
                        "SELECT `entry_date` FROM `DailyWeights` WHERE `id` = 11"
                )
        );
        assertEquals(
                199.0,
                doubleForQuery(
                        migratedDatabase,
                        "SELECT `weight` FROM `DailyWeights` WHERE `id` = 11"
                ),
                0.0001
        );
        assertEquals(
                0,
                longForQuery(
                        migratedDatabase,
                        "SELECT COUNT(*) FROM `DailyWeights` WHERE `id` = 10"
                )
        );
        assertEquals(
                "2026-03-06",
                stringForQuery(
                        migratedDatabase,
                        "SELECT `entry_date` FROM `DailyWeights` WHERE `id` = 12"
                )
        );
        assertEquals(
                "2026-03-05",
                stringForQuery(
                        migratedDatabase,
                        "SELECT `entry_date` FROM `DailyWeights` WHERE `id` = 13"
                )
        );
        assertEquals(
                "2026-03-05",
                stringForQuery(
                        migratedDatabase,
                        "SELECT `entry_date` FROM `DailyWeights` "
                                + "WHERE `user_id` = 1 ORDER BY `entry_date` ASC, `id` ASC LIMIT 1"
                )
        );
        assertEquals(
                "2026-03-06",
                stringForQuery(
                        migratedDatabase,
                        "SELECT `entry_date` FROM `DailyWeights` "
                                + "WHERE `user_id` = 1 ORDER BY `entry_date` DESC, `id` DESC LIMIT 1"
                )
        );

        migratedDatabase.close();
    }

    @Test
    public void migrate3To4_upgradesEarlierRoomV3IdentityHash_andPreservesData() {
        SQLiteDatabase earlyRoomDatabase = appContext.openOrCreateDatabase(
                TEST_DB,
                Context.MODE_PRIVATE,
                null
        );

        LegacyMigrationFixtures.createEarlyRoomVersion3Schema(
                earlyRoomDatabase,
                STALE_ROOM_V3_IDENTITY_HASH
        );
        LegacyMigrationFixtures.insertEarlyRoomVersion3SeedData(earlyRoomDatabase);
        earlyRoomDatabase.setVersion(3);
        earlyRoomDatabase.close();

        AppDatabase migratedDatabase = Room.databaseBuilder(
                        appContext,
                        AppDatabase.class,
                        TEST_DB
                )
                .addMigrations(RoomMigrations.MIGRATION_3_4)
                .allowMainThreadQueries()
                .build();

        migratedDatabase.getOpenHelper().getWritableDatabase();

        assertEquals(1, longForQuery(migratedDatabase, "SELECT COUNT(*) FROM `Users`"));
        assertEquals(1, longForQuery(migratedDatabase, "SELECT COUNT(*) FROM `GoalWeight`"));
        assertEquals(2, longForQuery(migratedDatabase, "SELECT COUNT(*) FROM `DailyWeights`"));
        assertEquals(1, longForQuery(migratedDatabase, "SELECT COUNT(*) FROM `UserSettings`"));
        assertEquals(
                "5553334444",
                stringForQuery(
                        migratedDatabase,
                        "SELECT `sms_phone_number` FROM `UserSettings` WHERE `user_id` = 1"
                )
        );
        assertEquals(
                Double.doubleToLongBits(145.0),
                longForQuery(
                        migratedDatabase,
                        "SELECT `last_goal_notified_bits` FROM `UserSettings` WHERE `user_id` = 1"
                )
        );
        assertEquals(
                0,
                longForQuery(
                        migratedDatabase,
                        "SELECT COUNT(*) FROM `DailyWeights` WHERE `id` = 20"
                )
        );
        assertEquals(
                169.0,
                doubleForQuery(
                        migratedDatabase,
                        "SELECT `weight` FROM `DailyWeights` WHERE `id` = 21"
                ),
                0.0001
        );
        assertTrue(
                longForQuery(
                        migratedDatabase,
                        "SELECT COUNT(*) FROM sqlite_master "
                                + "WHERE type = 'index' "
                                + "AND name = 'index_DailyWeights_user_id_entry_date'"
                ) == 1
        );

        migratedDatabase.close();
    }

    private static long longForQuery(AppDatabase database, String query) {
        try (Cursor cursor = database.getOpenHelper().getWritableDatabase().query(query)) {
            cursor.moveToFirst();
            return cursor.getLong(0);
        }
    }

    private static double doubleForQuery(AppDatabase database, String query) {
        try (Cursor cursor = database.getOpenHelper().getWritableDatabase().query(query)) {
            cursor.moveToFirst();
            return cursor.getDouble(0);
        }
    }

    private static String stringForQuery(AppDatabase database, String query) {
        try (Cursor cursor = database.getOpenHelper().getWritableDatabase().query(query)) {
            cursor.moveToFirst();
            return cursor.getString(0);
        }
    }
}
