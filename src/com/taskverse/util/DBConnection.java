package com.taskverse.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * DBConnection Utility
 * Manages database connections using JDBC.
 *
 * Supports both local development and cloud deployment environments:
 * 1. Cloud / Container: reads DB_URL, DB_USER, DB_PASSWORD, DB_DRIVER from environment variables.
 * 2. Local Development: reads from 'db.properties' (classpath or db/ directory).
 * 3. Priority: Environment variables override properties file configuration.
 * 4. Security: No hardcoded paths, no unsafe fallback defaults (no default root user, empty password, or assumed URL).
 * 5. Credentials, internal paths, raw exception messages, database names, and JDBC URLs are never exposed in user-facing responses or logs.
 */
public class DBConnection {

    private static String url;
    private static String user;
    private static String password;
    private static String driver = "com.mysql.cj.jdbc.Driver";
    private static boolean initialized = false;
    private static String initError = null;

    static {
        loadConfiguration();
    }

    /**
     * Loads and validates database configuration from environment variables and db.properties.
     * Environment variables take precedence over db.properties values.
     * Failed initialization strictly resets all fields so no stale credentials or configuration are retained.
     */
    public static synchronized void loadConfiguration() {
        initialized = false;
        initError = null;
        url = null;
        user = null;
        password = null;
        driver = "com.mysql.cj.jdbc.Driver";

        Properties props = new Properties();
        InputStream input = null;

        try {
            // 1. Try loading from web application classpath (WEB-INF/classes/db.properties)
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            if (cl != null) {
                input = cl.getResourceAsStream("db.properties");
            }
            if (input == null) {
                input = DBConnection.class.getClassLoader().getResourceAsStream("db.properties");
            }
            if (input == null) {
                input = DBConnection.class.getResourceAsStream("/db.properties");
            }

            // 2. Fallback: try loading directly from project relative directory (db/db.properties)
            if (input == null) {
                File localFile = new File("db/db.properties");
                if (localFile.exists()) {
                    input = new FileInputStream(localFile);
                }
            }

            if (input != null) {
                props.load(input);
            }
        } catch (Exception e) {
            // Safe diagnostic notice: avoid logging raw exception messages or filesystem paths
            System.err.println("[TaskVerse DBConnection] Notice: Local db.properties could not be loaded; relying on environment variables.");
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (Exception ignored) {
                }
            }
        }

        // Environment variables take priority over db.properties
        String rawUrl = resolveConfigValue("DB_URL", "db.url", props);
        String rawUser = resolveConfigValue("DB_USER", "db.user", props);
        String rawPassword = resolveConfigValue("DB_PASSWORD", "db.password", props);
        String rawDriver = resolveConfigValue("DB_DRIVER", "db.driver", props);

        String resolvedUrl = getTrimmed(rawUrl);
        String resolvedUser = getTrimmed(rawUser);

        // Passwords: preserve intentional leading/trailing spaces for valid passwords,
        // but treat null, empty, or whitespace-only strings as missing/invalid.
        String resolvedPassword = null;
        if (rawPassword != null) {
            if (rawPassword.trim().isEmpty()) {
                resolvedPassword = null;
            } else {
                resolvedPassword = rawPassword;
            }
        }

        String resolvedDriver = getTrimmed(rawDriver);
        if (resolvedDriver == null) {
            resolvedDriver = "com.mysql.cj.jdbc.Driver";
        }

        // Validate required configuration (no unsafe fallback defaults)
        List<String> missing = new ArrayList<>();
        if (resolvedUrl == null) {
            missing.add("DB_URL (or db.url)");
        }
        if (resolvedUser == null) {
            missing.add("DB_USER (or db.user)");
        }
        if (resolvedPassword == null) {
            missing.add("DB_PASSWORD (or db.password)");
        }

