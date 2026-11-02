-- =====================================================================
-- TaskVerse — Security Baseline Database Migration
-- Target: MySQL 8.0+
-- Description: Adds user tenancy, authentication tables, per-user ownership,
--              and composite foreign key constraints.
-- Safe: Validates legacy data integrity before applying changes.
-- =====================================================================

-- Set target database: uses active connection schema (DATABASE()), or fallback if running standalone in GUI.
-- If running standalone in MySQL Workbench without an active schema selected, uncomment:
-- USE taskverse_db;

-- Step 0: Precondition Safety Check
-- Detect inconsistent legacy records (e.g. orphaned task_points)
DROP PROCEDURE IF EXISTS check_migration_preconditions;
DELIMITER //
CREATE PROCEDURE check_migration_preconditions()
BEGIN
    DECLARE v_orphan_points INT DEFAULT 0;

    IF (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'task_points') > 0 THEN
        SELECT COUNT(*) INTO v_orphan_points
        FROM task_points tp
        LEFT JOIN tasks t ON tp.task_id = t.task_id
        WHERE t.task_id IS NULL;

        IF v_orphan_points > 0 THEN
            SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Migration aborted: Inconsistent legacy records detected. Orphaned task_points found with non-existent task_id.';
        END IF;
    END IF;
END //
DELIMITER ;

CALL check_migration_preconditions();
DROP PROCEDURE check_migration_preconditions;

-- Step 1: Create Users Table
CREATE TABLE IF NOT EXISTS users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(191) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    password_salt VARCHAR(64) NOT NULL,
    role ENUM('OWNER', 'ADMIN', 'MEMBER') NOT NULL DEFAULT 'MEMBER',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_users_email (email),
    INDEX idx_users_username (username),
    INDEX idx_users_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Enforce exactly one OWNER account at the database level using a functional index
SET @add_single_owner_idx = (
    SELECT IF(COUNT(*) = 0, 'CREATE UNIQUE INDEX uq_single_owner ON users ((CASE WHEN role = "OWNER" THEN 1 ELSE NULL END))', 'SELECT 1')
    FROM information_schema.STATISTICS 
    WHERE TABLE_SCHEMA = DATABASE() 
      AND TABLE_NAME = 'users' 
      AND INDEX_NAME = 'uq_single_owner'
);
PREPARE stmt_add_single_owner FROM @add_single_owner_idx;
EXECUTE stmt_add_single_owner;
DEALLOCATE PREPARE stmt_add_single_owner;

-- Step 2: Update Categories Table for Multi-Tenancy
-- Existing table has global UNIQUE constraint on 'name'.
-- Drop global unique key and add user_id column + per-user unique index.

SET @drop_cat_name_idx = (
    SELECT IF(COUNT(*) > 0, 'ALTER TABLE categories DROP INDEX name', 'SELECT 1')
    FROM information_schema.STATISTICS 
    WHERE TABLE_SCHEMA = DATABASE() 
      AND TABLE_NAME = 'categories' 
      AND INDEX_NAME = 'name'
);
PREPARE stmt_drop_cat_idx FROM @drop_cat_name_idx;
EXECUTE stmt_drop_cat_idx;
DEALLOCATE PREPARE stmt_drop_cat_idx;

-- Add user_id to categories if not exists
SET @add_cat_user = (
    SELECT IF(COUNT(*) = 0, 'ALTER TABLE categories ADD COLUMN user_id INT NULL AFTER category_id', 'SELECT 1')
    FROM information_schema.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() 
      AND TABLE_NAME = 'categories' 
      AND COLUMN_NAME = 'user_id'
);
PREPARE stmt_add_cat_user FROM @add_cat_user;
EXECUTE stmt_add_cat_user;
DEALLOCATE PREPARE stmt_add_cat_user;

-- Add is_system to categories if not exists
SET @add_cat_system = (
    SELECT IF(COUNT(*) = 0, 'ALTER TABLE categories ADD COLUMN is_system BOOLEAN NOT NULL DEFAULT FALSE AFTER color', 'SELECT 1')
    FROM information_schema.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() 
      AND TABLE_NAME = 'categories' 
      AND COLUMN_NAME = 'is_system'
);
PREPARE stmt_add_cat_system FROM @add_cat_system;
EXECUTE stmt_add_cat_system;
DEALLOCATE PREPARE stmt_add_cat_system;

-- Mark pre-existing default categories as system categories
UPDATE categories 
SET is_system = TRUE 
WHERE name IN ('College', 'Personal', 'Work', 'Projects', 'Other') AND user_id IS NULL;

-- Add per-user unique constraint for categories
SET @add_cat_uq = (
    SELECT IF(COUNT(*) = 0, 'ALTER TABLE categories ADD CONSTRAINT uq_categories_user_name UNIQUE KEY ((COALESCE(user_id, 0)), name)', 'SELECT 1')
    FROM information_schema.TABLE_CONSTRAINTS 
    WHERE TABLE_SCHEMA = DATABASE() 
      AND TABLE_NAME = 'categories' 
      AND CONSTRAINT_NAME = 'uq_categories_user_name'
);
PREPARE stmt_add_cat_uq FROM @add_cat_uq;
EXECUTE stmt_add_cat_uq;
DEALLOCATE PREPARE stmt_add_cat_uq;

