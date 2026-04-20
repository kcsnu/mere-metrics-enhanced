package com.example.cs360_charlton_molloy_keir.data;

import android.content.Context;
import android.database.sqlite.SQLiteConstraintException;

import com.example.cs360_charlton_molloy_keir.data.room.AppDatabase;
import com.example.cs360_charlton_molloy_keir.data.room.AppDatabaseProvider;
import com.example.cs360_charlton_molloy_keir.data.room.entity.DailyWeightEntity;
import com.example.cs360_charlton_molloy_keir.data.room.entity.GoalWeightEntity;
import com.example.cs360_charlton_molloy_keir.model.WeightEntry;
import com.example.cs360_charlton_molloy_keir.util.DateUtil;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Goal-weight and daily-weight access to the app database */
public class WeightRepository {

    public enum AddDailyWeightStatus {
        SUCCESS,
        DUPLICATE_DATE,
        FAILURE
    }

    public enum UpdateDailyWeightStatus {
        SUCCESS,
        DUPLICATE_DATE,
        FAILURE
    }

    public static final class UpdateDailyWeightResult {
        private final UpdateDailyWeightStatus status;

        private UpdateDailyWeightResult(UpdateDailyWeightStatus status) {
            this.status = status;
        }

        public UpdateDailyWeightStatus getStatus() {
            return status;
        }
    }

    private final AppDatabase database;

    public WeightRepository(Context context) {
        this.database = AppDatabaseProvider.getInstance(context.getApplicationContext());
    }

    public void upsertGoalWeight(long userId, double goalWeight) {
        database.goalWeightDao().upsert(new GoalWeightEntity(userId, goalWeight));
    }

    public Double getGoalWeight(long userId) {
        GoalWeightEntity goalWeight = database.goalWeightDao().getByUserId(userId);
        return goalWeight == null ? null : goalWeight.goalWeight;
    }

    public AddDailyWeightStatus addDailyWeight(long userId, String date, double weight) {
        if (date == null) {
            return AddDailyWeightStatus.FAILURE;
        }

        String normalizedDate = date.trim();
        if (normalizedDate.isEmpty()) {
            return AddDailyWeightStatus.FAILURE;
        }

        String storageDate = DateUtil.toStorageDate(normalizedDate);
        if (storageDate == null) {
            return AddDailyWeightStatus.FAILURE;
        }

        try {
            long insertedId = database.dailyWeightDao().insert(
                    new DailyWeightEntity(userId, storageDate, weight)
            );
            return insertedId == -1L
                    ? AddDailyWeightStatus.DUPLICATE_DATE
                    : AddDailyWeightStatus.SUCCESS;
        } catch (SQLiteConstraintException e) {
            return AddDailyWeightStatus.FAILURE;
        }
    }

    public List<WeightEntry> getDailyWeightsNewestFirst(long userId) {
        return mapEntitiesToDisplayEntries(database.dailyWeightDao().getByUserIdNewestFirst(userId));
    }

    public List<WeightEntry> getDailyWeightsOldestFirst(long userId) {
        return mapEntitiesToDisplayEntries(database.dailyWeightDao().getByUserIdOldestFirst(userId));
    }

    public List<WeightEntry> getDailyWeightsBetween(long userId, LocalDate startDate, LocalDate endDate) {
        String storageStartDate = startDate == null ? null : DateUtil.formatStorageDate(startDate);
        String storageEndDate = endDate == null ? null : DateUtil.formatStorageDate(endDate);
        return mapEntitiesToDisplayEntries(
                database.dailyWeightDao().getByUserIdBetweenDates(
                        userId,
                        storageStartDate,
                        storageEndDate
                )
        );
    }

    public UpdateDailyWeightResult updateDailyWeight(
            long userId,
            long entryId,
            String newDate,
            double newWeight
    ) {
        if (newDate == null) {
            return new UpdateDailyWeightResult(UpdateDailyWeightStatus.FAILURE);
        }

        String normalizedDate = newDate.trim();
        if (normalizedDate.isEmpty()) {
            return new UpdateDailyWeightResult(UpdateDailyWeightStatus.FAILURE);
        }

        String storageDate = DateUtil.toStorageDate(normalizedDate);
        if (storageDate == null) {
            return new UpdateDailyWeightResult(UpdateDailyWeightStatus.FAILURE);
        }

        DailyWeightEntity existingEntry = database.dailyWeightDao().getByIdForUser(userId, entryId);
        if (existingEntry == null) {
            return new UpdateDailyWeightResult(UpdateDailyWeightStatus.FAILURE);
        }

        if (storageDate.equals(existingEntry.entryDate)
                && Double.compare(existingEntry.weight, newWeight) == 0) {
            return new UpdateDailyWeightResult(UpdateDailyWeightStatus.SUCCESS);
        }

        DailyWeightEntity updatedEntry = new DailyWeightEntity(
                existingEntry.id,
                existingEntry.userId,
                storageDate,
                newWeight
        );

        try {
            int rowsUpdated = database.dailyWeightDao().update(updatedEntry);
            if (rowsUpdated > 0) {
                return new UpdateDailyWeightResult(UpdateDailyWeightStatus.SUCCESS);
            }

            DailyWeightEntity entryAfterIgnoredUpdate =
                    database.dailyWeightDao().getByIdForUser(userId, entryId);
            return new UpdateDailyWeightResult(
                    entryAfterIgnoredUpdate == null
                            ? UpdateDailyWeightStatus.FAILURE
                            : UpdateDailyWeightStatus.DUPLICATE_DATE
            );
        } catch (SQLiteConstraintException e) {
            return new UpdateDailyWeightResult(UpdateDailyWeightStatus.FAILURE);
        }
    }

    public boolean deleteDailyWeight(long userId, long entryId) {
        return database.dailyWeightDao().deleteByIdForUser(userId, entryId) > 0;
    }

    private List<WeightEntry> mapEntitiesToDisplayEntries(List<DailyWeightEntity> entities) {
        List<WeightEntry> entries = new ArrayList<>();
        if (entities == null || entities.isEmpty()) {
            return entries;
        }

        for (DailyWeightEntity entity : entities) {
            String displayDate = DateUtil.toDisplayDate(entity.entryDate);
            entries.add(new WeightEntry(
                    entity.id,
                    displayDate == null ? entity.entryDate : displayDate,
                    entity.weight
            ));
        }

        return entries;
    }
}
