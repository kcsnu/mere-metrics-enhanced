package com.example.cs360_charlton_molloy_keir.data.room.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.cs360_charlton_molloy_keir.data.room.entity.GoalWeightEntity;

/** DAO for goal weights */
@Dao
public interface GoalWeightDao {

    @Query("SELECT * FROM GoalWeight WHERE user_id = :userId LIMIT 1")
    GoalWeightEntity getByUserId(long userId);

    // REPLACE on the user_id PK gives us upsert behavior with no existence check
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long upsert(GoalWeightEntity goalWeight);
}
