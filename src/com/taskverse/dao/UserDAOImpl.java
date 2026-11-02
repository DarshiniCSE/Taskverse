package com.taskverse.dao;

import com.taskverse.model.User;
import com.taskverse.util.DBConnection;

import java.sql.*;

/**
 * UserDAO Implementation
 * Manages user authentication records and legacy data ownership adoption.
 */
public class UserDAOImpl implements UserDAO {

    @Override
    public User createUser(User user) throws SQLException {
        String sql = "INSERT INTO users (username, email, password_hash, password_salt, role, is_active) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, user.getUsername().trim());
            ps.setString(2, user.getEmail().trim().toLowerCase());
            ps.setString(3, user.getPasswordHash());
            ps.setString(4, user.getPasswordSalt());
            ps.setString(5, user.getRole());
            ps.setBoolean(6, user.isActive());

            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new SQLException("Creating user failed, no rows affected.");
            }

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    user.setUserId(generatedKeys.getInt(1));
                }
            }
        }
        return user;
    }

    @Override
    public User getUserByEmail(String email) throws SQLException {
        if (email == null || email.trim().isEmpty()) {
            return null;
        }

        String sql = "SELECT user_id, username, email, password_hash, password_salt, role, is_active, created_at, updated_at " +
                     "FROM users WHERE email = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, email.trim().toLowerCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToUser(rs);
                }
            }
        }
        return null;
    }

    @Override
    public User getUserByUsername(String username) throws SQLException {
        if (username == null || username.trim().isEmpty()) {
            return null;
        }

        String sql = "SELECT user_id, username, email, password_hash, password_salt, role, is_active, created_at, updated_at " +
                     "FROM users WHERE username = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToUser(rs);
                }
            }
        }
        return null;
    }

    @Override
    public User getUserById(int userId) throws SQLException {
        String sql = "SELECT user_id, username, email, password_hash, password_salt, role, is_active, created_at, updated_at " +
                     "FROM users WHERE user_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToUser(rs);
                }
            }
        }
        return null;
    }

    @Override
    public int getOwnerCount() throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE role = 'OWNER'";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    @Override
    public int adoptLegacyRecords(int ownerUserId) throws SQLException {
        Connection conn = null;
        int totalAdopted = 0;

        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            // 1. Adopt unowned categories (excluding system categories)
            String catSql = "UPDATE categories SET user_id = ? WHERE user_id IS NULL AND is_system = FALSE";
            try (PreparedStatement ps = conn.prepareStatement(catSql)) {
                ps.setInt(1, ownerUserId);
                totalAdopted += ps.executeUpdate();
            }

            // 2. Adopt unowned tasks
            String taskSql = "UPDATE tasks SET user_id = ? WHERE user_id IS NULL";
            try (PreparedStatement ps = conn.prepareStatement(taskSql)) {
                ps.setInt(1, ownerUserId);
                totalAdopted += ps.executeUpdate();
            }

            // 3. Adopt unowned notes
            String noteSql = "UPDATE notes SET user_id = ? WHERE user_id IS NULL";
            try (PreparedStatement ps = conn.prepareStatement(noteSql)) {
                ps.setInt(1, ownerUserId);
                totalAdopted += ps.executeUpdate();
            }

            // 4. Adopt unowned task_points
            String pointsSql = "UPDATE task_points SET user_id = ? WHERE user_id IS NULL";
            try (PreparedStatement ps = conn.prepareStatement(pointsSql)) {
                ps.setInt(1, ownerUserId);
                totalAdopted += ps.executeUpdate();
            }

            conn.commit();
            return totalAdopted;

        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignored) {}
            }
            throw e;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignored) {}
            }
        }
    }

    private User mapRowToUser(ResultSet rs) throws SQLException {
        return new User(
                rs.getInt("user_id"),
                rs.getString("username"),
                rs.getString("email"),
                rs.getString("password_hash"),
                rs.getString("password_salt"),
                rs.getString("role"),
                rs.getBoolean("is_active"),
                rs.getTimestamp("created_at"),
                rs.getTimestamp("updated_at")
        );
    }
}
