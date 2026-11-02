package com.taskverse.security;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RateLimiter
 * A simple, concurrency-safe, in-memory rate limiter to protect authentication
 * endpoints against credential stuffing and brute-force attacks.
 * 
 * Rules:
 * - 5 failed attempts per IP or username/email within a 15-minute window triggers a 15-minute lockout.
 * - Successful authentication clears the attempt counter for that key.
 * - Thread-safe operations via ConcurrentHashMap and atomic updates.
 */
public class RateLimiter {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long WINDOW_MILLIS = 15L * 60L * 1000L; // 15 minutes
    private static final long LOCKOUT_MILLIS = 15L * 60L * 1000L; // 15 minutes lockout

    private static final ConcurrentHashMap<String, AttemptTracker> cache = new ConcurrentHashMap<>();

    private static class AttemptTracker {
        int failedCount;
        long windowStart;
        long lockoutUntil;

        AttemptTracker(long now) {
            this.failedCount = 1;
            this.windowStart = now;
            this.lockoutUntil = 0L;
        }
    }

    /**
     * Checks if requests for the given key (IP address or user identifier) are permitted.
     * @param key identifier key (e.g. "IP:192.168.1.10" or "USER:alex@example.com")
     * @return true if allowed, false if currently locked out
     */
    public static boolean isAllowed(String key) {
        if (key == null || key.trim().isEmpty()) {
            return true;
        }

        long now = System.currentTimeMillis();
        AttemptTracker tracker = cache.get(key.trim().toLowerCase());
        if (tracker == null) {
            return true;
        }

        synchronized (tracker) {
            if (tracker.lockoutUntil > now) {
                return false; // Still locked out
            }
            if (now - tracker.windowStart > WINDOW_MILLIS && tracker.lockoutUntil <= now) {
                // Window expired and not locked out
                return true;
            }
            return tracker.failedCount < MAX_FAILED_ATTEMPTS;
        }
    }

    /**
     * Returns remaining lockout duration in seconds, or 0 if not locked out.
     */
    public static int getRemainingLockoutSeconds(String key) {
        if (key == null || key.trim().isEmpty()) {
            return 0;
        }

        long now = System.currentTimeMillis();
        AttemptTracker tracker = cache.get(key.trim().toLowerCase());
        if (tracker == null) {
            return 0;
        }

        synchronized (tracker) {
            if (tracker.lockoutUntil > now) {
                return (int) Math.ceil((tracker.lockoutUntil - now) / 1000.0);
            }
            return 0;
        }
    }

    /**
     * Records a failed authentication attempt.
     */
    public static void recordFailure(String key) {
        if (key == null || key.trim().isEmpty()) {
            return;
        }

        String normalizedKey = key.trim().toLowerCase();
        long now = System.currentTimeMillis();

        cache.compute(normalizedKey, (k, tracker) -> {
            if (tracker == null || (now - tracker.windowStart > WINDOW_MILLIS && tracker.lockoutUntil <= now)) {
                return new AttemptTracker(now);
            }

            synchronized (tracker) {
                tracker.failedCount++;
                if (tracker.failedCount >= MAX_FAILED_ATTEMPTS) {
                    tracker.lockoutUntil = now + LOCKOUT_MILLIS;
                }
                return tracker;
            }
        });
    }

    /**
     * Records a successful authentication, resetting the tracker.
     */
    public static void recordSuccess(String key) {
        if (key == null || key.trim().isEmpty()) {
            return;
        }
        cache.remove(key.trim().toLowerCase());
    }

    /**
     * Periodic cleanup of expired entries to prevent memory growth.
     */
    public static void cleanup() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, AttemptTracker>> iterator = cache.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, AttemptTracker> entry = iterator.next();
            AttemptTracker tracker = entry.getValue();
            synchronized (tracker) {
                if (tracker.lockoutUntil <= now && (now - tracker.windowStart > WINDOW_MILLIS)) {
                    iterator.remove();
                }
            }
        }
    }

    /**
     * Resets all trackers (primarily for unit testing).
     */
    public static void resetAll() {
        cache.clear();
    }
}
