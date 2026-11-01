package com.taskverse.model;

/**
 * PointResult Model Class
 * Encapsulates the outcome of a task status update or edit operation,
 * including any points awarded and current total points.
 * 
 * Demonstrates OOP Encapsulation.
 */
public class PointResult {
    private boolean success;
    private int pointsAwarded;
    private int totalPoints;
    private String message;

    public PointResult(boolean success, int pointsAwarded, int totalPoints, String message) {
        this.success = success;
        this.pointsAwarded = pointsAwarded;
        this.totalPoints = totalPoints;
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public int getPointsAwarded() {
        return pointsAwarded;
    }

    public void setPointsAwarded(int pointsAwarded) {
        this.pointsAwarded = pointsAwarded;
    }

    public int getTotalPoints() {
        return totalPoints;
    }

    public void setTotalPoints(int totalPoints) {
        this.totalPoints = totalPoints;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String toJSON() {
        return "{"
            + "\"success\":" + success + ","
            + "\"pointsAwarded\":" + pointsAwarded + ","
            + "\"totalPoints\":" + totalPoints + ","
            + "\"message\":\"" + (message != null ? message.replace("\\", "\\\\").replace("\"", "\\\"") : "") + "\""
            + "}";
    }
}
