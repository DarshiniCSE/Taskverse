package com.taskverse.filter;

import com.taskverse.security.CsrfTokenManager;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * CsrfFilter
 * Enforces Synchronizer Token Pattern (STP) CSRF validation on all state-changing requests.
 * 
 * Rules:
 * - Mutating HTTP methods (POST, PUT, DELETE, PATCH) targeting protected API routes must supply
 *   a valid X-CSRF-Token matching the token stored in the user's session.
 * - Exempt: /api/auth/login, /api/auth/register, /api/health, and safe read-only methods (GET, HEAD, OPTIONS).
 */
public class CsrfFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        String method = req.getMethod();
        String contextPath = req.getContextPath();
        String uri = req.getRequestURI();
        String path = uri.substring(contextPath.length());

        // Safe methods (GET, HEAD, OPTIONS) do not alter server state
        if (isSafeMethod(method)) {
            chain.doFilter(request, response);
            return;
        }

        // Check if route is exempt from CSRF
        if (isExemptRoute(path)) {
            chain.doFilter(request, response);
            return;
        }

        // Only enforce on /api/* routes
        if (path.startsWith("/api/")) {
            if (!CsrfTokenManager.isValidToken(req)) {
                res.setStatus(HttpServletResponse.SC_FORBIDDEN);
                res.setContentType("application/json");
                res.setCharacterEncoding("UTF-8");
                res.getWriter().print("{\"success\":false,\"message\":\"Invalid or missing CSRF token.\"}");
                return;
            }
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
    }

    private boolean isSafeMethod(String method) {
        if (method == null) return true;
        String m = method.toUpperCase();
        return "GET".equals(m) || "HEAD".equals(m) || "OPTIONS".equals(m);
    }

    private boolean isExemptRoute(String path) {
        if (path == null) return true;
        // Login and Registration are pre-authentication and do not require session-bound CSRF tokens
        return path.startsWith("/api/auth/login") ||
               path.startsWith("/api/auth/register") ||
               path.startsWith("/api/health");
    }
}
