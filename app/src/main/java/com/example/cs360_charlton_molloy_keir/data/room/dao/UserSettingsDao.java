package com.example.cs360_charlton_molloy_keir.data.room.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.cs360_charlton_molloy_keir.data.room.entity.UserSettingsEntity;

/** DAO for SMS settings and goal notification state */
@Dao
public interface UserSettingsDao {

    @Query("SELECT * FROM UserSettings WHERE user_id = :userId LIMIT 1")
    UserSettingsEntity getByUserId(long userId);

    // REPLACE on the user_id PK gives us upsert behavior with no existence check
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long upsert(UserSettingsEntity settings);

    @Query("UPDATE UserSettings SET sms_enabled = :enabled WHERE user_id = :userId")
    int updateSmsEnabled(long userId, boolean enabled);

    @Query("UPDATE UserSettings SET sms_phone_number = :phoneNumber WHERE user_id = :userId")
    int updateSmsPhoneNumber(long userId, String phoneNumber);

    @Query("UPDATE UserSettings SET last_goal_notified_bits = :goalBits WHERE user_id = :userId")
    int updateLastGoalNotifiedBits(long userId, Long goalBits);

    // Setting bits to NULL means "no goal has been notified yet," so changing the
    // goal weight triggers a fresh notification check
    @Query("UPDATE UserSettings SET last_goal_notified_bits = NULL WHERE user_id = :userId")
    int clearLastGoalNotifiedBits(long userId);
}
