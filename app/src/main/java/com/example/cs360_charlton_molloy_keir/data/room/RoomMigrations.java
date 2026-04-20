package com.example.cs360_charlton_molloy_keir.data.room;

import android.database.Cursor;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

/** Room migrations for the app database */
public final class RoomMigrations {

    // v2 to v3
    // Rebuild the original tables so Room sees the expected schema.
    public static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Turn off FK checks while tables are rebuilt
            database.execSQL("PRAGMA foreign_keys=OFF");

            // Users
            database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `Users_new` ("
                            + "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                            + "`username` TEXT NOT NULL, "
                            + "`password_salt` TEXT NOT NULL, "
                            + "`password_hash` TEXT NOT NULL)"
            );

            // Goal weight
            database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `GoalWeight_new` ("
                            + "`user_id` INTEGER NOT NULL, "
                            + "`goal_weight` REAL NOT NULL, "
                            + "PRIMARY KEY(`user_id`), "
                            + "FOREIGN KEY(`user_id`) REFERENCES `Users`(`id`) "
                            + "ON UPDATE NO ACTION ON DELETE CASCADE)"
            );

            // Daily weights
            database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `DailyWeights_new` ("
                            + "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                            + "`user_id` INTEGER NOT NULL, "
                            + "`entry_date` TEXT NOT NULL, "
                            + "`weight` REAL NOT NULL, "
                            + "FOREIGN KEY(`user_id`) REFERENCES `Users`(`id`) "
                            + "ON UPDATE NO ACTION ON DELETE CASCADE)"
            );

            // User settings
            database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `UserSettings` ("
                            + "`user_id` INTEGER NOT NULL, "
                            + "`sms_enabled` INTEGER NOT NULL, "
                            + "`sms_phone_number` TEXT NOT NULL, "
                            + "`last_goal_notified_bits` INTEGER, "
                            + "PRIMARY KEY(`user_id`), "
                            + "FOREIGN KEY(`user_id`) REFERENCES `Users`(`id`) "
                            + "ON UPDATE NO ACTION ON DELETE CASCADE)"
            );

            database.execSQL(
                    "INSERT INTO `Users_new` (`id`, `username`, `password_salt`, `password_hash`) "
                            + "SELECT `id`, `username`, `password_salt`, `password_hash` "
                            + "FROM `Users`"
            );

            database.execSQL(
                    "INSERT INTO `GoalWeight_new` (`user_id`, `goal_weight`) "
                            + "SELECT `user_id`, `goal_weight` "
                            + "FROM `GoalWeight`"
            );

            // Convert dates to yyyy-MM-dd and keep the newest row for each user/date.
            database.execSQL(
                    "INSERT INTO `DailyWeights_new` (`id`, `user_id`, `entry_date`, `weight`) "
                            + "SELECT dw.`id`, dw.`user_id`, "
                            + "substr(dw.`entry_date`, 7, 4) || '-' || "
                            + "substr(dw.`entry_date`, 1, 2) || '-' || "
                            + "substr(dw.`entry_date`, 4, 2) AS `entry_date`, "
                            + "dw.`weight` "
                            + "FROM `DailyWeights` dw "
                            + "INNER JOIN ("
                            + "SELECT `user_id`, "
                            + "substr(`entry_date`, 7, 4) || '-' || "
                            + "substr(`entry_date`, 1, 2) || '-' || "
                            + "substr(`entry_date`, 4, 2) AS `iso_entry_date`, "
                            + "MAX(`id`) AS `kept_id` "
                            + "FROM `DailyWeights` "
                            + "GROUP BY `user_id`, `iso_entry_date`"
                            + ") dedup "
                            + "ON dw.`id` = dedup.`kept_id`"
            );

            database.execSQL("DROP TABLE IF EXISTS `DailyWeights`");
            database.execSQL("DROP TABLE IF EXISTS `GoalWeight`");
            database.execSQL("DROP TABLE IF EXISTS `Users`");

            database.execSQL("ALTER TABLE `Users_new` RENAME TO `Users`");
            database.execSQL("ALTER TABLE `GoalWeight_new` RENAME TO `GoalWeight`");
            database.execSQL("ALTER TABLE `DailyWeights_new` RENAME TO `DailyWeights`");

