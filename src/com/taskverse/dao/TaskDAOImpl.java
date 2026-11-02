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
 * Executes JDBC queries scoped strictly to the authenticated user.
 */
public class TaskDAOImpl implements TaskDAO {

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
    public Task createTask(Task task, int userId) throws SQLException {
        String insertTaskSql = "INSERT INTO tasks (user_id, title, description, priority, status, category, due_date, completed_at) " +
                               "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        Connection conn = null;

        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement ps = conn.prepareStatement(insertTaskSql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, userId);
                ps.setString(2, task.getTitle());
                ps.setString(3, task.getDescription());
                ps.setString(4, task.getPriority());
                ps.setString(5, task.getStatus());
                ps.setString(6, task.getCategory());

                if (task.getDueDate() != null && !task.getDueDate().trim().isEmpty()) {
                    ps.setDate(7, Date.valueOf(task.getDueDate().trim()));
                } else {
                    ps.setNull(7, Types.DATE);
                }

                if ("Completed".equalsIgnoreCase(task.getStatus() != null ? task.getStatus().trim() : "")) {
                    ps.setTimestamp(8, new Timestamp(System.currentTimeMillis()));
                } else {
                    ps.setNull(8, Types.TIMESTAMP);
                }

                int affectedRows = ps.executeUpdate();
                if (affectedRows == 0) {
                    throw new SQLException("Creating task failed, no rows affected.");
                }

                try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        task.setTaskId(generatedKeys.getInt(1));
                        task.setUserId(userId);
                    }
                }
            }

            // Seed task_points with 0 if created with Completed status
            if ("Completed".equalsIgnoreCase(task.getStatus() != null ? task.getStatus().trim() : "")) {
                String zeroPointSql = "INSERT IGNORE INTO task_points (user_id, task_id, task_title, priority, points) VALUES (?, ?, ?, ?, 0)";
                try (PreparedStatement psPoints = conn.prepareStatement(zeroPointSql)) {
                    psPoints.setInt(1, userId);
                    psPoints.setInt(2, task.getTaskId());
                    psPoints.setString(3, task.getTitle());
                    psPoints.setString(4, task.getPriority() != null ? task.getPriority() : "Medium");
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
    public List<Task> getAllTasks(int userId) throws SQLException {
        List<Task> tasks = new ArrayList<>();
        String sql = "SELECT task_id, user_id, title, description, priority, status, category, due_date, completed_at, created_at, updated_at " +
                     "FROM tasks WHERE user_id = ? ORDER BY created_at DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tasks.add(mapRowToTask(rs));
                }
            }
        }
        return tasks;
    }

    @Override
    public Task getTaskById(int taskId, int userId) throws SQLException {
        String sql = "SELECT task_id, user_id, title, description, priority, status, category, due_date, completed_at, created_at, updated_at " +
                     "FROM tasks WHERE task_id = ? AND user_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, taskId);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToTask(rs);
                }
            }
        }
        return null;
    }

    @Override
    public boolean updateTask(Task task, int userId) throws SQLException {
        PointResult result = updateTaskWithPoints(task, userId);
        return result.isSuccess();
    }

    @Override
    public PointResult updateTaskWithPoints(Task task, int userId) throws SQLException {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            // 1. Fetch current status & priority using row lock, strictly scoped to userId
            String selectSql = "SELECT task_id, title, priority, status FROM tasks WHERE task_id = ? AND user_id = ? FOR UPDATE";
            String currentStatus = null;
            try (PreparedStatement psSelect = conn.prepareStatement(selectSql)) {
                psSelect.setInt(1, task.getTaskId());
                psSelect.setInt(2, userId);
                try (ResultSet rs = psSelect.executeQuery()) {
                    if (rs.next()) {
                        currentStatus = rs.getString("status");
                    } else {
                        conn.rollback();
                        return new PointResult(false, 0, getTotalPointsInternal(conn, userId), "Task not found with ID: " + task.getTaskId());
                    }
                }
            }

            int pointsAwarded = 0;
            String newStatus = task.getStatus() != null ? task.getStatus().trim() : "";
            boolean isCompletingNow = "Completed".equalsIgnoreCase(newStatus) && !"Completed".equalsIgnoreCase(currentStatus != null ? currentStatus.trim() : "");

            // 2. Evaluate Points
            if (isCompletingNow) {
                String checkPointSql = "SELECT point_id FROM task_points WHERE task_id = ? AND user_id = ?";
                boolean alreadyAwarded = false;
                try (PreparedStatement psCheck = conn.prepareStatement(checkPointSql)) {
                    psCheck.setInt(1, task.getTaskId());
                    psCheck.setInt(2, userId);
                    try (ResultSet rsCheck = psCheck.executeQuery()) {
                        if (rsCheck.next()) {
                            alreadyAwarded = true;
                        }
                    }
                }

                if (!alreadyAwarded) {
                    pointsAwarded = calculatePoints(task.getPriority());
                    String insertPointSql = "INSERT INTO task_points (user_id, task_id, task_title, priority, points) VALUES (?, ?, ?, ?, ?)";
                    try (PreparedStatement psInsert = conn.prepareStatement(insertPointSql)) {
                        psInsert.setInt(1, userId);
                        psInsert.setInt(2, task.getTaskId());
                        psInsert.setString(3, task.getTitle());
                        psInsert.setString(4, task.getPriority() != null ? task.getPriority() : "Medium");
                        psInsert.setInt(5, pointsAwarded);
                        psInsert.executeUpdate();
                    }
                }
            }

            // 3. Update the Task
            String updateSql;
            if (isCompletingNow) {
                updateSql = "UPDATE tasks SET title = ?, description = ?, priority = ?, status = ?, category = ?, due_date = ?, completed_at = CURRENT_TIMESTAMP WHERE task_id = ? AND user_id = ?";
            } else if ("Completed".equalsIgnoreCase(currentStatus) && !"Completed".equalsIgnoreCase(newStatus)) {
                updateSql = "UPDATE tasks SET title = ?, description = ?, priority = ?, status = ?, category = ?, due_date = ?, completed_at = NULL WHERE task_id = ? AND user_id = ?";
            } else {
                updateSql = "UPDATE tasks SET title = ?, description = ?, priority = ?, status = ?, category = ?, due_date = ? WHERE task_id = ? AND user_id = ?";
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
                psUpdate.setInt(8, userId);
                int rows = psUpdate.executeUpdate();
                if (rows == 0) {
                    conn.rollback();
                    return new PointResult(false, 0, getTotalPointsInternal(conn, userId), "Task update failed.");
                }
            }

            int totalPoints = getTotalPointsInternal(conn, userId);
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
    public boolean deleteTask(int taskId, int userId) throws SQLException {
        String sql = "DELETE FROM tasks WHERE task_id = ? AND user_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, taskId);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        }
    }

    @Override
    public boolean updateTaskStatus(int taskId, String status, int userId) throws SQLException {
        PointResult result = updateTaskStatusWithPoints(taskId, status, userId);
        return result.isSuccess();
    }

    @Override
    public PointResult updateTaskStatusWithPoints(int taskId, String status, int userId) throws SQLException {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            String selectSql = "SELECT task_id, title, priority, status FROM tasks WHERE task_id = ? AND user_id = ? FOR UPDATE";
            String currentStatus = null;
            String currentPriority = null;
            String taskTitle = null;

            try (PreparedStatement psSelect = conn.prepareStatement(selectSql)) {
                psSelect.setInt(1, taskId);
                psSelect.setInt(2, userId);
                try (ResultSet rs = psSelect.executeQuery()) {
                    if (rs.next()) {
                        taskTitle = rs.getString("title");
                        currentPriority = rs.getString("priority");
                        currentStatus = rs.getString("status");
                    } else {
                        conn.rollback();
                        return new PointResult(false, 0, getTotalPointsInternal(conn, userId), "Task not found with ID: " + taskId);
                    }
                }
            }

            int pointsAwarded = 0;
            String newStatus = status != null ? status.trim() : "";
            boolean isCompletingNow = "Completed".equalsIgnoreCase(newStatus) && !"Completed".equalsIgnoreCase(currentStatus != null ? currentStatus.trim() : "");

            if (isCompletingNow) {
                String checkPointSql = "SELECT point_id FROM task_points WHERE task_id = ? AND user_id = ?";
                boolean alreadyAwarded = false;
                try (PreparedStatement psCheck = conn.prepareStatement(checkPointSql)) {
                    psCheck.setInt(1, taskId);
                    psCheck.setInt(2, userId);
                    try (ResultSet rsCheck = psCheck.executeQuery()) {
                        if (rsCheck.next()) {
                            alreadyAwarded = true;
                        }
                    }
                }

                if (!alreadyAwarded) {
                    pointsAwarded = calculatePoints(currentPriority);
                    String insertPointSql = "INSERT INTO task_points (user_id, task_id, task_title, priority, points) VALUES (?, ?, ?, ?, ?)";
                    try (PreparedStatement psInsert = conn.prepareStatement(insertPointSql)) {
                        psInsert.setInt(1, userId);
                        psInsert.setInt(2, taskId);
                        psInsert.setString(3, taskTitle != null ? taskTitle : "Task #" + taskId);
                        psInsert.setString(4, currentPriority != null ? currentPriority : "Medium");
                        psInsert.setInt(5, pointsAwarded);
                        psInsert.executeUpdate();
                    }
                }
            }

            String updateSql;
            if (isCompletingNow) {
                updateSql = "UPDATE tasks SET status = ?, completed_at = CURRENT_TIMESTAMP WHERE task_id = ? AND user_id = ?";
            } else if ("Completed".equalsIgnoreCase(currentStatus) && !"Completed".equalsIgnoreCase(newStatus)) {
                updateSql = "UPDATE tasks SET status = ?, completed_at = NULL WHERE task_id = ? AND user_id = ?";
            } else {
                updateSql = "UPDATE tasks SET status = ? WHERE task_id = ? AND user_id = ?";
            }
            try (PreparedStatement psUpdate = conn.prepareStatement(updateSql)) {
                psUpdate.setString(1, newStatus);
                psUpdate.setInt(2, taskId);
                psUpdate.setInt(3, userId);
                psUpdate.executeUpdate();
            }

            int totalPoints = getTotalPointsInternal(conn, userId);
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
    public TaskStats getTaskStats(int userId) throws SQLException {
        String statsSql = "SELECT " +
                          "COUNT(*) AS total, " +
                          "COALESCE(SUM(CASE WHEN LOWER(TRIM(status)) = 'pending' THEN 1 ELSE 0 END), 0) AS pending, " +
                          "COALESCE(SUM(CASE WHEN LOWER(TRIM(status)) = 'in progress' THEN 1 ELSE 0 END), 0) AS in_progress, " +
                          "COALESCE(SUM(CASE WHEN LOWER(TRIM(status)) = 'completed' THEN 1 ELSE 0 END), 0) AS completed " +
                          "FROM tasks WHERE user_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(statsSql)) {

            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                int totalPoints = getTotalPointsInternal(conn, userId);

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
    }

    @Override
    public int getTotalPoints(int userId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return getTotalPointsInternal(conn, userId);
        }
    }

    private int getTotalPointsInternal(Connection conn, int userId) {
        String sumSql = "SELECT COALESCE(SUM(points), 0) FROM task_points WHERE user_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sumSql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("[TaskVerse] Error computing total points: " + e.getMessage());
        }
        return 0;
    }

    private Task mapRowToTask(ResultSet rs) throws SQLException {
        Date dueDate = rs.getDate("due_date");
        Timestamp completedAt = rs.getTimestamp("completed_at");
        Timestamp createdAt = rs.getTimestamp("created_at");
        Timestamp updatedAt = rs.getTimestamp("updated_at");

        Task task = new Task(
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
        task.setUserId(rs.getInt("user_id"));
        return task;
    }
}
