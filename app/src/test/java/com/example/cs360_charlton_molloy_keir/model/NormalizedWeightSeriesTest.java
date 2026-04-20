package com.example.cs360_charlton_molloy_keir.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.example.cs360_charlton_molloy_keir.util.DateUtil;

import org.junit.Test;

import java.time.LocalDate;
import java.util.Arrays;

public class NormalizedWeightSeriesTest {

    @Test
    public void fromChronologicalEntries_tracksInvalidEntriesAndPrefixSums() {
        NormalizedWeightSeries series = NormalizedWeightSeries.fromChronologicalEntries(
                Arrays.asList(
                        new WeightEntry(1L, "01/01/2026", 200.0),
                        new WeightEntry(2L, "bad-date", 199.0),
                        new WeightEntry(3L, "01/05/2026", 198.0),
                        new WeightEntry(4L, "01/10/2026", 197.0)
                )
        );

        assertEquals(4, series.getSourceEntryCount());
        assertEquals(1, series.getExcludedInvalidEntryCount());
        assertEquals(3, series.size());
        assertEquals(595.0, series.sumWeights(0, series.size()), 0.0001);
    }

    @Test
    public void binarySearchMethods_respectInclusiveAndExclusiveBoundaries() {
        NormalizedWeightSeries series = NormalizedWeightSeries.fromChronologicalEntries(
                Arrays.asList(
                        new WeightEntry(1L, "01/01/2026", 200.0),
                        new WeightEntry(2L, "01/10/2026", 198.0),
                        new WeightEntry(3L, "01/10/2026", 197.0),
                        new WeightEntry(4L, "01/20/2026", 196.0)
                )
        );

        assertEquals(1, series.lowerBound(series.get(1).getEpochDay()));
        assertEquals(3, series.upperBound(series.get(2).getEpochDay()));
        assertEquals(2, series.floorIndex(series.get(2).getEpochDay()));
    }

    @Test
    public void buildDisplayEntries_returnsNewestFirstCopy() {
        NormalizedWeightSeries series = NormalizedWeightSeries.fromChronologicalEntries(
                Arrays.asList(
                        new WeightEntry(1L, "01/01/2026", 200.0),
                        new WeightEntry(2L, "01/05/2026", 198.0),
                        new WeightEntry(3L, "01/10/2026", 197.0)
                )
        );

        assertEquals(3L, series.buildDisplayEntries(0, series.size()).get(0).id);
        assertEquals(1L, series.buildDisplayEntries(0, series.size()).get(2).id);
        assertTrue(series.buildDisplayEntries(1, 1).isEmpty());
    }

    @Test
    public void searchMethods_handleTargetsOutsideStoredRange() {
        NormalizedWeightSeries series = NormalizedWeightSeries.fromChronologicalEntries(
                Arrays.asList(
                        new WeightEntry(1L, "01/01/2026", 200.0),
                        new WeightEntry(2L, "01/10/2026", 198.0),
                        new WeightEntry(3L, "01/20/2026", 196.0)
                )
        );

        long beforeFirstEntry = DateUtil.toEpochDay(LocalDate.of(2025, 12, 31));
        long afterLastEntry = DateUtil.toEpochDay(LocalDate.of(2026, 1, 21));

        assertEquals(0, series.lowerBound(beforeFirstEntry));
        assertEquals(0, series.upperBound(beforeFirstEntry));
        assertEquals(-1, series.floorIndex(beforeFirstEntry));

        assertEquals(series.size(), series.lowerBound(afterLastEntry));
        assertEquals(series.size(), series.upperBound(afterLastEntry));
        assertEquals(series.size() - 1, series.floorIndex(afterLastEntry));
    }

    @Test
    public void fromChronologicalEntries_preservesRepositoryOrderForSameDayRows() {
        NormalizedWeightSeries series = NormalizedWeightSeries.fromChronologicalEntries(
                Arrays.asList(
                        new WeightEntry(1L, "01/10/2026", 200.0),
                        new WeightEntry(2L, "01/10/2026", 199.0),
                        new WeightEntry(3L, "01/11/2026", 198.0)
                )
        );

        assertEquals(1L, series.get(0).getEntry().id);
        assertEquals(2L, series.get(1).getEntry().id);
        assertEquals(3L, series.get(2).getEntry().id);
    }
}
