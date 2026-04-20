package com.example.cs360_charlton_molloy_keir.support;

import com.example.cs360_charlton_molloy_keir.data.room.AppDatabase;
import com.example.cs360_charlton_molloy_keir.data.room.entity.DailyWeightEntity;
import com.example.cs360_charlton_molloy_keir.data.room.entity.UserEntity;
import com.example.cs360_charlton_molloy_keir.service.WeightService;

import java.util.Arrays;
import java.util.List;

/** Reusable analytics and ordering fixtures for instrumentation tests. */
public final class AnalyticsScenarioFixtures {

    private AnalyticsScenarioFixtures() {
    }

    public static long createUser(AppDatabase database, String username) {
        return database.userDao().insert(
                new UserEntity(username, "salt-" + username, "hash-" + username)
        );
    }

    public static void insertIsoWeights(
            AppDatabase database,
            long userId,
            List<WeightedDate> entries
    ) {
        for (WeightedDate entry : entries) {
            database.dailyWeightDao().insert(
                    new DailyWeightEntity(userId, entry.dateText, entry.weight)
            );
        }
    }

    public static void addDisplayWeights(
            WeightService weightService,
            long userId,
            List<WeightedDate> entries
    ) {
        for (WeightedDate entry : entries) {
            WeightService.AddEntryResult result =
                    weightService.addEntry(userId, entry.dateText, String.valueOf(entry.weight));
            if (result.getStatus() != WeightService.AddEntryStatus.SUCCESS) {
                throw new IllegalStateException("Failed to seed analytics fixture entry: " + entry.dateText);
            }
        }
    }

    public static List<WeightedDate> orderedIsoOrderingScenario() {
        return Arrays.asList(
                new WeightedDate("2026-03-08", 198.0),
                new WeightedDate("2026-03-05", 200.0),
                new WeightedDate("2026-03-06", 199.0)
        );
    }

    public static List<WeightedDate> sparseGapDisplayScenario() {
        return Arrays.asList(
                new WeightedDate("01/01/2026", 205.0),
                new WeightedDate("01/09/2026", 201.0),
                new WeightedDate("01/24/2026", 197.0),
                new WeightedDate("02/14/2026", 193.0),
                new WeightedDate("03/20/2026", 188.0)
        );
    }

    public static List<WeightedDate> goalRegainedDisplayScenario() {
        return Arrays.asList(
                new WeightedDate("01/01/2026", 210.0),
                new WeightedDate("01/20/2026", 195.0),
                new WeightedDate("02/10/2026", 179.5),
                new WeightedDate("03/01/2026", 184.0),
                new WeightedDate("04/01/2026", 189.0)
        );
    }

    public static final class WeightedDate {
        public final String dateText;
        public final double weight;

        public WeightedDate(String dateText, double weight) {
            this.dateText = dateText;
            this.weight = weight;
        }
    }
}
