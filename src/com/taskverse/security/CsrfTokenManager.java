package com.taskverse.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * CsrfTokenManager
 * Manages Synchronizer Token Pattern (STP) CSRF tokens bound to user HttpSessions.
 */
public class CsrfTokenManager {

    public static final String CSRF_SESSION_ATTR = "CSRF_TOKEN";
    public static final String CSRF_HEADER_NAME = "X-CSRF-Token";
    public static final String CSRF_PARAM_NAME = "csrf_token";

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 32;

    /**
     * Retrieves the existing CSRF token from the session, or generates a new one.
     */
    public static String getToken(HttpSession session) {
        if (session == null) {
            return null;
        }

        synchronized (session) {
            String token = (String) session.getAttribute(CSRF_SESSION_ATTR);
            if (token == null || token.trim().isEmpty()) {
                token = generateToken();
                session.setAttribute(CSRF_SESSION_ATTR, token);
            }
            return token;
        }
    }

    /**
     * Regenerates a new CSRF token for the session (e.g. upon login).
     */
    public static String refreshToken(HttpSession session) {
        if (session == null) {
            return null;
        }
        synchronized (session) {
            String token = generateToken();
            session.setAttribute(CSRF_SESSION_ATTR, token);
            return token;
        }
    }

    /**
     * Validates an incoming request's CSRF token against the session token in constant time.
     */
    public static boolean isValidToken(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return false;
        }

        String sessionToken = (String) session.getAttribute(CSRF_SESSION_ATTR);
        if (sessionToken == null || sessionToken.trim().isEmpty()) {
            return false;
        }

        String requestToken = request.getHeader(CSRF_HEADER_NAME);
        if (requestToken == null || requestToken.trim().isEmpty()) {
            requestToken = request.getParameter(CSRF_PARAM_NAME);
        }

        if (requestToken == null || requestToken.trim().isEmpty()) {
            return false;
        }

        // Constant-time comparison
        return MessageDigest.isEqual(
                sessionToken.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                requestToken.trim().getBytes(java.nio.charset.StandardCharsets.UTF_8)
        );
    }

    private static String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
