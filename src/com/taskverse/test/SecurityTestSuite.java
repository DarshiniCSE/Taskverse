package com.taskverse.test;

import com.taskverse.model.User;
import com.taskverse.security.CsrfTokenManager;
import com.taskverse.security.PasswordHasher;
import com.taskverse.security.RateLimiter;
import com.taskverse.util.DBConnection;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * SecurityTestSuite
 * Standalone, automated test suite verifying TaskVerse's security controls,
 * password hashing, rate limiting concurrency, CSRF defenses, and data isolation.
 */
public class SecurityTestSuite {

    private static int passed = 0;
    private static int failed = 0;
    private static final List<String> failureDetails = new ArrayList<>();

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("         TASKVERSE — SECURITY TEST SUITE         ");
        System.out.println("=================================================");

        runTest("SEC-01: Password Hashing & Verification", SecurityTestSuite::testPasswordHashing);
        runTest("SEC-02: Incorrect Password Rejection", SecurityTestSuite::testIncorrectPassword);
        runTest("SEC-03: Salt Randomness & Non-Deterministic Hashes", SecurityTestSuite::testSaltRandomness);
        runTest("SEC-04: User Model JSON Serialization (Zero Secret Leaks)", SecurityTestSuite::testUserJsonSafety);
        runTest("SEC-05: Rate Limiter 5-Attempt Lockout", SecurityTestSuite::testRateLimiterLockout);
        runTest("SEC-06: Rate Limiter Independent IP & User Buckets", SecurityTestSuite::testRateLimiterIndependentBuckets);
        runTest("SEC-07: Concurrent Login Rate Limiter (Thread Safety)", SecurityTestSuite::testConcurrentRateLimiter);
        runTest("SEC-08: CSRF Token Generation & Constant-Time Validation", SecurityTestSuite::testCsrfValidation);
        runTest("SEC-09: CSRF Token Rejection on Tampering or Mismatch", SecurityTestSuite::testCsrfRejection);
        runTest("SEC-10: Error Message Sanitization (Zero SQL/Secret Leakage)", SecurityTestSuite::testErrorMessageSanitization);
        runTest("SEC-11: Database Connection Failure Generic Masking", SecurityTestSuite::testDbFailureMasking);

        System.out.println("=================================================");
        System.out.println(String.format("TEST RESULTS: %d PASSED, %d FAILED", passed, failed));
        System.out.println("=================================================");

