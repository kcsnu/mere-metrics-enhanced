package com.example.cs360_charlton_molloy_keir.service;

import com.example.cs360_charlton_molloy_keir.model.NormalizedWeightSeries;
import com.example.cs360_charlton_molloy_keir.model.WeightAnalyticsSummary;
import com.example.cs360_charlton_molloy_keir.model.WeightEntry;
import com.example.cs360_charlton_molloy_keir.util.DateUtil;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

/** Builds analytics summaries from normalized weight history. */
public class WeightAnalyticsService {

    static final double GOAL_TOLERANCE = 0.01;
    private static final int ROLLING_WINDOW_DAYS = 7;
    private static final int WEEKLY_LOOKBACK_DAYS = 7;
    private static final int MONTHLY_LOOKBACK_DAYS = 30;

    public WeightAnalyticsSummary buildSummary(
            List<WeightEntry> chronologicalEntries,
            Double goalWeight,
            LocalDate startDate,
            LocalDate endDate
    ) {
        return buildSummaryFromSeries(
                NormalizedWeightSeries.fromChronologicalEntries(chronologicalEntries),
                goalWeight,
                startDate,
                endDate
        );
    }

    /** Builds analytics from a normalized series so filter changes can reuse parsed history. */
    public WeightAnalyticsSummary buildSummaryFromSeries(
            NormalizedWeightSeries series,
            Double goalWeight,
            LocalDate startDate,
            LocalDate endDate
    ) {
        if (series == null || series.isEmpty()) {
            return buildEmptySummary(
                    goalWeight,
                    startDate,
                    endDate,
                    series == null ? 0 : series.getSourceEntryCount(),
                    series == null ? 0 : series.getExcludedInvalidEntryCount()
            );
        }

        int startIndex = startDate == null
                ? 0
                : series.lowerBound(DateUtil.toEpochDay(startDate));
        int endExclusive = endDate == null
                ? series.size()
                : series.upperBound(DateUtil.toEpochDay(endDate));

        if (startIndex >= endExclusive) {
            return buildEmptySummary(
                    goalWeight,
                    startDate,
                    endDate,
                    series.getSourceEntryCount(),
                    series.getExcludedInvalidEntryCount()
            );
        }

        NormalizedWeightSeries.SeriesEntry firstEntry = series.get(startIndex);
        NormalizedWeightSeries.SeriesEntry latestEntry = series.get(endExclusive - 1);

        // Use the first full-history entry so filters do not change goal direction.
        // Goal reached still depends on the latest entry in the active range.
        double directionBaseline = series.get(0).getEntry().weight;
        boolean goalReached = hasReachedGoal(
                directionBaseline,
                latestEntry.getEntry().weight,
                goalWeight
        );
        LocalDate goalDate = goalReached
                ? findFirstGoalReachedDate(series, endExclusive, directionBaseline, goalWeight)
                : computeProjectedGoalDateFromSlice(directionBaseline, firstEntry, latestEntry, goalWeight);

        return new WeightAnalyticsSummary(
                series.buildDisplayEntries(startIndex, endExclusive),
                series.getSourceEntryCount(),
                series.getExcludedInvalidEntryCount(),
                startDate,
                endDate,
                firstEntry.getDate(),
                latestEntry.getDate(),
                goalWeight,
                firstEntry.getEntry().weight,
                latestEntry.getEntry().weight,
                computeLatestRollingAverage(series, startIndex, endExclusive),
                computeChange(series, startIndex, endExclusive, WEEKLY_LOOKBACK_DAYS),
                computeChange(series, startIndex, endExclusive, MONTHLY_LOOKBACK_DAYS),
                computePercentProgress(directionBaseline, latestEntry.getEntry().weight, goalWeight),
                goalDate,
                goalReached
        );
    }