-- Step 3: Update Tasks Table for Multi-Tenancy
SET @add_task_user = (
    SELECT IF(COUNT(*) = 0, 'ALTER TABLE tasks ADD COLUMN user_id INT NULL AFTER task_id', 'SELECT 1')
    FROM information_schema.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() 
      AND TABLE_NAME = 'tasks' 
      AND COLUMN_NAME = 'user_id'
);
PREPARE stmt_add_task_user FROM @add_task_user;
EXECUTE stmt_add_task_user;
DEALLOCATE PREPARE stmt_add_task_user;

SET @add_task_cat_id = (
    SELECT IF(COUNT(*) = 0, 'ALTER TABLE tasks ADD COLUMN category_id INT NULL AFTER category', 'SELECT 1')
    FROM information_schema.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() 
      AND TABLE_NAME = 'tasks' 
      AND COLUMN_NAME = 'category_id'
);
PREPARE stmt_add_task_cat_id FROM @add_task_cat_id;
EXECUTE stmt_add_task_cat_id;
DEALLOCATE PREPARE stmt_add_task_cat_id;

-- Backfill tasks.category_id based on category string
UPDATE tasks t
JOIN categories c ON t.category = c.name
SET t.category_id = c.category_id
WHERE t.category_id IS NULL;

-- Add composite unique key on tasks(task_id, user_id) to support composite FK from task_points
SET @add_tasks_uq = (
    SELECT IF(COUNT(*) = 0, 'ALTER TABLE tasks ADD CONSTRAINT uq_tasks_task_user UNIQUE KEY (task_id, user_id)', 'SELECT 1')
    FROM information_schema.TABLE_CONSTRAINTS 
    WHERE TABLE_SCHEMA = DATABASE() 
      AND TABLE_NAME = 'tasks' 
      AND CONSTRAINT_NAME = 'uq_tasks_task_user'
);
PREPARE stmt_add_tasks_uq FROM @add_tasks_uq;
EXECUTE stmt_add_tasks_uq;
DEALLOCATE PREPARE stmt_add_tasks_uq;

-- Add index on tasks(user_id)
SET @add_task_user_idx = (
    SELECT IF(COUNT(*) = 0, 'CREATE INDEX idx_tasks_user_id ON tasks (user_id)', 'SELECT 1')
    FROM information_schema.STATISTICS 
    WHERE TABLE_SCHEMA = DATABASE() 
      AND TABLE_NAME = 'tasks' 
      AND INDEX_NAME = 'idx_tasks_user_id'
);
PREPARE stmt_add_task_user_idx FROM @add_task_user_idx;
EXECUTE stmt_add_task_user_idx;
DEALLOCATE PREPARE stmt_add_task_user_idx;