        if (!missing.isEmpty()) {
            initialized = false;
            url = null;
            user = null;
            password = null;
            initError = "Missing required database configuration: " + String.join(", ", missing)
                    + ". Please configure them via environment variables or db.properties.";
            System.err.println("[TaskVerse DBConnection] " + initError);
            return;
        }

        try {
            driver = resolvedDriver;
            Class.forName(driver);
            url = resolvedUrl;
            user = resolvedUser;
            password = resolvedPassword;
            initialized = true;
            initError = null;
        } catch (ClassNotFoundException e) {
            initialized = false;
            url = null;
            user = null;
            password = null;
            initError = "JDBC Driver class not found.";
            System.err.println("[TaskVerse DBConnection] Error: JDBC Driver class could not be loaded.");
        } catch (Exception e) {
            initialized = false;
            url = null;
            user = null;
            password = null;
            initError = "Failed to initialize database driver.";
            System.err.println("[TaskVerse DBConnection] Error: Database driver initialization failed.");
        }
    }

    /**
     * Resolves a configuration value with the following precedence:
     * 1. Environment variable (envName)
     * 2. JVM System property (sysPropName or envName)
     * 3. Properties loaded from db.properties (sysPropName)
     */
    private static String resolveConfigValue(String envName, String sysPropName, Properties props) {
        // 1. Environment variable has highest priority
        String envVal = System.getenv(envName);
        if (envVal != null) {
            return envVal;
        }

        // 2. JVM System properties (-Ddb.url or -DDB_URL)
        if (sysPropName != null) {
            String sysVal = System.getProperty(sysPropName);
            if (sysVal != null) {
                return sysVal;
            }
        }
        String sysEnvVal = System.getProperty(envName);
        if (sysEnvVal != null) {
            return sysEnvVal;
        }

        // 3. Values from db.properties file
        if (props != null && sysPropName != null) {
            return props.getProperty(sysPropName);
        }

        return null;
    }

    private static String getTrimmed(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * Obtains a new database connection.
     * Throws a safe, generic SQLException message if configuration or connection is unavailable,
     * ensuring internal configuration details, connection strings, or raw exception messages
     * are never exposed to HTTP clients or callers.
     *
     * @return Connection object to taskverse_db
     * @throws SQLException if configuration or database connection is unavailable
     */
    public static Connection getConnection() throws SQLException {
        if (!initialized) {
            loadConfiguration();
        }
        if (initError != null) {
            throw new SQLException("Database configuration is unavailable.");
        }
        try {
            return DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            String sqlState = e.getSQLState();
            System.err.println("[TaskVerse DBConnection] Failed to establish database connection. SQLState: "
                    + (sqlState != null ? sqlState : "N/A") + ", ErrorCode: " + e.getErrorCode());
            throw new SQLException("Database connection is unavailable.", sqlState, e.getErrorCode(), e);
        }
    }

    /**
     * Tests the database connection and returns a sanitized status message.
     * Prevents leakage of database catalog, driver name, database URLs, credentials, or raw exception details.
     */
    public static String testConnection() {
        if (!initialized) {
            loadConfiguration();
        }
        if (initError != null) {
            return "Configuration Error: Database configuration is incomplete or invalid.";
        }
        try (Connection conn = getConnection()) {
            if (conn != null && !conn.isClosed()) {
                return "SUCCESS: Database connection established.";
            }
            return "FAILED: Connection was null or closed.";
        } catch (SQLException e) {
            String sqlState = e.getSQLState();
            if (sqlState != null && !sqlState.trim().isEmpty()) {
                return "Connection Failed: Database service unavailable or access denied (SQLState: " + sqlState + ")";
            }
            return "Connection Failed: Database service unavailable or access denied";
        }
    }

    public static String getUrl() {
        if (!initialized && initError == null) {
            loadConfiguration();
        }
        return url;
    }

    public static String getUser() {
        if (!initialized && initError == null) {
            loadConfiguration();
        }
        return user;
    }
}
