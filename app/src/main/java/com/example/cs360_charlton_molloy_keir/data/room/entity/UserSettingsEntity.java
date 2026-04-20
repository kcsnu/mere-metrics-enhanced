package com.example.cs360_charlton_molloy_keir.data.room.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

/** Room entity for SMS settings and goal notification state */
@Entity(
        tableName = "UserSettings",
        foreignKeys = {
                @ForeignKey(
                        entity = UserEntity.class,
                        parentColumns = "id",
                        childColumns = "user_id",
                        onDelete = ForeignKey.CASCADE
                )
        }
)
public class UserSettingsEntity {

    @PrimaryKey
    @ColumnInfo(name = "user_id")
    public long userId;

    @ColumnInfo(name = "sms_enabled")
    public boolean smsEnabled;

    @ColumnInfo(name = "sms_phone_number")
    @NonNull
    public String smsPhoneNumber;

    // Null means the user has never been notified about reaching a goal
    @ColumnInfo(name = "last_goal_notified_bits")
    public Long lastGoalNotifiedBits;

    public UserSettingsEntity(
            long userId,
            boolean smsEnabled,
            String smsPhoneNumber,
            Long lastGoalNotifiedBits
    ) {
        this.userId = userId;
        this.smsEnabled = smsEnabled;
        this.smsPhoneNumber = smsPhoneNumber;
        this.lastGoalNotifiedBits = lastGoalNotifiedBits;
    }
}
