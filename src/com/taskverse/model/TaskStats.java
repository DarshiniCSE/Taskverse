package com.taskverse.model;

/**
 * TaskStats Model Class
 * Represents aggregated task counts and total points computed from database records.
 * Demonstrates OOP Encapsulation.
 */
public class TaskStats {
    private int totalTasks;
    private int pendingTasks;
    private int inProgressTasks;
    private int completedTasks;
    private int totalPoints;

    public TaskStats() {
    }

    public TaskStats(int totalTasks, int pendingTasks, int inProgressTasks, int completedTasks) {
        this(totalTasks, pendingTasks, inProgressTasks, completedTasks, 0);
    }

    public TaskStats(int totalTasks, int pendingTasks, int inProgressTasks, int completedTasks, int totalPoints) {
        this.totalTasks = totalTasks;
        this.pendingTasks = pendingTasks;
        this.inProgressTasks = inProgressTasks;
        this.completedTasks = completedTasks;
        this.totalPoints = totalPoints;
    }

    public int getTotalTasks() {
        return totalTasks;
    }

    public void setTotalTasks(int totalTasks) {
        this.totalTasks = totalTasks;
    }

    public int getPendingTasks() {
        return pendingTasks;
    }

    public void setPendingTasks(int pendingTasks) {
        this.pendingTasks = pendingTasks;
    }

    public int getInProgressTasks() {
        return inProgressTasks;
    }

    public void setInProgressTasks(int inProgressTasks) {
        this.inProgressTasks = inProgressTasks;
    }

    public int getCompletedTasks() {
        return completedTasks;
    }

    public void setCompletedTasks(int completedTasks) {
        this.completedTasks = completedTasks;
    }

    public int getTotalPoints() {
        return totalPoints;
    }

    public void setTotalPoints(int totalPoints) {
        this.totalPoints = totalPoints;
    }

    public String toJSON() {
        return "{"
            + "\"total\":" + totalTasks + ","
            + "\"pending\":" + pendingTasks + ","
            + "\"inProgress\":" + inProgressTasks + ","
            + "\"completed\":" + completedTasks + ","
            + "\"totalPoints\":" + totalPoints
            + "}";
    }
}
