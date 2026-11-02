package com.taskverse.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * AuthFilter
 * Enforces session-based authentication on all protected API endpoints.
 * 
 * Rules:
 * - Unauthenticated requests to /api/tasks/*, /api/notes/*, /api/productivity/*, /api/dashboard/*
 *   are rejected with HTTP 401 Unauthorized.
 * - Public routes (/api/auth/*, /api/health, static resources) are allowed through.
 * - Standard security headers are added to every response.
 */
public class AuthFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        // Apply standard security headers
        res.setHeader("X-Content-Type-Options", "nosniff");
        res.setHeader("X-Frame-Options", "DENY");
        res.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

        String contextPath = req.getContextPath();
        String uri = req.getRequestURI();
        String path = uri.substring(contextPath.length());

        // Check if route requires authentication
        if (isProtectedApi(path)) {
            HttpSession session = req.getSession(false);
            Integer userId = (session != null) ? (Integer) session.getAttribute("USER_ID") : null;

            if (userId == null) {
                res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                res.setContentType("application/json");
                res.setCharacterEncoding("UTF-8");
                res.getWriter().print("{\"success\":false,\"message\":\"Authentication required. Please sign in.\"}");
                return;
            }
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
    }

    private boolean isProtectedApi(String path) {
        if (path == null) return false;
        
        // Health check is always public
        if (path.startsWith("/api/health")) {
            return false;
        }

        // Auth routes are public (login, register, me, csrf)
        if (path.startsWith("/api/auth/")) {
            return false;
        }

        // Protected API endpoints
        return path.startsWith("/api/tasks") ||
               path.startsWith("/api/notes") ||
               path.startsWith("/api/productivity") ||
               path.startsWith("/api/dashboard");
    }
}
