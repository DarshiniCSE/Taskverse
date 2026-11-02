-- =====================================================================
-- TaskVerse — Security Baseline Database Rollback Script
-- Target: MySQL 8.0+
-- Description: Safely reverts multi-tenancy columns, foreign keys, and indexes.
-- Safety: Halts if multiple users exist to prevent blending private data.
-- =====================================================================

-- Set target database: uses active connection schema (DATABASE()), or fallback if running standalone in GUI.
-- If running standalone in MySQL Workbench without an active schema selected, uncomment:
-- USE taskverse_db;

-- Step 0: Rollback Safety Precondition Check
DROP PROCEDURE IF EXISTS check_rollback_safety;
DELIMITER //
CREATE PROCEDURE check_rollback_safety()
BEGIN
    DECLARE v_user_count INT DEFAULT 0;

    IF (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users') > 0 THEN
        SELECT COUNT(*) INTO v_user_count FROM users;
        IF v_user_count > 1 THEN
            SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Rollback aborted: Multiple users exist in database. Reverting multi-tenancy would merge private data.';
        END IF;
    END IF;
END //
DELIMITER ;

CALL check_rollback_safety();
DROP PROCEDURE check_rollback_safety;

-- 1. Drop foreign keys
SET @drop_fk1 = (SELECT IF(COUNT(*) > 0, 'ALTER TABLE task_points DROP FOREIGN KEY fk_task_points_task_user', 'SELECT 1') FROM information_schema.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA = DATABASE() AND CONSTRAINT_NAME = 'fk_task_points_task_user');
PREPARE stmt1 FROM @drop_fk1; EXECUTE stmt1; DEALLOCATE PREPARE stmt1;

SET @drop_fk2 = (SELECT IF(COUNT(*) > 0, 'ALTER TABLE task_points DROP FOREIGN KEY fk_task_points_user', 'SELECT 1') FROM information_schema.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA = DATABASE() AND CONSTRAINT_NAME = 'fk_task_points_user');
PREPARE stmt2 FROM @drop_fk2; EXECUTE stmt2; DEALLOCATE PREPARE stmt2;

SET @drop_fk3 = (SELECT IF(COUNT(*) > 0, 'ALTER TABLE notes DROP FOREIGN KEY fk_notes_user', 'SELECT 1') FROM information_schema.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA = DATABASE() AND CONSTRAINT_NAME = 'fk_notes_user');
PREPARE stmt3 FROM @drop_fk3; EXECUTE stmt3; DEALLOCATE PREPARE stmt3;

SET @drop_fk4 = (SELECT IF(COUNT(*) > 0, 'ALTER TABLE tasks DROP FOREIGN KEY fk_tasks_category', 'SELECT 1') FROM information_schema.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA = DATABASE() AND CONSTRAINT_NAME = 'fk_tasks_category');
PREPARE stmt4 FROM @drop_fk4; EXECUTE stmt4; DEALLOCATE PREPARE stmt4;

SET @drop_fk5 = (SELECT IF(COUNT(*) > 0, 'ALTER TABLE tasks DROP FOREIGN KEY fk_tasks_user', 'SELECT 1') FROM information_schema.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA = DATABASE() AND CONSTRAINT_NAME = 'fk_tasks_user');
PREPARE stmt5 FROM @drop_fk5; EXECUTE stmt5; DEALLOCATE PREPARE stmt5;

SET @drop_fk6 = (SELECT IF(COUNT(*) > 0, 'ALTER TABLE categories DROP FOREIGN KEY fk_categories_user', 'SELECT 1') FROM information_schema.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA = DATABASE() AND CONSTRAINT_NAME = 'fk_categories_user');
PREPARE stmt6 FROM @drop_fk6; EXECUTE stmt6; DEALLOCATE PREPARE stmt6;

-- 2. Drop constraints & indexes
SET @drop_idx_composite = (SELECT IF(COUNT(*) > 0, 'ALTER TABLE tasks DROP INDEX uq_tasks_task_user', 'SELECT 1') FROM information_schema.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA = DATABASE() AND CONSTRAINT_NAME = 'uq_tasks_task_user');
PREPARE stmt_comp FROM @drop_idx_composite; EXECUTE stmt_comp; DEALLOCATE PREPARE stmt_comp;

SET @drop_idx1 = (SELECT IF(COUNT(*) > 0, 'ALTER TABLE categories DROP INDEX uq_categories_user_name', 'SELECT 1') FROM information_schema.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA = DATABASE() AND CONSTRAINT_NAME = 'uq_categories_user_name');
PREPARE stmt7 FROM @drop_idx1; EXECUTE stmt7; DEALLOCATE PREPARE stmt7;

-- Clean up user category name collisions with default categories before restoring global uniqueness
UPDATE tasks t 
JOIN categories c_user ON t.category_id = c_user.category_id 
JOIN categories c_sys ON c_user.name = c_sys.name 
SET t.category_id = c_sys.category_id 
WHERE c_user.user_id IS NOT NULL AND c_sys.is_system = TRUE;

DELETE c_user FROM categories c_user 
JOIN categories c_sys ON c_user.name = c_sys.name 
WHERE c_user.user_id IS NOT NULL AND c_sys.is_system = TRUE;

-- Restore unique on categories.name
SET @restore_cat_idx = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE categories ADD CONSTRAINT uq_categories_name UNIQUE (name)', 'SELECT 1') FROM information_schema.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA = DATABASE() AND CONSTRAINT_NAME = 'uq_categories_name');
PREPARE stmt8 FROM @restore_cat_idx; EXECUTE stmt8; DEALLOCATE PREPARE stmt8;

SET @drop_owner_idx = (SELECT IF(COUNT(*) > 0, 'ALTER TABLE users DROP INDEX uq_single_owner', 'SELECT 1') FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND INDEX_NAME = 'uq_single_owner');
PREPARE stmt_owner FROM @drop_owner_idx; EXECUTE stmt_owner; DEALLOCATE PREPARE stmt_owner;

-- 3. Drop columns
SET @drop_col1 = (SELECT IF(COUNT(*) > 0, 'ALTER TABLE task_points DROP COLUMN user_id', 'SELECT 1') FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'task_points' AND COLUMN_NAME = 'user_id');
PREPARE stmt9 FROM @drop_col1; EXECUTE stmt9; DEALLOCATE PREPARE stmt9;

SET @drop_col2 = (SELECT IF(COUNT(*) > 0, 'ALTER TABLE notes DROP COLUMN user_id', 'SELECT 1') FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'notes' AND COLUMN_NAME = 'user_id');
PREPARE stmt10 FROM @drop_col2; EXECUTE stmt10; DEALLOCATE PREPARE stmt10;

SET @drop_col3 = (SELECT IF(COUNT(*) > 0, 'ALTER TABLE tasks DROP COLUMN category_id', 'SELECT 1') FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tasks' AND COLUMN_NAME = 'category_id');
PREPARE stmt11 FROM @drop_col3; EXECUTE stmt11; DEALLOCATE PREPARE stmt11;

SET @drop_col4 = (SELECT IF(COUNT(*) > 0, 'ALTER TABLE tasks DROP COLUMN user_id', 'SELECT 1') FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tasks' AND COLUMN_NAME = 'user_id');
PREPARE stmt12 FROM @drop_col4; EXECUTE stmt12; DEALLOCATE PREPARE stmt12;

SET @drop_col5 = (SELECT IF(COUNT(*) > 0, 'ALTER TABLE categories DROP COLUMN is_system', 'SELECT 1') FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'categories' AND COLUMN_NAME = 'is_system');
PREPARE stmt13 FROM @drop_col5; EXECUTE stmt13; DEALLOCATE PREPARE stmt13;

SET @drop_col6 = (SELECT IF(COUNT(*) > 0, 'ALTER TABLE categories DROP COLUMN user_id', 'SELECT 1') FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'categories' AND COLUMN_NAME = 'user_id');
PREPARE stmt14 FROM @drop_col6; EXECUTE stmt14; DEALLOCATE PREPARE stmt14;

SELECT 'Security rollback completed successfully.' AS status;
