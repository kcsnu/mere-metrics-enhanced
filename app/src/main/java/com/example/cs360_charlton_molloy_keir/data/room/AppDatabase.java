package com.example.cs360_charlton_molloy_keir.data.room;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.example.cs360_charlton_molloy_keir.data.room.dao.DailyWeightDao;
import com.example.cs360_charlton_molloy_keir.data.room.dao.GoalWeightDao;
import com.example.cs360_charlton_molloy_keir.data.room.dao.UserDao;
import com.example.cs360_charlton_molloy_keir.data.room.dao.UserSettingsDao;
import com.example.cs360_charlton_molloy_keir.data.room.entity.DailyWeightEntity;
import com.example.cs360_charlton_molloy_keir.data.room.entity.GoalWeightEntity;
import com.example.cs360_charlton_molloy_keir.data.room.entity.UserEntity;
import com.example.cs360_charlton_molloy_keir.data.room.entity.UserSettingsEntity;

/** Room database for users, goal weights, daily weight entries, and SMS settings */
@Database(
        entities = {
                UserEntity.class,
                GoalWeightEntity.class,
                DailyWeightEntity.class,
                UserSettingsEntity.class
        },
        version = AppDatabase.DB_VERSION,
        exportSchema = true
)
public abstract class AppDatabase extends RoomDatabase {

    public static final String DB_NAME = "cs360_weight_tracker.db";
    public static final int DB_VERSION = 4;

    public abstract UserDao userDao();

    public abstract GoalWeightDao goalWeightDao();

    public abstract DailyWeightDao dailyWeightDao();

    public abstract UserSettingsDao userSettingsDao();
}
