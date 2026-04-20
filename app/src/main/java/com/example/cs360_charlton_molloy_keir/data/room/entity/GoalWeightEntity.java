package com.example.cs360_charlton_molloy_keir.data.room.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

/** Room entity for goal weights */
@Entity(
        tableName = "GoalWeight",
        foreignKeys = {
                @ForeignKey(
                        entity = UserEntity.class,
                        parentColumns = "id",
                        childColumns = "user_id",
                        onDelete = ForeignKey.CASCADE
                )
        }
)
public class GoalWeightEntity {

    @PrimaryKey
    @ColumnInfo(name = "user_id")
    public long userId;

    @ColumnInfo(name = "goal_weight")
    public double goalWeight;

    public GoalWeightEntity(long userId, double goalWeight) {
        this.userId = userId;
        this.goalWeight = goalWeight;
    }
}