    /**
     * Builds analytics from a DAO-bounded series while preserving the full-history
     * totals and goal-direction semantics used elsewhere in the app.
     */
    public WeightAnalyticsSummary buildSummaryFromBoundedSeries(
            NormalizedWeightSeries fullSeries,
            NormalizedWeightSeries boundedSeries,
            Double goalWeight,
            LocalDate startDate,
            LocalDate endDate
    ) {
        if (fullSeries == null || fullSeries.isEmpty()) {
            return buildEmptySummary(
                    goalWeight,
                    startDate,
                    endDate,
                    fullSeries == null ? 0 : fullSeries.getSourceEntryCount(),
                    fullSeries == null ? 0 : fullSeries.getExcludedInvalidEntryCount()
            );
        }

        if (boundedSeries == null || boundedSeries.isEmpty()) {
            return buildEmptySummary(
                    goalWeight,
                    startDate,
                    endDate,
                    fullSeries.getSourceEntryCount(),
                    fullSeries.getExcludedInvalidEntryCount()
            );
        }

        NormalizedWeightSeries.SeriesEntry firstEntry = boundedSeries.get(0);
        NormalizedWeightSeries.SeriesEntry latestEntry = boundedSeries.get(boundedSeries.size() - 1);

        double directionBaseline = fullSeries.get(0).getEntry().weight;
        boolean goalReached = hasReachedGoal(
                directionBaseline,
                latestEntry.getEntry().weight,
                goalWeight
        );

        int goalScanEndExclusive = endDate == null
                ? fullSeries.size()
                : fullSeries.upperBound(DateUtil.toEpochDay(endDate));
        LocalDate goalDate = goalReached
                ? findFirstGoalReachedDate(fullSeries, goalScanEndExclusive, directionBaseline, goalWeight)
                : computeProjectedGoalDateFromSlice(directionBaseline, firstEntry, latestEntry, goalWeight);

        return new WeightAnalyticsSummary(
                boundedSeries.buildDisplayEntries(0, boundedSeries.size()),
                fullSeries.getSourceEntryCount(),
                fullSeries.getExcludedInvalidEntryCount(),
                startDate,
                endDate,
                firstEntry.getDate(),
                latestEntry.getDate(),
                goalWeight,
                firstEntry.getEntry().weight,
                latestEntry.getEntry().weight,
                computeLatestRollingAverage(boundedSeries, 0, boundedSeries.size()),
                computeChange(boundedSeries, 0, boundedSeries.size(), WEEKLY_LOOKBACK_DAYS),
                computeChange(boundedSeries, 0, boundedSeries.size(), MONTHLY_LOOKBACK_DAYS),
                computePercentProgress(directionBaseline, latestEntry.getEntry().weight, goalWeight),
                goalDate,
                goalReached
        );
    }

    private WeightAnalyticsSummary buildEmptySummary(
            Double goalWeight,
            LocalDate startDate,
            LocalDate endDate,
            int totalEntryCount,
            int excludedInvalidEntryCount
    ) {
        return new WeightAnalyticsSummary(
                Collections.emptyList(),
                totalEntryCount,
                excludedInvalidEntryCount,
                startDate,
                endDate,
                null,
                null,
                goalWeight,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                false
        );
    }

    /** Returns the latest seven-day rolling average for the filtered slice */
    private Double computeLatestRollingAverage(
            NormalizedWeightSeries series,
            int startIndex,
            int endExclusive
    ) {
        if (startIndex >= endExclusive) {
            return null;
        }

        long latestEpochDay = series.get(endExclusive - 1).getEpochDay();
        long oldestAllowedDay = latestEpochDay - (ROLLING_WINDOW_DAYS - 1L);
        int windowStart = Math.max(startIndex, series.lowerBound(oldestAllowedDay));
        int windowSize = endExclusive - windowStart;

        if (windowSize <= 0) {
            return null;
        }

        return series.sumWeights(windowStart, endExclusive) / windowSize;
    }

