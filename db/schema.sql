-- =====================================================================
-- TaskVerse — Database Schema
-- Database Name: taskverse_db
-- Compatibility: MySQL Server 8.0+ / MySQL Workbench 8.0
-- =====================================================================

-- Step 1: Create Database if it does not exist
CREATE DATABASE IF NOT EXISTS taskverse_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE taskverse_db;

-- Step 2: Create Categories Table
CREATE TABLE IF NOT EXISTS categories (
    category_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    color VARCHAR(20) DEFAULT '#6366f1',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Step 3: Create Tasks Table
CREATE TABLE IF NOT EXISTS tasks (
    task_id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    priority ENUM('Low', 'Medium', 'High') NOT NULL DEFAULT 'Medium',
    status ENUM('Pending', 'In Progress', 'Completed') NOT NULL DEFAULT 'Pending',
    category VARCHAR(50) NOT NULL DEFAULT 'General',
    due_date DATE NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_task_status (status),
    INDEX idx_task_priority (priority),
    INDEX idx_task_due_date (due_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Step 4: Seed Default Categories
INSERT INTO categories (name, color) VALUES
('College', '#818cf8'),
('Personal', '#38bdf8'),
('Work', '#34d399'),
('Projects', '#f472b6'),
('Other', '#fbbf24')
ON DUPLICATE KEY UPDATE name=name;

-- Step 5: Seed Initial Sample Tasks (for testing & immediate visualization)
INSERT INTO tasks (title, description, priority, status, category, due_date) VALUES
('Set up TaskVerse project', 'Inspect Java, Tomcat, and MySQL environment, configure database connection and build scripts.', 'High', 'Completed', 'Projects', CURRENT_DATE()),
('Explore Midnight Theme Dashboard', 'Review dark mode UI cards, glowing accents, status indicators, and modal popups.', 'Medium', 'In Progress', 'Personal', DATE_ADD(CURRENT_DATE(), INTERVAL 2 DAY)),
('Prepare College Project Presentation', 'Document OOP architecture, DAO design pattern, JDBC connection pooling, and MySQL schema.', 'High', 'Pending', 'College', DATE_ADD(CURRENT_DATE(), INTERVAL 5 DAY)),
('Submit Weekly Assignment', 'Complete and submit the database management system assignment online.', 'Low', 'Pending', 'College', DATE_ADD(CURRENT_DATE(), INTERVAL 7 DAY));

-- Step 6: Verify Database Contents
SELECT * FROM categories;
SELECT * FROM tasks;