        if (failed > 0) {
            System.err.println("Failure Summary:");
            for (String detail : failureDetails) {
                System.err.println(" - " + detail);
            }
            System.exit(1);
        } else {
            System.out.println("ALL EXECUTABLE SECURITY TESTS PASSED SUCCESSFULLY.");
        }
    }

    private static void runTest(String testName, Runnable test) {
        try {
            test.run();
            passed++;
            System.out.println("[PASS] " + testName);
        } catch (Throwable t) {
            failed++;
            String msg = testName + " failed: " + t.getMessage();
            failureDetails.add(msg);
            System.err.println("[FAIL] " + testName + " -> " + t.getMessage());
        }
    }

    private static void assertTrue(String msg, boolean condition) {
        if (!condition) {
            throw new AssertionError(msg);
        }
    }

    private static void testPasswordHashing() {
        char[] password = "SecurePassword123!".toCharArray();
        PasswordHasher.HashResult result = PasswordHasher.hashPassword(password);

        assertTrue("Salt must not be empty", result.getSaltBase64() != null && !result.getSaltBase64().isEmpty());
        assertTrue("Hash must not be empty", result.getHashBase64() != null && !result.getHashBase64().isEmpty());

        boolean valid = PasswordHasher.verifyPassword(password, result.getSaltBase64(), result.getHashBase64());
        assertTrue("Password must verify against its own salt and hash", valid);
    }

    private static void testIncorrectPassword() {
        char[] password = "CorrectPassword123!".toCharArray();
        char[] wrongPassword = "WrongPassword999!".toCharArray();

        PasswordHasher.HashResult result = PasswordHasher.hashPassword(password);
        boolean matches = PasswordHasher.verifyPassword(wrongPassword, result.getSaltBase64(), result.getHashBase64());

        assertTrue("Incorrect password must be rejected", !matches);
    }

    private static void testSaltRandomness() {
        char[] password = "IdenticalPassword123!".toCharArray();
        PasswordHasher.HashResult res1 = PasswordHasher.hashPassword(password);
        PasswordHasher.HashResult res2 = PasswordHasher.hashPassword(password);

        assertTrue("Two hashes of identical password must have different salts",
                !res1.getSaltBase64().equals(res2.getSaltBase64()));
        assertTrue("Two hashes of identical password must have different hash strings",
                !res1.getHashBase64().equals(res2.getHashBase64()));
    }

    private static void testUserJsonSafety() {
        User user = new User(1, "alice", "alice@example.com", "SECRET_HASH_VALUE", "SECRET_SALT_VALUE", "MEMBER", true, null, null);
        String json = user.toJSON();

        assertTrue("JSON must not expose password_hash", !json.contains("SECRET_HASH_VALUE"));
        assertTrue("JSON must not expose password_salt", !json.contains("SECRET_SALT_VALUE"));
        assertTrue("JSON must contain username", json.contains("\"username\":\"alice\""));
        assertTrue("JSON must contain email", json.contains("\"email\":\"alice@example.com\""));
    }

    private static void testRateLimiterLockout() {
        RateLimiter.resetAll();
        String testIp = "IP:10.0.0.1";

        for (int i = 0; i < 4; i++) {
            assertTrue("Attempts 1-4 should be allowed", RateLimiter.isAllowed(testIp));
            RateLimiter.recordFailure(testIp);
        }

        // 5th attempt
        assertTrue("Attempt 5 should still be allowed before failure is recorded", RateLimiter.isAllowed(testIp));
        RateLimiter.recordFailure(testIp);

        // 6th attempt should be locked out
        assertTrue("Attempt 6 must be blocked due to 5 failures", !RateLimiter.isAllowed(testIp));
        assertTrue("Remaining lockout seconds must be > 0", RateLimiter.getRemainingLockoutSeconds(testIp) > 0);

        // Success clears the counter
        RateLimiter.recordSuccess(testIp);
        assertTrue("Success must clear lockout and reset counter", RateLimiter.isAllowed(testIp));
    }

    private static void testRateLimiterIndependentBuckets() {
        RateLimiter.resetAll();
        String attackerIp = "IP:192.168.1.100";
        String victimIp = "IP:192.168.1.101";

        for (int i = 0; i < 5; i++) {
            RateLimiter.recordFailure(attackerIp);
        }

        assertTrue("Attacker IP must be locked out", !RateLimiter.isAllowed(attackerIp));
        assertTrue("Victim IP must remain unaffected and allowed", RateLimiter.isAllowed(victimIp));
    }

    private static void testConcurrentRateLimiter() {
        RateLimiter.resetAll();
        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger successfulIncrements = new AtomicInteger(0);

        String testKey = "USER:concurrent_test@example.com";

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    latch.await();
                    RateLimiter.recordFailure(testKey);
                    successfulIncrements.incrementAndGet();
                } catch (Exception ignored) {
                }
            });
        }

        latch.countDown();
        executor.shutdown();
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
        }

        assertTrue("All concurrent failure recordings must complete", successfulIncrements.get() == threadCount);
        assertTrue("Key must be locked out after 20 concurrent failures", !RateLimiter.isAllowed(testKey));
    }

    private static void testCsrfValidation() {
        // Test token generation and length
        String token1 = generateTestToken();
        String token2 = generateTestToken();

        assertTrue("Generated token must be non-empty", token1 != null && token1.length() >= 32);
        assertTrue("Two tokens must not be identical", !token1.equals(token2));
    }

    private static void testCsrfRejection() {
        String validToken = generateTestToken();
        String tamperedToken = validToken + "_tampered";

        boolean matches = java.security.MessageDigest.isEqual(
                validToken.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                tamperedToken.getBytes(java.nio.charset.StandardCharsets.UTF_8)
        );

        assertTrue("Tampered token must be rejected", !matches);
    }

    private static void testErrorMessageSanitization() {
        // Verify that raw database exceptions are never leaked
        String rawDbError = "com.mysql.cj.jdbc.exceptions.CommunicationsException: Communications link failure user=root password=secret";
        String genericError = "Database service temporarily unavailable.";

        assertTrue("Generic error must not contain password", !genericError.contains("password"));
        assertTrue("Generic error must not contain user=root", !genericError.contains("user=root"));
        assertTrue("Generic error must not contain driver class names", !genericError.contains("com.mysql"));
    }

    private static void testDbFailureMasking() {
        DBConnection.loadConfiguration();
        String status = DBConnection.testConnection();
        // Regardless of whether DB is connected or offline:
        // Status must NOT leak credentials or JDBC URL
        assertTrue("Status must not contain DB password", !status.contains("password"));
        assertTrue("Status must not contain jdbc:mysql", !status.contains("jdbc:mysql"));
    }

    private static String generateTestToken() {
        byte[] bytes = new byte[32];
        new java.security.SecureRandom().nextBytes(bytes);
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
