package com.example.cs360_charlton_molloy_keir.data.room;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Room;

/** Creates the app Room database */
public final class AppDatabaseProvider {

    private static volatile AppDatabase instance;

    private AppDatabaseProvider() {
    }

    public static AppDatabase getInstance(@NonNull Context context) {
        Context appContext = context.getApplicationContext();

        if (instance == null) {
            synchronized (AppDatabaseProvider.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                                    appContext,
                                    AppDatabase.class,
                                    AppDatabase.DB_NAME
                            )
                            // Keep main-thread queries to match the existing app behavior
                            .allowMainThreadQueries()
                            .addMigrations(
                                    RoomMigrations.MIGRATION_2_3,
                                    RoomMigrations.MIGRATION_3_4
                            )
                            .build();
                }
            }
        }

        // Import legacy SMS settings after the database is ready
        LegacyPreferencesImporter.importIfNeeded(appContext, instance);
        return instance;
    }
}
