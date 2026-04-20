package com.example.cs360_charlton_molloy_keir.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.cs360_charlton_molloy_keir.R;
import com.example.cs360_charlton_molloy_keir.model.WeightAnalyticsSummary;

import org.junit.Test;

import java.time.LocalDate;
import java.util.Collections;

public class WeightAnalyticsFormatterTest {

    @Test
    public void shouldDisplayGoalReached_returnsTrueWhenSummaryIsReached() {
        assertTrue(WeightAnalyticsFormatter.shouldDisplayGoalReached(
                summary(160.0, 100.0, true, LocalDate.of(2026, 1, 21))
        ));
    }

    @Test
    public void shouldDisplayGoalReached_returnsTrueWhenProgressIsOneHundredPercent() {
        assertTrue(WeightAnalyticsFormatter.shouldDisplayGoalReached(
                summary(160.0, 100.0, false, LocalDate.of(2026, 1, 21))
        ));
    }

    @Test
    public void shouldDisplayGoalReached_returnsFalseWhenGoalIsStillInProgress() {
        assertFalse(WeightAnalyticsFormatter.shouldDisplayGoalReached(
                summary(160.0, 75.0, false, LocalDate.of(2026, 2, 1))
        ));
    }

    @Test
    public void getGoalDateLabelRes_returnsReachedLabelWhenGoalIsReached() {
        assertEquals(
                R.string.analytics_goal_reached_date_label,
                WeightAnalyticsFormatter.getGoalDateLabelRes(
                        summary(160.0, 100.0, true, LocalDate.of(2026, 1, 21))
                )
        );
    }

    @Test
    public void getGoalDateLabelRes_returnsProjectionLabelWhenGoalIsInProgress() {
        assertEquals(
                R.string.analytics_projection_label,
                WeightAnalyticsFormatter.getGoalDateLabelRes(
                        summary(160.0, 75.0, false, LocalDate.of(2026, 2, 1))
                )
        );
    }

    @Test
    public void formatSignedWeightValue_formatsPositiveAndNegativeChanges() {
        assertEquals("+2.5", WeightAnalyticsFormatter.formatSignedWeightValue(2.5));
        assertEquals("-1.0", WeightAnalyticsFormatter.formatSignedWeightValue(-1.0));
    }

    @Test
    public void formatPercentValue_formatsSingleDecimalPercent() {
        assertEquals("75.0%", WeightAnalyticsFormatter.formatPercentValue(75.0));
    }

    private static WeightAnalyticsSummary summary(
            Double goalWeight,
            Double percentProgress,
            boolean goalReached,
            LocalDate projectedGoalDate
    ) {
        return new WeightAnalyticsSummary(
                Collections.emptyList(),
                0,
                0,
                null,
                null,
                null,
                null,
                goalWeight,
                null,
                null,
                null,
                null,
                null,
                percentProgress,
                projectedGoalDate,
                goalReached
        );
    }
}
