package com.taskverse.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * DBConnection Utility
 * Manages database connections using JDBC.
 * 
 * Safety & Security Rules:
 * - Credentials are read dynamically from 'db.properties' (never hardcoded in source code).
 * - Excluded from version control.
 * - Follows OOP Single Responsibility Principle (dedicated solely to DB connectivity).
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
     * Loads database configuration from classpath or local fallback file.
     */
    private static synchronized void loadConfiguration() {
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

            // 2. Fallback: try loading directly from project directory if running standalone/testing
            if (input == null) {
                File localFile = new File("db/db.properties");
                if (localFile.exists()) {
                    input = new FileInputStream(localFile);
                } else {
                    File absFile = new File("C:/Users/darsh/Downloads/Taskverse/db/db.properties");
                    if (absFile.exists()) {
                        input = new FileInputStream(absFile);
                    }
                }
            }

            if (input != null) {
                props.load(input);
                url = props.getProperty("db.url", "jdbc:mysql://localhost:3306/taskverse_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8");
                user = props.getProperty("db.user", "root");
                password = props.getProperty("db.password", "");
                if (props.containsKey("db.driver")) {
                    driver = props.getProperty("db.driver");
                }
                
                // Explicitly load MySQL JDBC Driver class
                Class.forName(driver);
                initialized = true;
                initError = null;
            } else {
                initError = "Configuration file 'db.properties' not found in classpath or db/ directory.";
                System.err.println("[TaskVerse DBConnection] " + initError);
            }
        } catch (ClassNotFoundException e) {
            initError = "MySQL JDBC Driver not found in classpath: " + e.getMessage();
            System.err.println("[TaskVerse DBConnection] " + initError);
        } catch (Exception e) {
            initError = "Failed to load database properties: " + e.getMessage();
            System.err.println("[TaskVerse DBConnection] " + initError);
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    /**
     * Obtains a new database connection.
     * @return Connection object to taskverse_db
     * @throws SQLException if a database access error occurs
     */
    public static Connection getConnection() throws SQLException {
        if (!initialized) {
            loadConfiguration();
        }
        if (initError != null) {
            throw new SQLException("Database configuration error: " + initError);
        }
        return DriverManager.getConnection(url, user, password);
    }

    /**
     * Tests the database connection and returns status message.
     */
    public static String testConnection() {
        if (!initialized) {
            loadConfiguration();
        }
        if (initError != null) {
            return "Configuration Error: " + initError;
        }
        try (Connection conn = getConnection()) {
            if (conn != null && !conn.isClosed()) {
                return "SUCCESS: Connected to MySQL database '" + conn.getCatalog() + "' via " + conn.getMetaData().getDriverName();
            }
            return "FAILED: Connection was null or closed.";
        } catch (SQLException e) {
            return "Connection Failed: " + e.getMessage() + " (SQLState: " + e.getSQLState() + ")";
        }
    }

    public static String getUrl() {
        return url;
    }

    public static String getUser() {
        return user;
    }
}
