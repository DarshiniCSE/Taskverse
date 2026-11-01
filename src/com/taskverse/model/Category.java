package com.taskverse.model;

/**
 * Category Model Class
 * Represents a task category entity.
 * Demonstrates OOP Encapsulation.
 */
public class Category {
    private int categoryId;
    private String name;
    private String color;

    public Category() {
    }

    public Category(int categoryId, String name, String color) {
        this.categoryId = categoryId;
        this.name = name;
        this.color = color;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String toJSON() {
        return "{"
            + "\"categoryId\":" + categoryId + ","
            + "\"name\":\"" + (name != null ? name.replace("\"", "\\\"") : "") + "\","
            + "\"color\":\"" + (color != null ? color : "#6366f1") + "\""
            + "}";
    }
}
