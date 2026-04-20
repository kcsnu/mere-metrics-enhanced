package com.example.cs360_charlton_molloy_keir.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.cs360_charlton_molloy_keir.model.WeightAnalyticsSummary;
import com.example.cs360_charlton_molloy_keir.model.WeightEntry;
import com.example.cs360_charlton_molloy_keir.model.NormalizedWeightSeries;

import org.junit.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;

/** Tests analytics filtering, progress, and projection behavior. */
public class WeightAnalyticsServiceTest {

    private final WeightAnalyticsService analyticsService = new WeightAnalyticsService();

    @Test
    public void buildSummary_filtersRangeAndExcludesInvalidDates() {
        WeightAnalyticsSummary summary = analyticsService.buildSummary(
                Arrays.asList(
                        entry(1L, "01/01/2026", 200.0),
                        entry(2L, "not-a-date", 199.0),
                        entry(3L, "01/10/2026", 198.0),
                        entry(4L, "01/20/2026", 196.0)
                ),
                190.0,
                LocalDate.of(2026, 1, 5),
                LocalDate.of(2026, 1, 20)
        );

        assertEquals(2, summary.getFilteredEntries().size());
        assertEquals(4, summary.getTotalEntryCount());
        assertEquals(1, summary.getExcludedInvalidEntryCount());
        assertEquals("01/20/2026", summary.getFilteredEntries().get(0).date);
        assertEquals("01/10/2026", summary.getFilteredEntries().get(1).date);
        assertEquals(LocalDate.of(2026, 1, 10), summary.getActualStartDate());
        assertEquals(LocalDate.of(2026, 1, 20), summary.getActualEndDate());
    }

    @Test
    public void buildSummary_treatsDateRangeBoundariesInclusively() {
        WeightAnalyticsSummary summary = analyticsService.buildSummary(
                Arrays.asList(
                        entry(1L, "01/01/2026", 200.0),
                        entry(2L, "01/10/2026", 198.0),
                        entry(3L, "01/20/2026", 196.0),
                        entry(4L, "01/25/2026", 195.0)
                ),
                190.0,
                LocalDate.of(2026, 1, 10),
                LocalDate.of(2026, 1, 20)
        );

        assertEquals(2, summary.getFilteredEntries().size());
        assertEquals("01/20/2026", summary.getFilteredEntries().get(0).date);
        assertEquals("01/10/2026", summary.getFilteredEntries().get(1).date);
    }

    @Test
    public void buildSummary_computesLatestRollingAverageFromFilteredSlice() {
        WeightAnalyticsSummary summary = analyticsService.buildSummary(
                Arrays.asList(
                        entry(1L, "01/01/2026", 200.0),
                        entry(2L, "01/04/2026", 198.0),
                        entry(3L, "01/07/2026", 196.0),
                        entry(4L, "01/10/2026", 194.0),
                        entry(5L, "01/11/2026", 193.0)
                ),
                180.0,
                LocalDate.of(2026, 1, 9),
                LocalDate.of(2026, 1, 11)
        );

        assertEquals(193.5, summary.getRollingAverage(), 0.0001);
        assertNull(summary.getWeeklyChange());
        assertNull(summary.getMonthlyChange());
    }

    @Test
    public void buildSummary_usesNearestEarlierEntryForSparseLookbackChanges() {
        WeightAnalyticsSummary summary = analyticsService.buildSummary(
                Arrays.asList(
                        entry(1L, "01/01/2026", 200.0),
                        entry(2L, "01/04/2026", 198.0),
                        entry(3L, "01/07/2026", 196.0),
                        entry(4L, "01/10/2026", 194.0),
                        entry(5L, "02/09/2026", 190.0)
                ),
                180.0,
                null,
                null
        );

        assertEquals(190.0, summary.getRollingAverage(), 0.0001);
        assertEquals(-4.0, summary.getWeeklyChange(), 0.0001);
        assertEquals(-4.0, summary.getMonthlyChange(), 0.0001);
    }

