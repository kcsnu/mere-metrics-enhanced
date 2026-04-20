package com.example.cs360_charlton_molloy_keir.support;

import android.database.sqlite.SQLiteDatabase;

/** Raw SQL fixture helpers for legacy helper and early Room migration states. */
public final class LegacyMigrationFixtures {

    private LegacyMigrationFixtures() {
    }

    public static void createVersion2HelperSchema(SQLiteDatabase database) {
        database.execSQL(
                "CREATE TABLE IF NOT EXISTS `Users` ("
                        + "`id` INTEGER PRIMARY KEY AUTOINCREMENT, "
                        + "`username` TEXT NOT NULL UNIQUE, "
                        + "`password_salt` TEXT NOT NULL, "
                        + "`password_hash` TEXT NOT NULL)"
        );

        database.execSQL(
                "CREATE TABLE IF NOT EXISTS `DailyWeights` ("
                        + "`id` INTEGER PRIMARY KEY AUTOINCREMENT, "
                        + "`user_id` INTEGER NOT NULL, "
                        + "`entry_date` TEXT NOT NULL, "
                        + "`weight` REAL NOT NULL, "
                        + "FOREIGN KEY(`user_id`) REFERENCES `Users`(`id`))"
        );

        database.execSQL(
                "CREATE TABLE IF NOT EXISTS `GoalWeight` ("
                        + "`user_id` INTEGER PRIMARY KEY, "
                        + "`goal_weight` REAL NOT NULL, "
                        + "FOREIGN KEY(`user_id`) REFERENCES `Users`(`id`))"
        );
    }

    public static void insertVersion2SeedData(SQLiteDatabase database) {
        database.execSQL(
                "INSERT INTO `Users` (`id`, `username`, `password_salt`, `password_hash`) VALUES "
                        + "(1, 'alice', 'salt-a', 'hash-a'), "
                        + "(2, 'bob', 'salt-b', 'hash-b')"
        );
        database.execSQL(
                "INSERT INTO `GoalWeight` (`user_id`, `goal_weight`) VALUES "
                        + "(1, 150.0), "
                        + "(2, 175.0)"
        );
        database.execSQL(
                "INSERT INTO `DailyWeights` (`id`, `user_id`, `entry_date`, `weight`) VALUES "
                        + "(10, 1, '03/05/2026', 200.0), "
                        + "(11, 1, '03/05/2026', 199.0), "
                        + "(12, 1, '03/06/2026', 198.0), "
                        + "(13, 2, '03/05/2026', 176.0)"
        );
    }

    public static void createEarlyRoomVersion3Schema(SQLiteDatabase database, String identityHash) {
        database.execSQL(
                "CREATE TABLE IF NOT EXISTS `Users` ("
                        + "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                        + "`username` TEXT NOT NULL, "
                        + "`password_salt` TEXT NOT NULL, "
                        + "`password_hash` TEXT NOT NULL)"
        );
        database.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_Users_username` "
                        + "ON `Users` (`username`)"
        );

        database.execSQL(
                "CREATE TABLE IF NOT EXISTS `GoalWeight` ("
                        + "`user_id` INTEGER NOT NULL, "
                        + "`goal_weight` REAL NOT NULL, "
                        + "PRIMARY KEY(`user_id`), "
                        + "FOREIGN KEY(`user_id`) REFERENCES `Users`(`id`) "
                        + "ON UPDATE NO ACTION ON DELETE NO ACTION)"
        );

        database.execSQL(
                "CREATE TABLE IF NOT EXISTS `DailyWeights` ("
                        + "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                        + "`user_id` INTEGER NOT NULL, "
                        + "`entry_date` TEXT NOT NULL, "
                        + "`weight` REAL NOT NULL, "
                        + "FOREIGN KEY(`user_id`) REFERENCES `Users`(`id`) "
                        + "ON UPDATE NO ACTION ON DELETE NO ACTION)"
        );
        database.execSQL(
                "CREATE TABLE IF NOT EXISTS `UserSettings` ("
                        + "`user_id` INTEGER NOT NULL, "
                        + "`sms_enabled` INTEGER NOT NULL, "
                        + "`sms_phone_number` TEXT NOT NULL, "
                        + "`last_goal_notified_bits` INTEGER, "
                        + "PRIMARY KEY(`user_id`), "
                        + "FOREIGN KEY(`user_id`) REFERENCES `Users`(`id`) "
                        + "ON UPDATE NO ACTION ON DELETE NO ACTION)"
        );
        database.execSQL(
                "CREATE TABLE IF NOT EXISTS room_master_table ("
                        + "id INTEGER PRIMARY KEY, "
                        + "identity_hash TEXT)"
        );
        database.execSQL(
                "INSERT OR REPLACE INTO room_master_table (id, identity_hash) VALUES "
                        + "(42, '" + identityHash + "')"
        );
    }

    public static void insertEarlyRoomVersion3SeedData(SQLiteDatabase database) {
        database.execSQL(
                "INSERT INTO `Users` (`id`, `username`, `password_salt`, `password_hash`) VALUES "
                        + "(1, 'carol', 'salt-c', 'hash-c')"
        );
        database.execSQL(
                "INSERT INTO `GoalWeight` (`user_id`, `goal_weight`) VALUES "
                        + "(1, 145.0)"
        );
        database.execSQL(
                "INSERT INTO `DailyWeights` (`id`, `user_id`, `entry_date`, `weight`) VALUES "
                        + "(20, 1, '2026-03-07', 170.0), "
                        + "(21, 1, '2026-03-07', 169.0), "
                        + "(22, 1, '2026-03-08', 168.0)"
        );
        database.execSQL(
                "INSERT INTO `UserSettings` "
                        + "(`user_id`, `sms_enabled`, `sms_phone_number`, `last_goal_notified_bits`) "
                        + "VALUES (1, 1, '5553334444', " + Double.doubleToLongBits(145.0) + ")"
        );
    }
}
