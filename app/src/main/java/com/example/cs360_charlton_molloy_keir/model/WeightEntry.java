package com.example.cs360_charlton_molloy_keir.model;

/** Represents a single weight-history entry */
public final class WeightEntry {

    public final long id;
    public final String date;
    public final double weight;

    public WeightEntry(long id, String date, double weight) {
        this.id = id;
        // Normalize null or padded values before storing the display date
        this.date = date == null ? "" : date.trim();
        this.weight = weight;
    }
}