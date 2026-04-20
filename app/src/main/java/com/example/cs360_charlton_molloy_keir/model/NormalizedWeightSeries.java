package com.example.cs360_charlton_molloy_keir.model;

import com.example.cs360_charlton_molloy_keir.util.DateUtil;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Parsed weight history with date indexes and running totals for analytics. */
public final class NormalizedWeightSeries {

    /** Normalized weight entry with both display and numeric date representations */
    public static final class SeriesEntry {
        private final WeightEntry entry;
        private final LocalDate date;
        private final long epochDay;

        private SeriesEntry(WeightEntry entry, LocalDate date, long epochDay) {
            this.entry = entry;
            this.date = date;
            this.epochDay = epochDay;
        }

        public WeightEntry getEntry() {
            return entry;
        }

        public LocalDate getDate() {
            return date;
        }

        public long getEpochDay() {
            return epochDay;
        }
    }

    private final List<SeriesEntry> entries;
    private final long[] epochDays;
    private final double[] prefixWeightSums;
    private final int sourceEntryCount;
    private final int excludedInvalidEntryCount;

    private NormalizedWeightSeries(
            List<SeriesEntry> entries,
            long[] epochDays,
            double[] prefixWeightSums,
            int sourceEntryCount,
            int excludedInvalidEntryCount
    ) {
        this.entries = List.copyOf(entries);
        this.epochDays = epochDays;
        this.prefixWeightSums = prefixWeightSums;
        this.sourceEntryCount = sourceEntryCount;
        this.excludedInvalidEntryCount = excludedInvalidEntryCount;
    }

    /** Builds the parsed series and running totals. */
    public static NormalizedWeightSeries fromChronologicalEntries(List<WeightEntry> chronologicalEntries) {
        if (chronologicalEntries == null || chronologicalEntries.isEmpty()) {
            return new NormalizedWeightSeries(
                    Collections.emptyList(),
                    new long[0],
                    new double[]{0.0},
                    0,
                    0
            );
        }

        List<SeriesEntry> normalizedEntries = new ArrayList<>();
        int excludedInvalidEntryCount = 0;

        // Repository rows already arrive in chronological order, so build directly.
        for (WeightEntry entry : chronologicalEntries) {
            LocalDate normalizedDate = DateUtil.parseDate(entry.date);
            if (normalizedDate == null) {
                excludedInvalidEntryCount++;
                continue;
            }

            normalizedEntries.add(new SeriesEntry(
                    entry,
                    normalizedDate,
                    DateUtil.toEpochDay(normalizedDate)
            ));
        }

        // Keep epoch days and running totals alongside the entries for later lookups.
        long[] epochDays = new long[normalizedEntries.size()];
        double[] prefixWeightSums = new double[normalizedEntries.size() + 1];

        for (int i = 0; i < normalizedEntries.size(); i++) {
            SeriesEntry normalizedEntry = normalizedEntries.get(i);
            epochDays[i] = normalizedEntry.epochDay;
            prefixWeightSums[i + 1] = prefixWeightSums[i] + normalizedEntry.entry.weight;
        }

        return new NormalizedWeightSeries(
                normalizedEntries,
                epochDays,
                prefixWeightSums,
                chronologicalEntries.size(),
                excludedInvalidEntryCount
        );
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public int size() {
        return entries.size();
    }

    public int getSourceEntryCount() {
        return sourceEntryCount;
    }

    public int getExcludedInvalidEntryCount() {
        return excludedInvalidEntryCount;
    }

    public SeriesEntry get(int index) {
        return entries.get(index);
    }

    /** Returns the first index on or after the target day. */
    public int lowerBound(long targetEpochDay) {
        int low = 0;
        int high = epochDays.length;

        while (low < high) {
            int mid = low + (high - low) / 2;
            if (epochDays[mid] < targetEpochDay) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }

        return low;
    }

    /** Returns the first index strictly after the target day. */
    public int upperBound(long targetEpochDay) {
        int low = 0;
        int high = epochDays.length;

        while (low < high) {
            int mid = low + (high - low) / 2;
            if (epochDays[mid] <= targetEpochDay) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }

        return low;
    }

    /** Returns the last index on or before the target day, or -1 if none exists. */
    public int floorIndex(long targetEpochDay) {
        int low = 0;
        int high = epochDays.length - 1;
        int result = -1;

        while (low <= high) {
            int mid = low + (high - low) / 2;
            if (epochDays[mid] <= targetEpochDay) {
                result = mid;
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }

        return result;
    }

    /** Returns the total weight between startInclusive and endExclusive. */
    public double sumWeights(int startInclusive, int endExclusive) {
        return prefixWeightSums[endExclusive] - prefixWeightSums[startInclusive];
    }

    /** Returns a newest-first copy for RecyclerView display */
    public List<WeightEntry> buildDisplayEntries(int startInclusive, int endExclusive) {
        List<WeightEntry> displayEntries = new ArrayList<>(Math.max(0, endExclusive - startInclusive));

        for (int i = endExclusive - 1; i >= startInclusive; i--) {
            displayEntries.add(entries.get(i).entry);
        }

        return displayEntries;
    }
}
