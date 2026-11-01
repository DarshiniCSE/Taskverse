package com.taskverse.dao;

import com.taskverse.model.PointResult;
import com.taskverse.model.Task;
import com.taskverse.model.TaskStats;
import com.taskverse.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * TaskDAO Implementation
 * Executes SQL queries using JDBC PreparedStatements and transactions.
 * Manages task operations and the Task Points System.
 * 
 * Demonstrates OOP Concepts:
 * - Polymorphism: Implements the TaskDAO interface.
 * - Resource Management: Uses try-with-resources for automatic closing of JDBC objects.
 * - Concurrency & Safety: Uses database transactions (setAutoCommit(false), FOR UPDATE locks)
 *   to guarantee atomicity and prevent duplicate point awards.
 * - Security: Uses PreparedStatement to prevent SQL injection.
 */
public class TaskDAOImpl implements TaskDAO {

    /**
     * Helper to compute points based on task priority:
     * - Low: 10 points
     * - Medium: 20 points
     * - High: 30 points
     */
    public static int calculatePoints(String priority) {
        if (priority == null) return 20;
        switch (priority.trim().toLowerCase()) {
            case "low":
                return 10;
            case "high":
                return 30;
            case "medium":
            default:
                return 20;
        }
    }

    @Override
    public Task createTask(Task task) throws SQLException {
        String insertTaskSql = "INSERT INTO tasks (title, description, priority, status, category, due_date, completed_at) VALUES (?, ?, ?, ?, ?, ?, ?)";
        Connection conn = null;

        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement ps = conn.prepareStatement(insertTaskSql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, task.getTitle());
                ps.setString(2, task.getDescription());
                ps.setString(3, task.getPriority());
                ps.setString(4, task.getStatus());
                ps.setString(5, task.getCategory());

                if (task.getDueDate() != null && !task.getDueDate().trim().isEmpty()) {
                    ps.setDate(6, Date.valueOf(task.getDueDate().trim()));
                } else {
                    ps.setNull(6, Types.DATE);
                }

                if ("Completed".equalsIgnoreCase(task.getStatus() != null ? task.getStatus().trim() : "")) {
                    ps.setTimestamp(7, new Timestamp(System.currentTimeMillis()));
                } else {
                    ps.setNull(7, Types.TIMESTAMP);
                }

                int affectedRows = ps.executeUpdate();
                if (affectedRows == 0) {
                    throw new SQLException("Creating task failed, no rows affected.");
                }

                try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        task.setTaskId(generatedKeys.getInt(1));
                    }
                }
            }

            // Rule: Do not award points when a task is created with Completed status.
            // Also seed task_points with 0 points so reopening and completing later cannot earn duplicate points.
            if ("Completed".equalsIgnoreCase(task.getStatus() != null ? task.getStatus().trim() : "")) {
                String zeroPointSql = "INSERT IGNORE INTO task_points (task_id, task_title, priority, points) VALUES (?, ?, ?, 0)";
                try (PreparedStatement psPoints = conn.prepareStatement(zeroPointSql)) {
                    psPoints.setInt(1, task.getTaskId());
                    psPoints.setString(2, task.getTitle());
                    psPoints.setString(3, task.getPriority() != null ? task.getPriority() : "Medium");
                    psPoints.executeUpdate();
                }
            }

            conn.commit();
            return task;
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

    @Override
    public List<Task> getAllTasks() throws SQLException {
        List<Task> tasks = new ArrayList<>();
        String sql = "SELECT task_id, title, description, priority, status, category, due_date, completed_at, created_at, updated_at " +
                     "FROM tasks ORDER BY created_at DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                tasks.add(mapRowToTask(rs));
            }
        }
        return tasks;
    }

    @Override
    public Task getTaskById(int taskId) throws SQLException {
        String sql = "SELECT task_id, title, description, priority, status, category, due_date, completed_at, created_at, updated_at " +
                     "FROM tasks WHERE task_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, taskId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToTask(rs);
                }
            }
        }
        return null;
    }

    @Override
    public boolean updateTask(Task task) throws SQLException {
        PointResult result = updateTaskWithPoints(task);
        return result.isSuccess();
    }

    @Override
    public PointResult updateTaskWithPoints(Task task) throws SQLException {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            // 1. Fetch current status & priority using row lock (FOR UPDATE)
            String selectSql = "SELECT task_id, title, priority, status FROM tasks WHERE task_id = ? FOR UPDATE";
            String currentStatus = null;
            try (PreparedStatement psSelect = conn.prepareStatement(selectSql)) {
                psSelect.setInt(1, task.getTaskId());
                try (ResultSet rs = psSelect.executeQuery()) {
                    if (rs.next()) {
                        currentStatus = rs.getString("status");
                    } else {
                        conn.rollback();
                        return new PointResult(false, 0, getTotalPointsInternal(conn), "Task not found with ID: " + task.getTaskId());
                    }
                }
            }

            int pointsAwarded = 0;
            String newStatus = task.getStatus() != null ? task.getStatus().trim() : "";
            boolean isCompletingNow = "Completed".equalsIgnoreCase(newStatus) && !"Completed".equalsIgnoreCase(currentStatus != null ? currentStatus.trim() : "");

            // 2. Evaluate Points
            if (isCompletingNow) {
                // Check if points were already awarded for this task
                String checkPointSql = "SELECT point_id FROM task_points WHERE task_id = ?";
                boolean alreadyAwarded = false;
                try (PreparedStatement psCheck = conn.prepareStatement(checkPointSql)) {
                    psCheck.setInt(1, task.getTaskId());
                    try (ResultSet rsCheck = psCheck.executeQuery()) {
                        if (rsCheck.next()) {
                            alreadyAwarded = true;
                        }
                    }
                }

                if (!alreadyAwarded) {
                    pointsAwarded = calculatePoints(task.getPriority());
                    String insertPointSql = "INSERT INTO task_points (task_id, task_title, priority, points) VALUES (?, ?, ?, ?)";
                    try (PreparedStatement psInsert = conn.prepareStatement(insertPointSql)) {
                        psInsert.setInt(1, task.getTaskId());
                        psInsert.setString(2, task.getTitle());
                        psInsert.setString(3, task.getPriority() != null ? task.getPriority() : "Medium");
                        psInsert.setInt(4, pointsAwarded);
                        psInsert.executeUpdate();
                    }
                }
            }

            // 3. Update the Task
            // Accurate completion timestamp preservation:
            // - If newly transitioning to Completed: completed_at = CURRENT_TIMESTAMP
            // - If transitioning away from Completed: completed_at = NULL
            // - If already Completed and remains Completed (e.g. title/description edited): PRESERVE original completed_at!
            String updateSql;
            if (isCompletingNow) {
                updateSql = "UPDATE tasks SET title = ?, description = ?, priority = ?, status = ?, category = ?, due_date = ?, completed_at = CURRENT_TIMESTAMP WHERE task_id = ?";
            } else if ("Completed".equalsIgnoreCase(currentStatus) && !"Completed".equalsIgnoreCase(newStatus)) {
                updateSql = "UPDATE tasks SET title = ?, description = ?, priority = ?, status = ?, category = ?, due_date = ?, completed_at = NULL WHERE task_id = ?";
            } else {
                updateSql = "UPDATE tasks SET title = ?, description = ?, priority = ?, status = ?, category = ?, due_date = ? WHERE task_id = ?";
            }
            try (PreparedStatement psUpdate = conn.prepareStatement(updateSql)) {
                psUpdate.setString(1, task.getTitle());
                psUpdate.setString(2, task.getDescription());
                psUpdate.setString(3, task.getPriority());
                psUpdate.setString(4, task.getStatus());
                psUpdate.setString(5, task.getCategory());

                if (task.getDueDate() != null && !task.getDueDate().trim().isEmpty()) {
                    psUpdate.setDate(6, Date.valueOf(task.getDueDate().trim()));
                } else {
                    psUpdate.setNull(6, Types.DATE);
                }

                psUpdate.setInt(7, task.getTaskId());
                int rows = psUpdate.executeUpdate();
                if (rows == 0) {
                    conn.rollback();
                    return new PointResult(false, 0, getTotalPointsInternal(conn), "Task update failed.");
                }
            }

            int totalPoints = getTotalPointsInternal(conn);
            conn.commit();
            return new PointResult(true, pointsAwarded, totalPoints, "Task updated successfully.");

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

    @Override
    public boolean deleteTask(int taskId) throws SQLException {
        // Rule: Do not allow task deletion to award points.
        // Task is deleted from tasks; points history is preserved in task_points table.
        String sql = "DELETE FROM tasks WHERE task_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, taskId);
            return ps.executeUpdate() > 0;
        }
    }

    @Override
    public boolean updateTaskStatus(int taskId, String status) throws SQLException {
        PointResult result = updateTaskStatusWithPoints(taskId, status);
        return result.isSuccess();
    }

    @Override
    public PointResult updateTaskStatusWithPoints(int taskId, String status) throws SQLException {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            // 1. Row-level lock on the target task
            String selectSql = "SELECT task_id, title, priority, status FROM tasks WHERE task_id = ? FOR UPDATE";
            String currentStatus = null;
            String currentPriority = null;
            String taskTitle = null;

            try (PreparedStatement psSelect = conn.prepareStatement(selectSql)) {
                psSelect.setInt(1, taskId);
                try (ResultSet rs = psSelect.executeQuery()) {
                    if (rs.next()) {
                        taskTitle = rs.getString("title");
                        currentPriority = rs.getString("priority");
                        currentStatus = rs.getString("status");
                    } else {
                        conn.rollback();
                        return new PointResult(false, 0, getTotalPointsInternal(conn), "Task not found with ID: " + taskId);
                    }
                }
            }

            int pointsAwarded = 0;
            String newStatus = status != null ? status.trim() : "";
            boolean isCompletingNow = "Completed".equalsIgnoreCase(newStatus) && !"Completed".equalsIgnoreCase(currentStatus != null ? currentStatus.trim() : "");

            // 2. Evaluate Points
            if (isCompletingNow) {
                // Check if points were already awarded for this task (prevent duplicate points if reopened)
                String checkPointSql = "SELECT point_id FROM task_points WHERE task_id = ?";
                boolean alreadyAwarded = false;
                try (PreparedStatement psCheck = conn.prepareStatement(checkPointSql)) {
                    psCheck.setInt(1, taskId);
                    try (ResultSet rsCheck = psCheck.executeQuery()) {
                        if (rsCheck.next()) {
                            alreadyAwarded = true;
                        }
                    }
                }

                if (!alreadyAwarded) {
                    pointsAwarded = calculatePoints(currentPriority);
                    String insertPointSql = "INSERT INTO task_points (task_id, task_title, priority, points) VALUES (?, ?, ?, ?)";
                    try (PreparedStatement psInsert = conn.prepareStatement(insertPointSql)) {
                        psInsert.setInt(1, taskId);
                        psInsert.setString(2, taskTitle != null ? taskTitle : "Task #" + taskId);
                        psInsert.setString(3, currentPriority != null ? currentPriority : "Medium");
                        psInsert.setInt(4, pointsAwarded);
                        psInsert.executeUpdate();
                    }
                }
            }

            // 3. Update Status
            String updateSql;
            if (isCompletingNow) {
                updateSql = "UPDATE tasks SET status = ?, completed_at = CURRENT_TIMESTAMP WHERE task_id = ?";
            } else if ("Completed".equalsIgnoreCase(currentStatus) && !"Completed".equalsIgnoreCase(newStatus)) {
                updateSql = "UPDATE tasks SET status = ?, completed_at = NULL WHERE task_id = ?";
            } else {
                updateSql = "UPDATE tasks SET status = ? WHERE task_id = ?";
            }
            try (PreparedStatement psUpdate = conn.prepareStatement(updateSql)) {
                psUpdate.setString(1, newStatus);
                psUpdate.setInt(2, taskId);
                psUpdate.executeUpdate();
            }

            int totalPoints = getTotalPointsInternal(conn);
            conn.commit();

            String msg = pointsAwarded > 0
                ? "Task marked Completed! +" + pointsAwarded + " points awarded."
                : "Task status updated to: " + newStatus;

            return new PointResult(true, pointsAwarded, totalPoints, msg);

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

    @Override
    public TaskStats getTaskStats() throws SQLException {
        String statsSql = "SELECT " +
                          "COUNT(*) AS total, " +
                          "COALESCE(SUM(CASE WHEN LOWER(TRIM(status)) = 'pending' THEN 1 ELSE 0 END), 0) AS pending, " +
                          "COALESCE(SUM(CASE WHEN LOWER(TRIM(status)) = 'in progress' THEN 1 ELSE 0 END), 0) AS in_progress, " +
                          "COALESCE(SUM(CASE WHEN LOWER(TRIM(status)) = 'completed' THEN 1 ELSE 0 END), 0) AS completed " +
                          "FROM tasks";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(statsSql);
             ResultSet rs = ps.executeQuery()) {

            int totalPoints = getTotalPointsInternal(conn);

            if (rs.next()) {
                return new TaskStats(
                    rs.getInt("total"),
                    rs.getInt("pending"),
                    rs.getInt("in_progress"),
                    rs.getInt("completed"),
                    totalPoints
                );
            }
            return new TaskStats(0, 0, 0, 0, totalPoints);
        }
    }

    @Override
    public int getTotalPoints() throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return getTotalPointsInternal(conn);
        }
    }

    /**
     * Internal helper to sum points across an active or pooled Connection.
     */
    private int getTotalPointsInternal(Connection conn) {
        String sumSql = "SELECT COALESCE(SUM(points), 0) FROM task_points";
        try (PreparedStatement ps = conn.prepareStatement(sumSql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("[TaskVerse] Error computing total points: " + e.getMessage());
        }
        return 0;
    }

    /**
     * Helper method to map a ResultSet row to a Task object.
     */
    private Task mapRowToTask(ResultSet rs) throws SQLException {
        Date dueDate = rs.getDate("due_date");
        Timestamp completedAt = rs.getTimestamp("completed_at");
        Timestamp createdAt = rs.getTimestamp("created_at");
        Timestamp updatedAt = rs.getTimestamp("updated_at");

        return new Task(
            rs.getInt("task_id"),
            rs.getString("title"),
            rs.getString("description"),
            rs.getString("priority"),
            rs.getString("status"),
            rs.getString("category"),
            dueDate != null ? dueDate.toString() : null,
            completedAt != null ? completedAt.toString() : null,
            createdAt != null ? createdAt.toString() : null,
            updatedAt != null ? updatedAt.toString() : null
        );
    }
}