            createCurrentIndices(database);
            // Turn FK checks back on
            database.execSQL("PRAGMA foreign_keys=ON");
        }
    };

    // v3 to v4
    // Rebuild the current tables and copy any data that is still present.
    public static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("PRAGMA foreign_keys=OFF");

            database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `Users_new` ("
                            + "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                            + "`username` TEXT NOT NULL, "
                            + "`password_salt` TEXT NOT NULL, "
                            + "`password_hash` TEXT NOT NULL)"
            );

            database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `GoalWeight_new` ("
                            + "`user_id` INTEGER NOT NULL, "
                            + "`goal_weight` REAL NOT NULL, "
                            + "PRIMARY KEY(`user_id`), "
                            + "FOREIGN KEY(`user_id`) REFERENCES `Users`(`id`) "
                            + "ON UPDATE NO ACTION ON DELETE CASCADE)"
            );

            database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `DailyWeights_new` ("
                            + "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                            + "`user_id` INTEGER NOT NULL, "
                            + "`entry_date` TEXT NOT NULL, "
                            + "`weight` REAL NOT NULL, "
                            + "FOREIGN KEY(`user_id`) REFERENCES `Users`(`id`) "
                            + "ON UPDATE NO ACTION ON DELETE CASCADE)"
            );

            database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `UserSettings_new` ("
                            + "`user_id` INTEGER NOT NULL, "
                            + "`sms_enabled` INTEGER NOT NULL, "
                            + "`sms_phone_number` TEXT NOT NULL, "
                            + "`last_goal_notified_bits` INTEGER, "
                            + "PRIMARY KEY(`user_id`), "
                            + "FOREIGN KEY(`user_id`) REFERENCES `Users`(`id`) "
                            + "ON UPDATE NO ACTION ON DELETE CASCADE)"
            );

            // Copy Users if the old table is still there
            if (tableExists(database, "Users")) {
                database.execSQL(
                        "INSERT INTO `Users_new` (`id`, `username`, `password_salt`, `password_hash`) "
                                + "SELECT `id`, `username`, `password_salt`, `password_hash` "
                                + "FROM `Users`"
                );
            }

            if (tableExists(database, "GoalWeight")) {
                database.execSQL(
                        "INSERT INTO `GoalWeight_new` (`user_id`, `goal_weight`) "
                                + "SELECT `user_id`, `goal_weight` "
                                + "FROM `GoalWeight`"
                );
            }

            // Keep the newest DailyWeights row for each user/date
            if (tableExists(database, "DailyWeights")) {
                database.execSQL(
                        "INSERT INTO `DailyWeights_new` (`id`, `user_id`, `entry_date`, `weight`) "
                                + "SELECT dw.`id`, dw.`user_id`, dw.`entry_date`, dw.`weight` "
                                + "FROM `DailyWeights` dw "
                                + "INNER JOIN ("
                                + "SELECT `user_id`, `entry_date`, MAX(`id`) AS `kept_id` "
                                + "FROM `DailyWeights` "
                                + "GROUP BY `user_id`, `entry_date`"
                                + ") dedup "
                                + "ON dw.`id` = dedup.`kept_id`"
                );
            }

            // Copy UserSettings with defaults for missing columns
            copyUserSettingsIfPresent(database);

            database.execSQL("DROP TABLE IF EXISTS `UserSettings`");
            database.execSQL("DROP TABLE IF EXISTS `DailyWeights`");
            database.execSQL("DROP TABLE IF EXISTS `GoalWeight`");
            database.execSQL("DROP TABLE IF EXISTS `Users`");

            database.execSQL("ALTER TABLE `Users_new` RENAME TO `Users`");
            database.execSQL("ALTER TABLE `GoalWeight_new` RENAME TO `GoalWeight`");
            database.execSQL("ALTER TABLE `DailyWeights_new` RENAME TO `DailyWeights`");
            database.execSQL("ALTER TABLE `UserSettings_new` RENAME TO `UserSettings`");

            createCurrentIndices(database);
            database.execSQL("PRAGMA foreign_keys=ON");
        }
    };

    private RoomMigrations() {
    }

    private static void copyUserSettingsIfPresent(SupportSQLiteDatabase database) {
        if (!tableExists(database, "UserSettings")) {
            return;
        }

        String smsEnabledExpression = columnExists(database, "UserSettings", "sms_enabled")
                ? "`sms_enabled`"
                : "0";
        String smsPhoneExpression = columnExists(database, "UserSettings", "sms_phone_number")
                ? "COALESCE(`sms_phone_number`, '')"
                : "''";
        String lastGoalBitsExpression = columnExists(database, "UserSettings", "last_goal_notified_bits")
                ? "`last_goal_notified_bits`"
                : "NULL";

        database.execSQL(
                "INSERT INTO `UserSettings_new` "
                        + "(`user_id`, `sms_enabled`, `sms_phone_number`, `last_goal_notified_bits`) "
                        + "SELECT `user_id`, "
                        + smsEnabledExpression + ", "
                        + smsPhoneExpression + ", "
                        + lastGoalBitsExpression + " "
                        + "FROM `UserSettings`"
        );
    }

    private static void createCurrentIndices(SupportSQLiteDatabase database) {
        database.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_Users_username` "
                        + "ON `Users` (`username`)"
        );
        database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_DailyWeights_user_id` "
                        + "ON `DailyWeights` (`user_id`)"
        );
        database.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_DailyWeights_user_id_entry_date` "
                        + "ON `DailyWeights` (`user_id`, `entry_date`)"
        );
    }

    private static boolean tableExists(SupportSQLiteDatabase database, String tableName) {
        try (Cursor cursor = database.query(
                "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = '" + tableName + "'"
        )) {
            return cursor.moveToFirst();
        }
    }

    private static boolean columnExists(
            SupportSQLiteDatabase database,
            String tableName,
            String columnName
    ) {
        try (Cursor cursor = database.query("PRAGMA table_info(`" + tableName + "`)")) {
            while (cursor.moveToNext()) {
                String existingColumnName = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                if (columnName.equals(existingColumnName)) {
                    return true;
                }
            }
            return false;
        }
    }
}
