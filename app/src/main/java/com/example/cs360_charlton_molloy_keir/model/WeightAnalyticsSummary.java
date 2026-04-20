package com.example.cs360_charlton_molloy_keir.model;

import java.time.LocalDate;
import java.util.List;

/** Analytics snapshot with filtered entries and derived metrics for the UI */
public final class WeightAnalyticsSummary {

    private final List<WeightEntry> filteredEntries;
    private final int totalEntryCount;
    private final int excludedInvalidEntryCount;
    private final LocalDate requestedStartDate;
    private final LocalDate requestedEndDate;
    private final LocalDate actualStartDate;
    private final LocalDate actualEndDate;
    private final Double goalWeight;
    private final Double startingWeight;
    private final Double latestWeight;
    private final Double rollingAverage;
    private final Double weeklyChange;
    private final Double monthlyChange;
    private final Double percentProgressToGoal;
    private final LocalDate projectedGoalDate;
    private final boolean goalReached;

    public WeightAnalyticsSummary(
            List<WeightEntry> filteredEntries,
            int totalEntryCount,
            int excludedInvalidEntryCount,
            LocalDate requestedStartDate,
            LocalDate requestedEndDate,
            LocalDate actualStartDate,
            LocalDate actualEndDate,
            Double goalWeight,
            Double startingWeight,
            Double latestWeight,
            Double rollingAverage,
            Double weeklyChange,
            Double monthlyChange,
            Double percentProgressToGoal,
            LocalDate projectedGoalDate,
            boolean goalReached
    ) {
        this.filteredEntries = List.copyOf(filteredEntries);
        this.totalEntryCount = totalEntryCount;
        this.excludedInvalidEntryCount = excludedInvalidEntryCount;
        this.requestedStartDate = requestedStartDate;
        this.requestedEndDate = requestedEndDate;
        this.actualStartDate = actualStartDate;
        this.actualEndDate = actualEndDate;
        this.goalWeight = goalWeight;
        this.startingWeight = startingWeight;
        this.latestWeight = latestWeight;
        this.rollingAverage = rollingAverage;
        this.weeklyChange = weeklyChange;
        this.monthlyChange = monthlyChange;
        this.percentProgressToGoal = percentProgressToGoal;
        this.projectedGoalDate = projectedGoalDate;
        this.goalReached = goalReached;
    }

    public List<WeightEntry> getFilteredEntries() {
        return filteredEntries;
    }

    public int getTotalEntryCount() {
        return totalEntryCount;
    }

    public int getExcludedInvalidEntryCount() {
        return excludedInvalidEntryCount;
    }

    public LocalDate getRequestedStartDate() {
        return requestedStartDate;
    }

    public LocalDate getRequestedEndDate() {
        return requestedEndDate;
    }

    public LocalDate getActualStartDate() {
        return actualStartDate;
    }

    public LocalDate getActualEndDate() {
        return actualEndDate;
    }

    public Double getGoalWeight() {
        return goalWeight;
    }

    @SuppressWarnings("unused")
    public Double getStartingWeight() {
        return startingWeight;
    }

    @SuppressWarnings("unused")
    public Double getLatestWeight() {
        return latestWeight;
    }

    public Double getRollingAverage() {
        return rollingAverage;
    }

    public Double getWeeklyChange() {
        return weeklyChange;
    }

    public Double getMonthlyChange() {
        return monthlyChange;
    }

    public Double getPercentProgressToGoal() {
        return percentProgressToGoal;
    }

    /** Returns the projected goal date, or the first goal-reached date when the goal is met */
    public LocalDate getProjectedGoalDate() {
        return projectedGoalDate;
    }

    public boolean isGoalReached() {
        return goalReached;
    }
}
