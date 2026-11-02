package com.taskverse.dao;

import com.taskverse.model.PointResult;
import com.taskverse.model.Task;
import com.taskverse.model.TaskStats;
import java.sql.SQLException;
import java.util.List;

/**
 * TaskDAO Interface
 * Defines the contract for data operations on Tasks and Task Points with strict per-user scoping.
 */
public interface TaskDAO {

    /**
     * Inserts a new task record belonging to the authenticated user.
     * @param task the Task object containing input fields
     * @param userId the ID of the authenticated user
     * @return the created Task populated with its generated database ID and userId
     * @throws SQLException on database error
     */
    Task createTask(Task task, int userId) throws SQLException;

    /**
     * Retrieves all tasks belonging to the authenticated user.
     * @param userId the ID of the authenticated user
     * @return List of Task objects
     * @throws SQLException on database error
     */
    List<Task> getAllTasks(int userId) throws SQLException;

    /**
     * Retrieves a single task by ID, scoped to the authenticated user.
     * @param taskId the task ID
     * @param userId the ID of the authenticated user
     * @return the Task object if found and owned by the user, or null
     * @throws SQLException on database error
     */
    Task getTaskById(int taskId, int userId) throws SQLException;

    /**
     * Updates an existing task owned by the authenticated user.
     * @param task the Task with updated fields
     * @param userId the ID of the authenticated user
     * @return true if updated successfully, false if not found or not owned
     * @throws SQLException on database error
     */
    boolean updateTask(Task task, int userId) throws SQLException;

    /**
     * Updates an existing task and atomically evaluates point rewards if completing a non-completed task.
     * @param task the Task with updated fields
     * @param userId the ID of the authenticated user
     * @return PointResult containing success flag, points awarded, and total points
     * @throws SQLException on database error
     */
    PointResult updateTaskWithPoints(Task task, int userId) throws SQLException;

    /**
     * Deletes a task by ID, only if owned by the authenticated user.
     * @param taskId the ID of the task to delete
     * @param userId the ID of the authenticated user
     * @return true if deleted, false if not found or not owned
     * @throws SQLException on database error
     */
    boolean deleteTask(int taskId, int userId) throws SQLException;

    /**
     * Updates the status of a specific task owned by the authenticated user.
     * @param taskId the task ID
     * @param status the new status ('Pending', 'In Progress', 'Completed')
     * @param userId the ID of the authenticated user
     * @return true if updated, false if not found or not owned
     * @throws SQLException on database error
     */
    boolean updateTaskStatus(int taskId, String status, int userId) throws SQLException;

    /**
     * Updates task status and atomically evaluates points for the authenticated user.
     * @param taskId the task ID
     * @param status the new status
     * @param userId the ID of the authenticated user
     * @return PointResult with success status, points awarded, and total points
     * @throws SQLException on database error
     */
    PointResult updateTaskStatusWithPoints(int taskId, String status, int userId) throws SQLException;

    /**
     * Computes real-time task statistics and total earned points for the authenticated user.
     * @param userId the ID of the authenticated user
     * @return TaskStats object for the user
     * @throws SQLException on database error
     */
    TaskStats getTaskStats(int userId) throws SQLException;

    /**
     * Retrieves the total points earned by the authenticated user.
     * @param userId the ID of the authenticated user
     * @return total earned points
     * @throws SQLException on database error
     */
    int getTotalPoints(int userId) throws SQLException;
}
