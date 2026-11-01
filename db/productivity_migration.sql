-- =====================================================================
-- TaskVerse — Monthly Productivity Tracker Migration Script
-- Database Name: taskverse_db
-- Compatibility: MySQL Server 8.0+ / MySQL Workbench 8.0
-- Safe & Idempotent: Does not alter existing task records or categories.
-- =====================================================================

USE taskverse_db;

-- Step 1: Safely add completed_at timestamp column to tasks if not already present
SET @col_exists = (
    SELECT COUNT(*) 
    FROM information_schema.COLUMNS 
    WHERE TABLE_SCHEMA = 'taskverse_db' 
      AND TABLE_NAME = 'tasks' 
      AND COLUMN_NAME = 'completed_at'
);

SET @sql_col = IF(
    @col_exists = 0, 
    'ALTER TABLE tasks ADD COLUMN completed_at TIMESTAMP NULL DEFAULT NULL AFTER due_date', 
    'SELECT "Column completed_at already exists" AS status'
);

PREPARE stmt_col FROM @sql_col;
EXECUTE stmt_col;
DEALLOCATE PREPARE stmt_col;

-- Step 2: Create index on completed_at for fast monthly aggregation and range queries
SET @idx_exists = (
    SELECT COUNT(*) 
    FROM information_schema.STATISTICS 
    WHERE TABLE_SCHEMA = 'taskverse_db' 
      AND TABLE_NAME = 'tasks' 
      AND INDEX_NAME = 'idx_task_completed_at'
);

SET @sql_idx = IF(
    @idx_exists = 0, 
    'CREATE INDEX idx_task_completed_at ON tasks (completed_at)', 
    'SELECT "Index idx_task_completed_at already exists" AS status'
);

PREPARE stmt_idx FROM @sql_idx;
EXECUTE stmt_idx;
DEALLOCATE PREPARE stmt_idx;

-- Step 3: Backfill completed_at for existing completed tasks
-- Accurately uses task_points earned_at where available, otherwise falls back to updated_at / created_at.
UPDATE tasks t
LEFT JOIN task_points tp ON t.task_id = tp.task_id
SET t.completed_at = COALESCE(tp.earned_at, t.updated_at, t.created_at)
WHERE t.status = 'Completed' AND t.completed_at IS NULL;

-- Step 4: Verification Query
SELECT 
    task_id, title, status, due_date, completed_at, created_at, updated_at
FROM tasks;
