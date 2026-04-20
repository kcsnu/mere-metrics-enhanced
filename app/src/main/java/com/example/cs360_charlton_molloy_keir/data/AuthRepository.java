package com.example.cs360_charlton_molloy_keir.data;

import android.content.Context;
import android.database.sqlite.SQLiteConstraintException;

import com.example.cs360_charlton_molloy_keir.data.room.AppDatabase;
import com.example.cs360_charlton_molloy_keir.data.room.AppDatabaseProvider;
import com.example.cs360_charlton_molloy_keir.data.room.entity.UserEntity;
import com.example.cs360_charlton_molloy_keir.util.PasswordUtil;

/** Authentication-related access to the app database */
public class AuthRepository {

    private static final long INVALID_RESULT = -1L;

    private final AppDatabase database;

    public AuthRepository(Context context) {
        this.database = AppDatabaseProvider.getInstance(context.getApplicationContext());
    }

    public long validateLogin(String username, String password) {
        if (username == null || password == null) {
            return INVALID_RESULT;
        }

        String normalizedUsername = username.trim();
        if (normalizedUsername.isEmpty()) {
            return INVALID_RESULT;
        }

        UserEntity user = database.userDao().getByUsername(normalizedUsername);
        if (user == null) {
            return INVALID_RESULT;
        }

        return PasswordUtil.verifyPassword(password, user.passwordSalt, user.passwordHash)
                ? user.id
                : INVALID_RESULT;
    }

    public boolean usernameExists(String username) {
        if (username == null) {
            return false;
        }

        String normalizedUsername = username.trim();
        if (normalizedUsername.isEmpty()) {
            return false;
        }

        return database.userDao().countByUsername(normalizedUsername) > 0;
    }

    public long createUser(String username, String password) {
        if (username == null || password == null) {
            return INVALID_RESULT;
        }

        String normalizedUsername = username.trim();
        if (normalizedUsername.isEmpty()) {
            return INVALID_RESULT;
        }

        PasswordUtil.HashResult passwordHash = PasswordUtil.hashPassword(password);
        if (passwordHash == null) {
            return INVALID_RESULT;
        }

        try {
            return database.userDao().insert(
                    new UserEntity(
                            normalizedUsername,
                            passwordHash.saltB64,
                            passwordHash.hashB64
                    )
            );
        } catch (SQLiteConstraintException e) {
            return INVALID_RESULT;
        }
    }

}
