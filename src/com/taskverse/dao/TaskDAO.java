package com.taskverse.dao;

import com.taskverse.model.PointResult;
import com.taskverse.model.Task;
import com.taskverse.model.TaskStats;
import java.sql.SQLException;
import java.util.List;

/**
 * TaskDAO Interface
 * Defines the contract for data operations on Tasks and Task Points.
 * 
 * Demonstrates OOP Concept: Abstraction & Interface Design
 * - Decouples high-level business logic (servlets) from low-level database operations (SQL queries).
 * - Servlets interact only through this interface, not direct SQL queries.
 */
public interface TaskDAO {

    /**
     * Inserts a new task record into taskverse_db.
     * If created with 'Completed' status, records 0 points to prevent future duplicate rewards.
     * @param task the Task object containing input fields
     * @return the created Task populated with its generated database ID
     * @throws SQLException on database error
     */
    Task createTask(Task task) throws SQLException;

    /**
     * Retrieves all tasks ordered by creation time descending.
     * @return List of Task objects
     * @throws SQLException on database error
     */
    List<Task> getAllTasks() throws SQLException;

    /**
     * Retrieves a single task by its unique ID.
     * @param taskId the task ID
     * @return the Task object if found, or null
     * @throws SQLException on database error
     */
    Task getTaskById(int taskId) throws SQLException;

    /**
     * Updates an existing task's title, description, priority, status, category, and due date.
     * @param task the Task with updated fields
     * @return true if updated successfully, false otherwise
     * @throws SQLException on database error
     */
    boolean updateTask(Task task) throws SQLException;

    /**
     * Updates an existing task and atomically evaluates point rewards if completing a non-completed task.
     * @param task the Task with updated fields
     * @return PointResult containing success flag, points awarded (if any), and total points
     * @throws SQLException on database error
     */
    PointResult updateTaskWithPoints(Task task) throws SQLException;

    /**
     * Deletes a task from the database by its ID without awarding points.
     * @param taskId the ID of the task to delete
     * @return true if deleted, false if task was not found
     * @throws SQLException on database error
     */
    boolean deleteTask(int taskId) throws SQLException;

    /**
     * Updates the status of a specific task.
     * @param taskId the task ID
     * @param status the new status ('Pending', 'In Progress', 'Completed')
     * @return true if updated, false otherwise
     * @throws SQLException on database error
     */
    boolean updateTaskStatus(int taskId, String status) throws SQLException;

    /**
     * Updates the status of a specific task and atomically evaluates points.
     * Awards points (Low: 10, Medium: 20, High: 30) only if moving from non-completed to Completed,
     * and only if points were never previously awarded for this task.
     * @param taskId the task ID
     * @param status the new status ('Pending', 'In Progress', 'Completed')
     * @return PointResult with success status, points awarded, and total points
     * @throws SQLException on database error
     */
    PointResult updateTaskStatusWithPoints(int taskId, String status) throws SQLException;

    /**
     * Computes real-time task statistics and total earned points from database records.
     * @return TaskStats object with total, pending, in-progress, completed counts, and total points
     * @throws SQLException on database error
     */
    TaskStats getTaskStats() throws SQLException;

    /**
     * Retrieves the current total points earned by the user from the task_points table.
     * @return total earned points
     * @throws SQLException on database error
     */
    int getTotalPoints() throws SQLException;
}