-- Step 4: Ensure Notes Table Exists and Update for Multi-Tenancy
CREATE TABLE IF NOT EXISTS notes (
    note_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NULL,
    title VARCHAR(255) NOT NULL DEFAULT 'Untitled Note',
    content TEXT,
    color VARCHAR(20) NOT NULL DEFAULT 'yellow',
    is_pinned BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_notes_pinned (is_pinned),
    INDEX idx_notes_updated (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET @add_notes_user = (
    SELECT IF(COUNT(*) = 0, 'ALTER TABLE notes ADD COLUMN user_id INT NULL AFTER note_id', 'SELECT 1')
    FROM information_schema.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() 
      AND TABLE_NAME = 'notes' 
      AND COLUMN_NAME = 'user_id'
);
PREPARE stmt_add_notes_user FROM @add_notes_user;
EXECUTE stmt_add_notes_user;
DEALLOCATE PREPARE stmt_add_notes_user;

SET @add_notes_user_idx = (
    SELECT IF(COUNT(*) = 0, 'CREATE INDEX idx_notes_user_id ON notes (user_id)', 'SELECT 1')
    FROM information_schema.STATISTICS 
    WHERE TABLE_SCHEMA = DATABASE() 
      AND TABLE_NAME = 'notes' 
      AND INDEX_NAME = 'idx_notes_user_id'
);
PREPARE stmt_add_notes_user_idx FROM @add_notes_user_idx;
EXECUTE stmt_add_notes_user_idx;
DEALLOCATE PREPARE stmt_add_notes_user_idx;

-- Step 5: Ensure Task Points Table Exists and Update for Multi-Tenancy
CREATE TABLE IF NOT EXISTS task_points (
    point_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NULL,
    task_id INT NOT NULL,
    task_title VARCHAR(255) NOT NULL,
    priority ENUM('Low', 'Medium', 'High') NOT NULL,
    points INT NOT NULL,
    earned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_task_points_task_id (task_id),
    INDEX idx_task_points_earned_at (earned_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET @add_points_user = (
    SELECT IF(COUNT(*) = 0, 'ALTER TABLE task_points ADD COLUMN user_id INT NULL AFTER point_id', 'SELECT 1')
    FROM information_schema.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() 
      AND TABLE_NAME = 'task_points' 
      AND COLUMN_NAME = 'user_id'
);
PREPARE stmt_add_points_user FROM @add_points_user;
EXECUTE stmt_add_points_user;
DEALLOCATE PREPARE stmt_add_points_user;

-- Sync task_points.user_id with tasks.user_id
UPDATE task_points tp
JOIN tasks t ON tp.task_id = t.task_id
SET tp.user_id = t.user_id
WHERE tp.user_id IS NULL AND t.user_id IS NOT NULL;

SET @add_points_user_idx = (
    SELECT IF(COUNT(*) = 0, 'CREATE INDEX idx_task_points_user_id ON task_points (user_id)', 'SELECT 1')
    FROM information_schema.STATISTICS 
    WHERE TABLE_SCHEMA = DATABASE() 
      AND TABLE_NAME = 'task_points' 
      AND INDEX_NAME = 'idx_task_points_user_id'
);
PREPARE stmt_add_points_user_idx FROM @add_points_user_idx;
EXECUTE stmt_add_points_user_idx;
DEALLOCATE PREPARE stmt_add_points_user_idx;

-- Step 6: Foreign Key Constraints (Added safely after columns exist)
-- Categories FK
SET @add_cat_fk = (
    SELECT IF(COUNT(*) = 0, 'ALTER TABLE categories ADD CONSTRAINT fk_categories_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE', 'SELECT 1')
    FROM information_schema.TABLE_CONSTRAINTS 
    WHERE TABLE_SCHEMA = DATABASE() 
      AND TABLE_NAME = 'categories' 
      AND CONSTRAINT_NAME = 'fk_categories_user'
);
PREPARE stmt_add_cat_fk FROM @add_cat_fk;
EXECUTE stmt_add_cat_fk;
DEALLOCATE PREPARE stmt_add_cat_fk;

-- Tasks FKs
SET @add_task_fk_user = (
    SELECT IF(COUNT(*) = 0, 'ALTER TABLE tasks ADD CONSTRAINT fk_tasks_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE', 'SELECT 1')
    FROM information_schema.TABLE_CONSTRAINTS 
    WHERE TABLE_SCHEMA = DATABASE() 
      AND TABLE_NAME = 'tasks' 
      AND CONSTRAINT_NAME = 'fk_tasks_user'
);
PREPARE stmt_add_task_fk_user FROM @add_task_fk_user;
EXECUTE stmt_add_task_fk_user;
DEALLOCATE PREPARE stmt_add_task_fk_user;

SET @add_task_fk_cat = (
    SELECT IF(COUNT(*) = 0, 'ALTER TABLE tasks ADD CONSTRAINT fk_tasks_category FOREIGN KEY (category_id) REFERENCES categories(category_id) ON DELETE SET NULL', 'SELECT 1')
    FROM information_schema.TABLE_CONSTRAINTS 
    WHERE TABLE_SCHEMA = DATABASE() 
      AND TABLE_NAME = 'tasks' 
      AND CONSTRAINT_NAME = 'fk_tasks_category'
);
PREPARE stmt_add_task_fk_cat FROM @add_task_fk_cat;
EXECUTE stmt_add_task_fk_cat;
DEALLOCATE PREPARE stmt_add_task_fk_cat;

-- Notes FK
SET @add_notes_fk = (
    SELECT IF(COUNT(*) = 0, 'ALTER TABLE notes ADD CONSTRAINT fk_notes_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE', 'SELECT 1')
    FROM information_schema.TABLE_CONSTRAINTS 
    WHERE TABLE_SCHEMA = DATABASE() 
      AND TABLE_NAME = 'notes' 
      AND CONSTRAINT_NAME = 'fk_notes_user'
);
PREPARE stmt_add_notes_fk FROM @add_notes_fk;
EXECUTE stmt_add_notes_fk;
DEALLOCATE PREPARE stmt_add_notes_fk;

-- Task Points Composite FK: Enforces that task points belong to the same task AND the same user!
SET @add_points_fk_composite = (
    SELECT IF(COUNT(*) = 0, 'ALTER TABLE task_points ADD CONSTRAINT fk_task_points_task_user FOREIGN KEY (task_id, user_id) REFERENCES tasks(task_id, user_id) ON DELETE CASCADE ON UPDATE CASCADE', 'SELECT 1')
    FROM information_schema.TABLE_CONSTRAINTS 
    WHERE TABLE_SCHEMA = DATABASE() 
      AND TABLE_NAME = 'task_points' 
      AND CONSTRAINT_NAME = 'fk_task_points_task_user'
);
PREPARE stmt_add_points_fk_composite FROM @add_points_fk_composite;
EXECUTE stmt_add_points_fk_composite;
DEALLOCATE PREPARE stmt_add_points_fk_composite;

-- Verification query
SELECT 'Security migration applied successfully.' AS status;
