package com.taskverse.model;

import java.sql.Timestamp;

/**
 * User Model Class
 * Represents an authenticated user in TaskVerse.
 * Demonstrates OOP Encapsulation.
 */
public class User {

    private int userId;
    private String username;
    private String email;
    private String passwordHash;
    private String passwordSalt;
    private String role; // 'OWNER', 'ADMIN', 'MEMBER'
    private boolean active;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public User() {
    }

    public User(int userId, String username, String email, String role, boolean active) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.role = role;
        this.active = active;
    }

    public User(int userId, String username, String email, String passwordHash, String passwordSalt,
                String role, boolean active, Timestamp createdAt, Timestamp updatedAt) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.passwordSalt = passwordSalt;
        this.role = role;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getPasswordSalt() {
        return passwordSalt;
    }

    public void setPasswordSalt(String passwordSalt) {
        this.passwordSalt = passwordSalt;
    }

    public String getRole() {
        return role != null ? role : "MEMBER";
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * Serializes safe public profile fields to JSON.
     * CRITICAL SECURITY: Never serializes passwordHash or passwordSalt.
     */
    public String toJSON() {
        return "{"
                + "\"userId\":" + userId + ","
                + "\"username\":\"" + escapeJson(username) + "\","
                + "\"email\":\"" + escapeJson(email) + "\","
                + "\"role\":\"" + escapeJson(getRole()) + "\","
                + "\"active\":" + active + ","
                + "\"createdAt\":\"" + (createdAt != null ? createdAt.toString() : "") + "\""
                + "}";
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
