package com.example.cs360_charlton_molloy_keir.data.room.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** Room entity for daily weight rows stored with ISO dates */
@Entity(
        tableName = "DailyWeights",
        foreignKeys = {
                @ForeignKey(
                        entity = UserEntity.class,
                        parentColumns = "id",
                        childColumns = "user_id",
                        onDelete = ForeignKey.CASCADE
                )
        },
        indices = {
                @Index(value = {"user_id"}),
                // Unique on (user_id, entry_date) so each user gets one weight per day.
                // The migration removes older duplicates before this constraint is applied.
                @Index(value = {"user_id", "entry_date"}, unique = true)
        }
)
public class DailyWeightEntity {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    public long id;

    @ColumnInfo(name = "user_id")
    public long userId;

    // yyyy-MM-dd so SQLite can sort dates natively without substr() tricks
    @ColumnInfo(name = "entry_date")
    @NonNull
    public String entryDate;

    @ColumnInfo(name = "weight")
    public double weight;

    public DailyWeightEntity(long id, long userId, String entryDate, double weight) {
        this.id = id;
        this.userId = userId;
        this.entryDate = entryDate;
        this.weight = weight;
    }

    @Ignore
    public DailyWeightEntity(long userId, String entryDate, double weight) {
        this(0L, userId, entryDate, weight);
    }
}
