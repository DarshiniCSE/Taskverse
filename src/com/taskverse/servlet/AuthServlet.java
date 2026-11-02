package com.taskverse.servlet;

import com.taskverse.dao.UserDAO;
import com.taskverse.dao.UserDAOImpl;
import com.taskverse.model.User;
import com.taskverse.security.CsrfTokenManager;
import com.taskverse.security.PasswordHasher;
import com.taskverse.security.RateLimiter;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.regex.Pattern;

/**
 * AuthServlet
 * Handles user registration, login, logout, current session check, and CSRF token provisioning.
 */
public class AuthServlet extends HttpServlet {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]{3,30}$");

    private UserDAO userDAO;

    @Override
    public void init() throws ServletException {
        this.userDAO = new UserDAOImpl();
    }

    // Accessible for unit/integration testing
    public void setUserDAO(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        setupResponseHeaders(response);
        PrintWriter out = response.getWriter();

        String path = getSubPath(request);

        switch (path.toLowerCase()) {
            case "/me":
            case "/status":
                handleMe(request, response, out);
                break;
            case "/csrf":
                handleCsrf(request, response, out);
                break;
            default:
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.print("{\"success\":false,\"message\":\"Unknown auth endpoint.\"}");
                break;
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        setupResponseHeaders(response);
        PrintWriter out = response.getWriter();

        String path = getSubPath(request);

        switch (path.toLowerCase()) {
            case "/login":
                handleLogin(request, response, out);
                break;
            case "/register":
                handleRegister(request, response, out);
                break;
            case "/logout":
                handleLogout(request, response, out);
                break;
            default:
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.print("{\"success\":false,\"message\":\"Unknown auth endpoint.\"}");
                break;
        }
    }

    private void handleLogin(HttpServletRequest request, HttpServletResponse response, PrintWriter out) {
        String clientIp = getClientIp(request);
        String identifier = request.getParameter("identifier");
        if (identifier == null) identifier = request.getParameter("email");
        String password = request.getParameter("password");

        if (identifier == null || identifier.trim().isEmpty() || password == null || password.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"success\":false,\"message\":\"Identifier and password are required.\"}");
            return;
        }

        String ipKey = "IP:" + clientIp;
        String userKey = "USER:" + identifier.trim().toLowerCase();

        // Check rate limiting
        if (!RateLimiter.isAllowed(ipKey)) {
            int remaining = RateLimiter.getRemainingLockoutSeconds(ipKey);
            response.setStatus(429); // Too Many Requests
            out.print("{\"success\":false,\"message\":\"Too many failed attempts from your IP. Try again in " + remaining + " seconds.\"}");
            return;
        }

        if (!RateLimiter.isAllowed(userKey)) {
            int remaining = RateLimiter.getRemainingLockoutSeconds(userKey);
            response.setStatus(429);
            out.print("{\"success\":false,\"message\":\"Account temporarily locked due to excessive failed attempts. Try again in " + remaining + " seconds.\"}");
            return;
        }

        try {
            User user = null;
            if (identifier.contains("@")) {
                user = userDAO.getUserByEmail(identifier);
            } else {
                user = userDAO.getUserByUsername(identifier);
            }

            if (user == null || !user.isActive()) {
                RateLimiter.recordFailure(ipKey);
                RateLimiter.recordFailure(userKey);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                out.print("{\"success\":false,\"message\":\"Invalid email or password.\"}");
                return;
            }

            boolean matches = PasswordHasher.verifyPassword(
                    password.toCharArray(),
                    user.getPasswordSalt(),
                    user.getPasswordHash()
            );

            if (!matches) {
                RateLimiter.recordFailure(ipKey);
                RateLimiter.recordFailure(userKey);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                out.print("{\"success\":false,\"message\":\"Invalid email or password.\"}");
                return;
            }

            // Authentication succeeded: clear rate limiter
            RateLimiter.recordSuccess(ipKey);
            RateLimiter.recordSuccess(userKey);

            // Establish secure session with session fixation defense
            request.changeSessionId();
            HttpSession session = request.getSession(true);
            session.setAttribute("USER_ID", user.getUserId());
            session.setAttribute("USERNAME", user.getUsername());
            session.setAttribute("ROLE", user.getRole());

            String csrfToken = CsrfTokenManager.refreshToken(session);

            out.print("{\"success\":true,\"user\":" + user.toJSON() + ",\"csrfToken\":\"" + csrfToken + "\"}");

        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"success\":false,\"message\":\"Authentication service is temporarily unavailable.\"}");
        }
    }

    private void handleRegister(HttpServletRequest request, HttpServletResponse response, PrintWriter out) {
        String clientIp = getClientIp(request);
        String username = request.getParameter("username");
        String email = request.getParameter("email");
        String password = request.getParameter("password");

        String ipKey = "IP:" + clientIp;
        if (!RateLimiter.isAllowed(ipKey)) {
            int remaining = RateLimiter.getRemainingLockoutSeconds(ipKey);
            response.setStatus(429);
            out.print("{\"success\":false,\"message\":\"Too many requests. Try again in " + remaining + " seconds.\"}");
            return;
        }

        // Validate username
        if (username == null || !USERNAME_PATTERN.matcher(username.trim()).matches()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"success\":false,\"message\":\"Username must be 3-30 characters and alphanumeric or underscore.\"}");
            return;
        }

        // Validate email
        if (email == null || !EMAIL_PATTERN.matcher(email.trim()).matches()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"success\":false,\"message\":\"A valid email address is required.\"}");
            return;
        }

        // Validate password
        if (password == null || password.length() < 8) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"success\":false,\"message\":\"Password must be at least 8 characters long.\"}");
            return;
        }

        try {
            if (userDAO.getUserByEmail(email) != null) {
                response.setStatus(HttpServletResponse.SC_CONFLICT);
                out.print("{\"success\":false,\"message\":\"Email is already registered.\"}");
                return;
            }

            if (userDAO.getUserByUsername(username) != null) {
                response.setStatus(HttpServletResponse.SC_CONFLICT);
                out.print("{\"success\":false,\"message\":\"Username is already taken.\"}");
                return;
            }

            PasswordHasher.HashResult hashResult = PasswordHasher.hashPassword(password.toCharArray());

            // Public registration strictly creates standard MEMBER accounts.
            // Administrative OWNER accounts can ONLY be established via the local SetupAdmin CLI.
            User newUser = new User();
            newUser.setUsername(username.trim());
            newUser.setEmail(email.trim().toLowerCase());
            newUser.setPasswordHash(hashResult.getHashBase64());
            newUser.setPasswordSalt(hashResult.getSaltBase64());
            newUser.setRole("MEMBER");
            newUser.setActive(true);

            User created = userDAO.createUser(newUser);

            // Automatically sign in upon registration
            request.changeSessionId();
            HttpSession session = request.getSession(true);
            session.setAttribute("USER_ID", created.getUserId());
            session.setAttribute("USERNAME", created.getUsername());
            session.setAttribute("ROLE", created.getRole());

            String csrfToken = CsrfTokenManager.refreshToken(session);

            response.setStatus(HttpServletResponse.SC_CREATED);
            out.print("{\"success\":true,\"user\":" + created.toJSON() + ",\"csrfToken\":\"" + csrfToken + "\"}");

        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"success\":false,\"message\":\"Registration service is temporarily unavailable.\"}");
        }
    }

    private void handleLogout(HttpServletRequest request, HttpServletResponse response, PrintWriter out) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        out.print("{\"success\":true,\"message\":\"Logged out successfully.\"}");
    }

    private void handleMe(HttpServletRequest request, HttpServletResponse response, PrintWriter out) {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("USER_ID") == null) {
            out.print("{\"authenticated\":false}");
            return;
        }

        int userId = (Integer) session.getAttribute("USER_ID");
        try {
            User user = userDAO.getUserById(userId);
            if (user != null && user.isActive()) {
                String csrfToken = CsrfTokenManager.getToken(session);
                out.print("{\"authenticated\":true,\"user\":" + user.toJSON() + ",\"csrfToken\":\"" + csrfToken + "\"}");
            } else {
                session.invalidate();
                out.print("{\"authenticated\":false}");
            }
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"authenticated\":false,\"message\":\"Database temporarily unavailable.\"}");
        }
    }

    private void handleCsrf(HttpServletRequest request, HttpServletResponse response, PrintWriter out) {
        HttpSession session = request.getSession(true);
        String token = CsrfTokenManager.getToken(session);
        out.print("{\"success\":true,\"csrfToken\":\"" + token + "\"}");
    }

    private String getSubPath(HttpServletRequest request) {
        String pathInfo = request.getPathInfo();
        if (pathInfo != null) {
            return pathInfo;
        }
        String servletPath = request.getServletPath();
        if (servletPath != null && servletPath.startsWith("/api/auth")) {
            return servletPath.substring("/api/auth".length());
        }
        return "";
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.trim().isEmpty()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void setupResponseHeaders(HttpServletResponse response) {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        // Strip wildcard CORS; TaskVerse is same-origin
        response.setHeader("X-Content-Type-Options", "nosniff");
    }
}