    /** Compares the latest filtered entry to the entry at or before the requested lookback day */
    private Double computeChange(
            NormalizedWeightSeries series,
            int startIndex,
            int endExclusive,
            int lookbackDays
    ) {
        if (endExclusive - startIndex < 2) {
            return null;
        }

        NormalizedWeightSeries.SeriesEntry latestEntry = series.get(endExclusive - 1);
        long targetDay = latestEntry.getEpochDay() - lookbackDays;
        int comparisonIndex = series.floorIndex(targetDay);

        // If there is no exact lookback-day row, compare against the nearest earlier entry.
        if (comparisonIndex < startIndex) {
            return null;
        }

        return latestEntry.getEntry().weight - series.get(comparisonIndex).getEntry().weight;
    }

    private Double computePercentProgress(
            double startingWeight,
            double latestWeight,
            Double goalWeight
    ) {
        if (goalWeight == null) {
            return null;
        }

        double goalDistance = goalWeight - startingWeight;
        if (Math.abs(goalDistance) <= GOAL_TOLERANCE) {
            return isWithinGoalTolerance(latestWeight, goalWeight) ? 100.0 : null;
        }

        // Use the same signed formula for loss and gain goals.
        return ((latestWeight - startingWeight) / goalDistance) * 100.0;
    }

    /** Returns the first date the goal was reached within the scanned history. */
    private LocalDate findFirstGoalReachedDate(
            NormalizedWeightSeries series,
            int endExclusive,
            double directionBaseline,
            Double goalWeight
    ) {
        if (goalWeight == null) {
            return null;
        }

        for (int i = 0; i < endExclusive; i++) {
            NormalizedWeightSeries.SeriesEntry entry = series.get(i);
            if (hasReachedGoal(directionBaseline, entry.getEntry().weight, goalWeight)) {
                return entry.getDate();
            }
        }

        return series.get(endExclusive - 1).getDate();
    }

    /** Projects a future goal date from the active filtered slice. */
    private LocalDate computeProjectedGoalDateFromSlice(
            double directionBaseline,
            NormalizedWeightSeries.SeriesEntry firstEntry,
            NormalizedWeightSeries.SeriesEntry latestEntry,
            Double goalWeight
    ) {
        if (goalWeight == null) {
            return null;
        }

        long elapsedDays = latestEntry.getEpochDay() - firstEntry.getEpochDay();
        if (elapsedDays <= 0) {
            return null;
        }

        double dailyRate = (latestEntry.getEntry().weight - firstEntry.getEntry().weight) / elapsedDays;
        if (Math.abs(dailyRate) <= GOAL_TOLERANCE) {
            return null;
        }

        if (isWeightLossGoal(directionBaseline, goalWeight) && dailyRate >= 0) {
            return null;
        }

        if (isWeightGainGoal(directionBaseline, goalWeight) && dailyRate <= 0) {
            return null;
        }

        double remainingChange = goalWeight - latestEntry.getEntry().weight;
        double projectedDays = remainingChange / dailyRate;
        if (projectedDays < 0) {
            return null;
        }

        return latestEntry.getDate().plusDays((long) Math.ceil(projectedDays));
    }

    static boolean hasReachedGoal(double startingWeight, double latestWeight, Double goalWeight) {
        if (goalWeight == null) {
            return false;
        }

        if (isWithinGoalTolerance(latestWeight, goalWeight)) {
            return true;
        }

        if (goalWeight < startingWeight) {
            return latestWeight <= goalWeight + GOAL_TOLERANCE;
        }

        if (goalWeight > startingWeight) {
            return latestWeight >= goalWeight - GOAL_TOLERANCE;
        }

        return false;
    }

    private static boolean isWeightLossGoal(double startingWeight, double goalWeight) {
        return goalWeight < startingWeight - GOAL_TOLERANCE;
    }

    private static boolean isWeightGainGoal(double startingWeight, double goalWeight) {
        return goalWeight > startingWeight + GOAL_TOLERANCE;
    }

    private static boolean isWithinGoalTolerance(double weight, double goalWeight) {
        return Math.abs(weight - goalWeight) <= GOAL_TOLERANCE;
    }
}