    @Test
    public void buildSummary_computesProgressAndProjectionForWeightLoss() {
        WeightAnalyticsSummary summary = analyticsService.buildSummary(
                Arrays.asList(
                        entry(1L, "01/01/2026", 200.0),
                        entry(2L, "01/11/2026", 190.0),
                        entry(3L, "01/21/2026", 180.0)
                ),
                150.0,
                null,
                null
        );

        assertEquals(40.0, summary.getPercentProgressToGoal(), 0.0001);
        assertEquals(LocalDate.of(2026, 2, 20), summary.getProjectedGoalDate());
        assertFalse(summary.isGoalReached());
    }

    @Test
    public void buildSummary_computesProgressAndProjectionForWeightGain() {
        WeightAnalyticsSummary summary = analyticsService.buildSummary(
                Arrays.asList(
                        entry(1L, "01/01/2026", 150.0),
                        entry(2L, "01/11/2026", 160.0),
                        entry(3L, "01/21/2026", 170.0)
                ),
                200.0,
                null,
                null
        );

        assertEquals(40.0, summary.getPercentProgressToGoal(), 0.0001);
        assertEquals(LocalDate.of(2026, 2, 20), summary.getProjectedGoalDate());
        assertFalse(summary.isGoalReached());
    }

    @Test
    public void buildSummary_marksGoalReachedForWeightLoss() {
        WeightAnalyticsSummary summary = analyticsService.buildSummary(
                Arrays.asList(
                        entry(1L, "01/01/2026", 200.0),
                        entry(2L, "01/21/2026", 149.5)
                ),
                150.0,
                null,
                null
        );

        assertTrue(summary.isGoalReached());
        assertEquals(LocalDate.of(2026, 1, 21), summary.getProjectedGoalDate());
    }

    @Test
    public void buildSummary_marksGoalReachedForWeightGain() {
        WeightAnalyticsSummary summary = analyticsService.buildSummary(
                Arrays.asList(
                        entry(1L, "01/01/2026", 150.0),
                        entry(2L, "01/21/2026", 200.5)
                ),
                200.0,
                null,
                null
        );

        assertTrue(summary.isGoalReached());
        assertEquals(LocalDate.of(2026, 1, 21), summary.getProjectedGoalDate());
    }

    @Test
    public void buildSummary_returnsFirstHistoricalGoalDateWhenGoalWasReachedBeforeFilterStart() {
        WeightAnalyticsSummary summary = analyticsService.buildSummary(
                Arrays.asList(
                        entry(1L, "01/01/2026", 200.0),
                        entry(2L, "01/10/2026", 159.0),
                        entry(3L, "01/20/2026", 158.0)
                ),
                160.0,
                LocalDate.of(2026, 1, 20),
                LocalDate.of(2026, 1, 25)
        );

        assertTrue(summary.isGoalReached());
        assertEquals(LocalDate.of(2026, 1, 10), summary.getProjectedGoalDate());
    }

    @Test
    public void buildSummary_suppressesProjectionWhenTrendMovesAwayFromGoal() {
        WeightAnalyticsSummary summary = analyticsService.buildSummary(
                Arrays.asList(
                        entry(1L, "01/01/2026", 180.0),
                        entry(2L, "01/11/2026", 182.0),
                        entry(3L, "01/21/2026", 184.0)
                ),
                170.0,
                null,
                null
        );

        assertNull(summary.getProjectedGoalDate());
        assertEquals(-40.0, summary.getPercentProgressToGoal(), 0.0001);
    }

    @Test
    public void buildSummary_returnsNullProjectionWhenWeightIsFlat() {
        WeightAnalyticsSummary summary = analyticsService.buildSummary(
                Arrays.asList(
                        entry(1L, "01/01/2026", 180.0),
                        entry(2L, "01/11/2026", 180.0),
                        entry(3L, "01/21/2026", 180.0)
                ),
                170.0,
                null,
                null
        );

        assertNull(summary.getProjectedGoalDate());
    }

