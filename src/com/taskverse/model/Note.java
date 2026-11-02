package com.taskverse.model;

/**
 * Note Model Class
 * Represents a single Sticky Note entity in the TaskVerse system.
 * 
 * Demonstrates OOP Concept: Encapsulation
 * - Private fields with public getters, setters, and JSON serialization.
 */
public class Note {
    private int noteId;
    private int userId;
    private String title;
    private String content;
    private String color; // yellow, pink, blue, green, purple
    private boolean isPinned;
    private String createdAt;
    private String updatedAt;

    public Note() {
        this.title = "New Note";
        this.content = "";
        this.color = "yellow";
        this.isPinned = false;
    }

    public Note(String title, String content, String color, boolean isPinned) {
        this.title = (title != null && !title.trim().isEmpty()) ? title.trim() : "New Note";
        this.content = content != null ? content : "";
        this.color = (color != null && !color.trim().isEmpty()) ? color.trim().toLowerCase() : "yellow";
        this.isPinned = isPinned;
    }

    public Note(int noteId, String title, String content, String color, boolean isPinned, String createdAt, String updatedAt) {
        this.noteId = noteId;
        this.title = (title != null && !title.trim().isEmpty()) ? title.trim() : "New Note";
        this.content = content != null ? content : "";
        this.color = (color != null && !color.trim().isEmpty()) ? color.trim().toLowerCase() : "yellow";
        this.isPinned = isPinned;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public int getNoteId() {
        return noteId;
    }

    public void setNoteId(int noteId) {
        this.noteId = noteId;
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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public boolean isPinned() {
        return isPinned;
    }

    public void setPinned(boolean pinned) {
        isPinned = pinned;
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

    public String toJSON() {
        return "{"
            + "\"noteId\":" + noteId + ","
            + "\"userId\":" + userId + ","
            + "\"title\":\"" + escapeJson(title) + "\","
            + "\"content\":\"" + escapeJson(content) + "\","
            + "\"color\":\"" + escapeJson(color) + "\","
            + "\"isPinned\":" + isPinned + ","
            + "\"createdAt\":" + (createdAt != null ? "\"" + escapeJson(createdAt) + "\"" : "null") + ","
            + "\"updatedAt\":" + (updatedAt != null ? "\"" + escapeJson(updatedAt) + "\"" : "null")
            + "}";
    }

    private String escapeJson(String input) {
        if (input == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char ch = input.charAt(i);
            switch (ch) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (ch < ' ') {
                        String t = "000" + Integer.toHexString(ch);
                        sb.append("\\u").append(t.substring(t.length() - 4));
                    } else {
                        sb.append(ch);
                    }
                    break;
            }
        }
        return sb.toString();
    }
}
