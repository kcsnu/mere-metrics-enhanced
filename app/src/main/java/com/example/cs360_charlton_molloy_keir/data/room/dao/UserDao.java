package com.example.cs360_charlton_molloy_keir.data.room.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.cs360_charlton_molloy_keir.data.room.entity.UserEntity;

import java.util.List;

/** DAO for user accounts */
@Dao
public interface UserDao {

    @Query("SELECT * FROM Users WHERE username = :username LIMIT 1")
    UserEntity getByUsername(String username);

    @Query("SELECT COUNT(*) FROM Users WHERE username = :username")
    int countByUsername(String username);

    // Used by LegacyPreferencesImporter to walk all users during the SharedPrefs migration
    @Query("SELECT id FROM Users ORDER BY id ASC")
    List<Long> getAllUserIds();

    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insert(UserEntity user);
}
