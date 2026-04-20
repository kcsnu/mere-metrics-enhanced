package com.example.cs360_charlton_molloy_keir.data.room.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.cs360_charlton_molloy_keir.data.room.entity.DailyWeightEntity;

import java.util.List;

/** DAO for per-day weight history. ISO dates keep ordering query-friendly. */
@Dao
public interface DailyWeightDao {

    // entry_date is ISO so these ORDER BY clauses produce correct chronological results
    @Query("SELECT * FROM DailyWeights WHERE user_id = :userId ORDER BY entry_date DESC, id DESC")
    List<DailyWeightEntity> getByUserIdNewestFirst(long userId);

    @Query("SELECT * FROM DailyWeights WHERE user_id = :userId ORDER BY entry_date ASC, id ASC")
    List<DailyWeightEntity> getByUserIdOldestFirst(long userId);

    // Inclusive range query used when analytics needs a bounded history slice.
    // Either bound may be null so the service can support start-only and end-only filters.
    @Query("SELECT * FROM DailyWeights WHERE user_id = :userId "
            + "AND (:startDate IS NULL OR entry_date >= :startDate) "
            + "AND (:endDate IS NULL OR entry_date <= :endDate) "
            + "ORDER BY entry_date ASC, id ASC")
    List<DailyWeightEntity> getByUserIdBetweenDates(long userId, String startDate, String endDate);

    // Scoped to both id and user_id so one user can never read another user's rows
    @Query("SELECT * FROM DailyWeights WHERE id = :entryId AND user_id = :userId LIMIT 1")
    DailyWeightEntity getByIdForUser(long userId, long entryId);

    // IGNORE returns -1 when the same user already has a row for that day.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(DailyWeightEntity dailyWeight);

    // IGNORE returns 0 when an update would collide with another row for that day.
    @Update(onConflict = OnConflictStrategy.IGNORE)
    int update(DailyWeightEntity dailyWeight);

    // Scoped to both id and user_id so one user can never delete another user's rows
    @Query("DELETE FROM DailyWeights WHERE id = :entryId AND user_id = :userId")
    int deleteByIdForUser(long userId, long entryId);
}
