package com.taskverse.security;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Base64;

/**
 * PasswordHasher
 * Implements standard, cryptographically secure password hashing using
 * PBKDF2WithHmacSHA256 (NIST SP 800-132 / OWASP standard).
 * 
 * Features:
 * - 310,000 iterations
 * - 16-byte cryptographically secure random salt (SecureRandom)
 * - 256-bit derived key
 * - Constant-time equality comparison (prevents timing attacks)
 * - In-memory array hygiene
 */
public class PasswordHasher {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 310_000;
    private static final int KEY_LENGTH_BITS = 256;
    private static final int SALT_BYTES = 16;
    private static final SecureRandom RANDOM = new SecureRandom();

    public static class HashResult {
        private final String saltBase64;
        private final String hashBase64;

        public HashResult(String saltBase64, String hashBase64) {
            this.saltBase64 = saltBase64;
            this.hashBase64 = hashBase64;
        }

        public String getSaltBase64() {
            return saltBase64;
        }

        public String getHashBase64() {
            return hashBase64;
        }
    }

    /**
     * Hashes a plain-text password using a freshly generated 16-byte salt.
     * @param password password characters
     * @return HashResult containing Base64 salt and Base64 hash
     */
    public static HashResult hashPassword(char[] password) {
        if (password == null || password.length == 0) {
            throw new IllegalArgumentException("Password cannot be null or empty.");
        }

        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);

        byte[] hash = computeHash(password, salt, ITERATIONS, KEY_LENGTH_BITS);
        String saltBase64 = Base64.getEncoder().encodeToString(salt);
        String hashBase64 = Base64.getEncoder().encodeToString(hash);

        // Wipe temporary byte arrays
        Arrays.fill(salt, (byte) 0);
        Arrays.fill(hash, (byte) 0);

        return new HashResult(saltBase64, hashBase64);
    }

    /**
     * Verifies an entered password against stored salt and expected hash in constant time.
     * @param password entered password
     * @param saltBase64 stored salt Base64
     * @param expectedHashBase64 stored hash Base64
     * @return true if password matches, false otherwise
     */
    public static boolean verifyPassword(char[] password, String saltBase64, String expectedHashBase64) {
        if (password == null || saltBase64 == null || expectedHashBase64 == null) {
            return false;
        }

        byte[] salt;
        byte[] expectedHash;
        try {
            salt = Base64.getDecoder().decode(saltBase64);
            expectedHash = Base64.getDecoder().decode(expectedHashBase64);
        } catch (IllegalArgumentException e) {
            return false;
        }

        byte[] computedHash = computeHash(password, salt, ITERATIONS, KEY_LENGTH_BITS);

        // Constant-time comparison to prevent timing attacks
        boolean matches = MessageDigest.isEqual(computedHash, expectedHash);

        Arrays.fill(salt, (byte) 0);
        Arrays.fill(expectedHash, (byte) 0);
        Arrays.fill(computedHash, (byte) 0);

        return matches;
    }

    private static byte[] computeHash(char[] password, byte[] salt, int iterations, int keyLengthBits) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLengthBits);
        try {
            SecretKeyFactory skf = SecretKeyFactory.getInstance(ALGORITHM);
            return skf.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Cryptographic algorithm unavailable: " + ALGORITHM, e);
        } finally {
            spec.clearPassword();
        }
    }
}
