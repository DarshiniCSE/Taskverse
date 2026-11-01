-- =====================================================================
-- TaskVerse — Task Points System Migration Script
-- Database Name: taskverse_db
-- Compatibility: MySQL Server 8.0+ / MySQL Workbench 8.0
-- Safe & Idempotent: Does not alter existing task records or categories.
-- =====================================================================

USE taskverse_db;

-- Step 1: Create task_points table to persist earned points and enforce uniqueness
CREATE TABLE IF NOT EXISTS task_points (
    point_id INT AUTO_INCREMENT PRIMARY KEY,
    task_id INT NOT NULL UNIQUE,
    task_title VARCHAR(255) NOT NULL,
    priority ENUM('Low', 'Medium', 'High') NOT NULL,
    points INT NOT NULL,
    earned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_task_points_task_id (task_id),
    INDEX idx_task_points_earned_at (earned_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Step 2: Seed existing completed tasks with 0 points
-- This guarantees that existing completed tasks cannot be reopened and completed again for duplicate points
INSERT IGNORE INTO task_points (task_id, task_title, priority, points)
SELECT task_id, title, priority, 0 
FROM tasks 
WHERE status = 'Completed';

-- Step 3: Verification Query
SELECT 
    COUNT(*) AS records_count,
    COALESCE(SUM(points), 0) AS total_points_earned 
FROM task_points;
