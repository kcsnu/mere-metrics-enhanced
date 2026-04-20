package com.example.cs360_charlton_molloy_keir.data.room.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** Room entity for user accounts */
@Entity(
        tableName = "Users",
        indices = {
                // Unique index so the DAO insert can ABORT on duplicate usernames
                @Index(value = {"username"}, unique = true)
        }
)
public class UserEntity {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    public long id;

    @ColumnInfo(name = "username")
    @NonNull
    public String username;

    @ColumnInfo(name = "password_salt")
    @NonNull
    public String passwordSalt;

    @ColumnInfo(name = "password_hash")
    @NonNull
    public String passwordHash;

    public UserEntity(long id, String username, String passwordSalt, String passwordHash) {
        this.id = id;
        this.username = username;
        this.passwordSalt = passwordSalt;
        this.passwordHash = passwordHash;
    }

    @Ignore
    public UserEntity(String username, String passwordSalt, String passwordHash) {
        this(0L, username, passwordSalt, passwordHash);
    }
}
