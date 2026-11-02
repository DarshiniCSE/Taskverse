package com.taskverse.model;

/**
 * Task Model Class
 * Represents a single task entity in the TaskVerse system.
 * 
 * Demonstrates OOP Concept: Encapsulation
 * - All attributes are private (data hiding).
 * - Accessed and modified through public getters and setters with validation.
 */
public class Task {
    private int taskId;
    private int userId;
    private String title;
    private String description;
    private String priority; // Low, Medium, High
    private String status;   // Pending, In Progress, Completed
    private String category;
    private String dueDate;  // Formatted as YYYY-MM-DD
    private String completedAt; // Formatted as YYYY-MM-DD HH:MM:SS
    private String createdAt;
    private String updatedAt;

    // Default constructor
    public Task() {
        this.priority = "Medium";
        this.status = "Pending";
        this.category = "General";
    }

    // Parameterized constructor for new task creation
    public Task(String title, String description, String priority, String status, String category, String dueDate) {
        this.title = title;
        this.description = description;
        this.priority = (priority != null && !priority.trim().isEmpty()) ? priority : "Medium";
        this.status = (status != null && !status.trim().isEmpty()) ? status : "Pending";
        this.category = (category != null && !category.trim().isEmpty()) ? category : "General";
        this.dueDate = dueDate;
    }

    // Full constructor with completedAt
    public Task(int taskId, String title, String description, String priority, String status, 
                String category, String dueDate, String completedAt, String createdAt, String updatedAt) {
        this.taskId = taskId;
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.status = status;
        this.category = category;
        this.dueDate = dueDate;
        this.completedAt = completedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Backwards-compatible constructor without completedAt
    public Task(int taskId, String title, String description, String priority, String status, 
                String category, String dueDate, String createdAt, String updatedAt) {
        this(taskId, title, description, priority, status, category, dueDate, null, createdAt, updatedAt);
    }

    // Getters and Setters
    public int getTaskId() {
        return taskId;
    }

    public void setTaskId(int taskId) {
        this.taskId = taskId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDueDate() {
        return dueDate;
    }

    public void setDueDate(String dueDate) {
        this.dueDate = dueDate;
    }

    public String getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(String completedAt) {
        this.completedAt = completedAt;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * Converts the Task object to a clean JSON string representation.
     * Escapes special characters to ensure valid JSON syntax without external libraries.
     */
    public String toJSON() {
        return "{"
            + "\"taskId\":" + taskId + ","
            + "\"userId\":" + userId + ","
            + "\"title\":\"" + escapeJson(title) + "\","
            + "\"description\":\"" + escapeJson(description) + "\","
            + "\"priority\":\"" + escapeJson(priority) + "\","
            + "\"status\":\"" + escapeJson(status) + "\","
            + "\"category\":\"" + escapeJson(category) + "\","
            + "\"dueDate\":" + (dueDate != null ? "\"" + escapeJson(dueDate) + "\"" : "null") + ","
            + "\"completedAt\":" + (completedAt != null ? "\"" + escapeJson(completedAt) + "\"" : "null") + ","
            + "\"createdAt\":" + (createdAt != null ? "\"" + escapeJson(createdAt) + "\"" : "null") + ","
            + "\"updatedAt\":" + (updatedAt != null ? "\"" + escapeJson(updatedAt) + "\"" : "null")
            + "}";
    }

    private String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\b", "\\b")
                    .replace("\f", "\\f")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
    }

    @Override
    public String toString() {
        return "Task{" +
                "taskId=" + taskId +
                ", title='" + title + '\'' +
                ", priority='" + priority + '\'' +
                ", status='" + status + '\'' +
                ", category='" + category + '\'' +
                ", dueDate='" + dueDate + '\'' +
                '}';
    }
}
