package com.example.cs360_charlton_molloy_keir.util;

import android.util.Base64;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Password hashing helpers using PBKDF2 with Base64-encoded salt and hash values */
public final class PasswordUtil {

    private static final int SALT_BYTES = 16;
    private static final int ITERATIONS = 100_000;
    private static final int KEY_BITS = 256;
    private static final String PBKDF2_SHA256 = "PBKDF2WithHmacSHA256";
    private static final String PBKDF2_SHA1 = "PBKDF2WithHmacSHA1";

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private PasswordUtil() {
    }

    public static HashResult hashPassword(String password) {
        if (password == null || password.isEmpty()) {
            return null;
        }

        byte[] salt = new byte[SALT_BYTES];
        SECURE_RANDOM.nextBytes(salt);

        byte[] hash = pbkdf2(password, salt);
        if (hash == null || hash.length == 0) {
            return null;
        }

        String saltB64 = Base64.encodeToString(salt, Base64.NO_WRAP);
        String hashB64 = Base64.encodeToString(hash, Base64.NO_WRAP);

        return new HashResult(saltB64, hashB64);
    }

    public static boolean verifyPassword(String password, String saltB64, String expectedHashB64) {
        if (password == null || password.isEmpty()
                || StringUtil.isBlank(saltB64) || StringUtil.isBlank(expectedHashB64)) {
            return false;
        }

        try {
            byte[] salt = Base64.decode(saltB64, Base64.NO_WRAP);
            byte[] expectedHash = Base64.decode(expectedHashB64, Base64.NO_WRAP);
            if (salt.length == 0 || expectedHash.length == 0) {
                return false;
            }

            byte[] actualHash = pbkdf2(password, salt);
            if (actualHash == null || actualHash.length == 0) {
                return false;
            }

            // Use a timing-resistant comparison for stored password hashes
            return MessageDigest.isEqual(actualHash, expectedHash);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static byte[] pbkdf2(String password, byte[] salt) {
        if (salt == null || salt.length == 0) {
            return null;
        }

        char[] passwordChars = password.toCharArray();
        PBEKeySpec spec = new PBEKeySpec(passwordChars, salt, ITERATIONS, KEY_BITS);

        try {
            try {
                return generateSecret(spec, PBKDF2_SHA256);
            } catch (GeneralSecurityException primaryError) {
                // Fall back to SHA-1 PBKDF2 on older Android versions
                return generateSecret(spec, PBKDF2_SHA1);
            }
        } catch (GeneralSecurityException e) {
            return null;
        } finally {
            spec.clearPassword();
            Arrays.fill(passwordChars, '\0');
        }
    }

    private static byte[] generateSecret(PBEKeySpec spec, String algorithm)
            throws GeneralSecurityException {
        SecretKeyFactory factory = SecretKeyFactory.getInstance(algorithm);
        return factory.generateSecret(spec).getEncoded();
    }

    public static final class HashResult {

        public final String saltB64;
        public final String hashB64;

        public HashResult(String saltB64, String hashB64) {
            this.saltB64 = saltB64;
            this.hashB64 = hashB64;
        }
    }
}