    @Test
    public void buildSummary_keepsSameDayEntriesInNewestFirstDisplayOrder() {
        WeightAnalyticsSummary summary = analyticsService.buildSummary(
                Arrays.asList(
                        entry(1L, "01/10/2026", 200.0),
                        entry(2L, "01/10/2026", 199.0),
                        entry(3L, "01/11/2026", 198.0)
                ),
                190.0,
                null,
                null
        );

        assertEquals(3L, summary.getFilteredEntries().get(0).id);
        assertEquals(2L, summary.getFilteredEntries().get(1).id);
        assertEquals(1L, summary.getFilteredEntries().get(2).id);
    }

    @Test
    public void buildSummary_returnsEmptyMetricsWhenRangeHasNoMatches() {
        WeightAnalyticsSummary summary = analyticsService.buildSummary(
                Arrays.asList(
                        entry(1L, "01/01/2026", 180.0),
                        entry(2L, "01/02/2026", 179.0)
                ),
                170.0,
                LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 2, 10)
        );

        assertTrue(summary.getFilteredEntries().isEmpty());
        assertNull(summary.getRollingAverage());
        assertNull(summary.getWeeklyChange());
        assertNull(summary.getMonthlyChange());
        assertNull(summary.getPercentProgressToGoal());
        assertNull(summary.getProjectedGoalDate());
        assertFalse(summary.isGoalReached());
    }

    @Test
    public void buildSummaryFromBoundedSeries_preservesFullHistoryCountsAndGoalDate() {
        NormalizedWeightSeries fullSeries = NormalizedWeightSeries.fromChronologicalEntries(
                Arrays.asList(
                        entry(1L, "01/01/2026", 200.0),
                        entry(2L, "01/10/2026", 159.0),
                        entry(3L, "01/20/2026", 158.0)
                )
        );
        NormalizedWeightSeries boundedSeries = NormalizedWeightSeries.fromChronologicalEntries(
                Arrays.asList(
                        entry(3L, "01/20/2026", 158.0)
                )
        );

        WeightAnalyticsSummary summary = analyticsService.buildSummaryFromBoundedSeries(
                fullSeries,
                boundedSeries,
                160.0,
                LocalDate.of(2026, 1, 20),
                LocalDate.of(2026, 1, 25)
        );

        assertEquals(3, summary.getTotalEntryCount());
        assertEquals(1, summary.getFilteredEntries().size());
        assertTrue(summary.isGoalReached());
        assertEquals(LocalDate.of(2026, 1, 10), summary.getProjectedGoalDate());
    }

    @Test
    public void buildSummaryFromBoundedSeries_keepsFullHistoryCountsWhenRangeHasNoMatches() {
        NormalizedWeightSeries fullSeries = NormalizedWeightSeries.fromChronologicalEntries(
                Arrays.asList(
                        entry(1L, "01/01/2026", 180.0),
                        entry(2L, "01/02/2026", 179.0)
                )
        );

        WeightAnalyticsSummary summary = analyticsService.buildSummaryFromBoundedSeries(
                fullSeries,
                NormalizedWeightSeries.fromChronologicalEntries(Collections.emptyList()),
                170.0,
                LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 2, 10)
        );

        assertEquals(2, summary.getTotalEntryCount());
        assertTrue(summary.getFilteredEntries().isEmpty());
        assertNull(summary.getActualStartDate());
        assertNull(summary.getActualEndDate());
    }

    @Test
    public void buildSummary_usesFullHistoryBaselineForFilteredPercentProgress() {
        WeightAnalyticsSummary summary = analyticsService.buildSummary(
                Arrays.asList(
                        entry(1L, "01/01/2026", 200.0),
                        entry(2L, "01/11/2026", 180.0),
                        entry(3L, "01/21/2026", 170.0)
                ),
                150.0,
                LocalDate.of(2026, 1, 11),
                LocalDate.of(2026, 1, 21)
        );

        assertEquals(60.0, summary.getPercentProgressToGoal(), 0.0001);
        assertFalse(summary.isGoalReached());
    }

    @Test
    public void buildSummary_keepsLossGoalDirectionWhenFilteredSliceStartsPastGoal() {
        WeightAnalyticsSummary summary = analyticsService.buildSummary(
                Arrays.asList(
                        entry(1L, "01/01/2026", 200.0),
                        entry(2L, "01/15/2026", 150.0),
                        entry(3L, "01/25/2026", 170.0)
                ),
                160.0,
                LocalDate.of(2026, 1, 15),
                LocalDate.of(2026, 1, 25)
        );

        assertEquals(75.0, summary.getPercentProgressToGoal(), 0.0001);
        assertFalse(summary.isGoalReached());
        assertNull(summary.getProjectedGoalDate());
    }

    @Test
    public void buildSummary_returnsFilteredNotReachedStatusWhenLaterRowsWouldReachGoal() {
        WeightAnalyticsSummary summary = analyticsService.buildSummary(
                Arrays.asList(
                        entry(1L, "01/01/2026", 200.0),
                        entry(2L, "01/15/2026", 170.0),
                        entry(3L, "02/01/2026", 150.0)
                ),
                150.0,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 15)
        );

        assertFalse(summary.isGoalReached());
        assertEquals(LocalDate.of(2026, 1, 25), summary.getProjectedGoalDate());
    }

    @Test
    public void buildSummary_keepsGainGoalDirectionWhenFilteredSliceStartsPastGoal() {
        WeightAnalyticsSummary summary = analyticsService.buildSummary(
                Arrays.asList(
                        entry(1L, "01/01/2026", 150.0),
                        entry(2L, "01/15/2026", 210.0),
                        entry(3L, "01/25/2026", 190.0)
                ),
                200.0,
                LocalDate.of(2026, 1, 15),
                LocalDate.of(2026, 1, 25)
        );

        assertEquals(80.0, summary.getPercentProgressToGoal(), 0.0001);
        assertFalse(summary.isGoalReached());
        assertNull(summary.getProjectedGoalDate());
    }

    @Test
    public void buildSummary_returnsSingleEntryRollingAverageButNoTrendMetrics() {
        WeightAnalyticsSummary summary = analyticsService.buildSummary(
                Arrays.asList(
                        entry(1L, "01/01/2026", 180.0)
                ),
                null,
                null,
                null
        );

        assertEquals(180.0, summary.getRollingAverage(), 0.0001);
        assertNull(summary.getWeeklyChange());
        assertNull(summary.getMonthlyChange());
        assertNull(summary.getProjectedGoalDate());
    }

    @Test
    public void buildSummary_returnsEmptySummaryForNullEntryList() {
        WeightAnalyticsSummary summary = analyticsService.buildSummary(
                null,
                170.0,
                null,
                null
        );

        assertTrue(summary.getFilteredEntries().isEmpty());
        assertNull(summary.getRollingAverage());
        assertNull(summary.getPercentProgressToGoal());
        assertFalse(summary.isGoalReached());
    }

    @Test
    public void buildSummary_returnsEmptySummaryForEmptyEntryList() {
        WeightAnalyticsSummary summary = analyticsService.buildSummary(
                Collections.emptyList(),
                170.0,
                null,
                null
        );

        assertTrue(summary.getFilteredEntries().isEmpty());
        assertEquals(0, summary.getTotalEntryCount());
        assertNull(summary.getRollingAverage());
    }

    @Test
    public void buildSummary_returnsHundredPercentProgressWhenGoalMatchesStartingWeight() {
        WeightAnalyticsSummary summary = analyticsService.buildSummary(
                Arrays.asList(
                        entry(1L, "01/01/2026", 170.0),
                        entry(2L, "01/05/2026", 170.0)
                ),
                170.0,
                null,
                null
        );

        assertEquals(100.0, summary.getPercentProgressToGoal(), 0.0001);
        assertTrue(summary.isGoalReached());
        assertEquals(LocalDate.of(2026, 1, 1), summary.getProjectedGoalDate());
    }

    @Test
    public void hasReachedGoal_respectsToleranceBoundary() {
        assertTrue(WeightAnalyticsService.hasReachedGoal(200.0, 170.005, 170.0));
        assertFalse(WeightAnalyticsService.hasReachedGoal(200.0, 170.02, 170.0));
    }

    @Test
    public void hasReachedGoal_returnsFalseWhenGoalIsNull() {
        assertFalse(WeightAnalyticsService.hasReachedGoal(200.0, 180.0, null));
    }

    @Test
    public void hasReachedGoal_returnsTrueWhenLatestExactlyEqualsGoal() {
        assertTrue(WeightAnalyticsService.hasReachedGoal(200.0, 170.0, 170.0));
    }

    @Test
    public void buildSummary_returnsNullProjectionWhenOnlyOneEntry() {
        WeightAnalyticsSummary summary = analyticsService.buildSummary(
                Arrays.asList(
                        entry(1L, "01/01/2026", 180.0)
                ),
                170.0,
                null,
                null
        );

        assertNull(summary.getProjectedGoalDate());
        assertNull(summary.getWeeklyChange());
        assertNull(summary.getMonthlyChange());
    }

    @Test
    public void buildSummary_computesRollingAverageWithFewerEntriesThanWindowSize() {
        WeightAnalyticsSummary summary = analyticsService.buildSummary(
                Arrays.asList(
                        entry(1L, "01/01/2026", 180.0),
                        entry(2L, "01/03/2026", 176.0)
                ),
                170.0,
                null,
                null
        );

        assertEquals(178.0, summary.getRollingAverage(), 0.0001);
    }

    @Test
    public void goalStatus_returnsNullMetricsWhenNoGoalAndNoEntries() {
        WeightAnalyticsSummary summary = analyticsService.buildSummary(
                Arrays.asList(),
                null,
                null,
                null
        );

        assertNull(summary.getGoalWeight());
        assertNull(summary.getLatestWeight());
        assertFalse(summary.isGoalReached());
    }

    @Test
    public void goalStatus_usesCurrentGoalAndLatestEntry() {
        WeightAnalyticsSummary summary = analyticsService.buildSummary(
                Arrays.asList(
                        entry(1L, "01/01/2026", 180.0),
                        entry(2L, "01/02/2026", 175.0)
                ),
                170.0,
                null,
                null
        );

        assertEquals(170.0, summary.getGoalWeight(), 0.0001);
        assertEquals(175.0, summary.getLatestWeight(), 0.0001);
        assertFalse(summary.isGoalReached());
    }

    @Test
    public void goalStatus_usesLatestEntryInActiveFilter() {
        WeightAnalyticsSummary fullHistorySummary = analyticsService.buildSummary(
                Arrays.asList(
                        entry(1L, "01/01/2026", 200.0),
                        entry(2L, "01/15/2026", 170.0),
                        entry(3L, "02/01/2026", 150.0)
                ),
                150.0,
                null,
                null
        );

        WeightAnalyticsSummary filteredSummary = analyticsService.buildSummary(
                Arrays.asList(
                        entry(1L, "01/01/2026", 200.0),
                        entry(2L, "01/15/2026", 170.0),
                        entry(3L, "02/01/2026", 150.0)
                ),
                150.0,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 15)
        );

        assertTrue(fullHistorySummary.isGoalReached());
        assertFalse(filteredSummary.isGoalReached());
    }

    @Test
    public void goalStatus_matchesNotificationReachedStateLogic() {
        WeightAnalyticsSummary summary = analyticsService.buildSummary(
                Arrays.asList(
                        entry(1L, "01/01/2026", 200.0),
                        entry(2L, "01/15/2026", 149.9)
                ),
                150.0,
                null,
                null
        );

        assertEquals(
                WeightAnalyticsService.hasReachedGoal(200.0, 149.9, 150.0),
                summary.isGoalReached()
        );
    }

    private static WeightEntry entry(long id, String date, double weight) {
        return new WeightEntry(id, date, weight);
    }
}
