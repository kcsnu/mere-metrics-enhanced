package com.example.cs360_charlton_molloy_keir.service;

import android.content.Context;

import com.example.cs360_charlton_molloy_keir.data.AuthRepository;
import com.example.cs360_charlton_molloy_keir.data.UserPreferencesRepository;
import com.example.cs360_charlton_molloy_keir.util.StringUtil;

/** Handles login and account-creation workflows */
public class AuthService {

    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final long INVALID_USER_ID = -1L;

    public enum AuthStatus {
        SUCCESS,
        MISSING_CREDENTIALS,
        LOGIN_FAILED,
        USERNAME_TAKEN,
        WEAK_PASSWORD,
        CREATE_FAILED
    }

    /** Result of a login or account-creation request */
    public static final class AuthResult {
        private final AuthStatus status;

        private AuthResult(AuthStatus status) {
            this.status = status;
        }

        public AuthStatus getStatus() {
            return status;
        }
    }

    private final AuthRepository authRepository;
    private final UserPreferencesRepository userPreferencesRepository;

    public AuthService(Context context) {
        this.authRepository = new AuthRepository(context);
        this.userPreferencesRepository = new UserPreferencesRepository(context);
    }

    public AuthResult logIn(String username, String password) {
        String normalizedUsername = StringUtil.safeTrim(username);

        if (normalizedUsername.isEmpty() || StringUtil.isBlank(password)) {
            return new AuthResult(AuthStatus.MISSING_CREDENTIALS);
        }

        long userId = authRepository.validateLogin(normalizedUsername, password);
        if (userId == INVALID_USER_ID) {
            return new AuthResult(AuthStatus.LOGIN_FAILED);
        }

        userPreferencesRepository.storeLoggedInUser(userId);
        return new AuthResult(AuthStatus.SUCCESS);
    }

    public AuthResult createAccount(String username, String password) {
        String normalizedUsername = StringUtil.safeTrim(username);

        if (normalizedUsername.isEmpty() || StringUtil.isBlank(password)) {
            return new AuthResult(AuthStatus.MISSING_CREDENTIALS);
        }

        // Reject obviously weak passwords before attempting persistence
        if (!meetsMinimumPasswordLength(password)) {
            return new AuthResult(AuthStatus.WEAK_PASSWORD);
        }

        // Keep account-creation rules here so the UI only handles presentation
        if (authRepository.usernameExists(normalizedUsername)) {
            return new AuthResult(AuthStatus.USERNAME_TAKEN);
        }

        long userId = authRepository.createUser(normalizedUsername, password);
        if (userId == INVALID_USER_ID) {
            return new AuthResult(AuthStatus.CREATE_FAILED);
        }

        userPreferencesRepository.storeLoggedInUser(userId);
        return new AuthResult(AuthStatus.SUCCESS);
    }

    private static boolean meetsMinimumPasswordLength(String password) {
        return password != null && password.length() >= MIN_PASSWORD_LENGTH;
    }
}
