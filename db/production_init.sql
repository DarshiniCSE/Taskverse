-- =====================================================================
-- TaskVerse — Production Database Initialization Script
-- Target: MySQL 8.4 LTS / MySQL 8.0+ (Cloud: Aiven, AWS RDS, GCP Cloud SQL, PlanetScale)
-- Description: Creates the complete, canonical multi-tenant schema in one execution.
--              Includes all tables, indexes, functional keys, and foreign keys.
-- Safe: Idempotent (IF NOT EXISTS), zero sample/test tasks, zero credentials.
-- =====================================================================

-- Note: In cloud consoles (such as Aiven), the target database (e.g. defaultdb)
-- is selected automatically by the session. If running via CLI, ensure your
-- connection targets the intended database.

-- ---------------------------------------------------------------------
-- 1. Table: users
-- ---------------------------------------------------------------------
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
    INDEX idx_users_role (role),
    CONSTRAINT uq_single_owner UNIQUE KEY ((CASE WHEN role = 'OWNER' THEN 1 ELSE NULL END))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- 2. Table: categories
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS categories (
    category_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NULL,
    name VARCHAR(50) NOT NULL,
    color VARCHAR(20) DEFAULT '#6366f1',
    is_system BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_categories_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_categories_user_id (user_id),
    CONSTRAINT uq_categories_user_name UNIQUE KEY ((COALESCE(user_id, 0)), name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Seed default system categories (available globally to all users)
INSERT INTO categories (name, color, is_system) VALUES
('College', '#818cf8', TRUE),
('Personal', '#38bdf8', TRUE),
('Work', '#34d399', TRUE),
('Projects', '#f472b6', TRUE),
('Other', '#fbbf24', TRUE)
ON DUPLICATE KEY UPDATE is_system = TRUE;

-- ---------------------------------------------------------------------
-- 3. Table: tasks
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS tasks (
    task_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    priority ENUM('Low', 'Medium', 'High') NOT NULL DEFAULT 'Medium',
    status ENUM('Pending', 'In Progress', 'Completed') NOT NULL DEFAULT 'Pending',
    category VARCHAR(50) NOT NULL DEFAULT 'General',
    category_id INT NULL,
    due_date DATE NULL,
    completed_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_tasks_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_tasks_category FOREIGN KEY (category_id) REFERENCES categories(category_id) ON DELETE SET NULL,
    CONSTRAINT uq_tasks_task_user UNIQUE KEY (task_id, user_id),
    INDEX idx_tasks_user_id (user_id),
    INDEX idx_tasks_status (status),
    INDEX idx_tasks_priority (priority),
    INDEX idx_tasks_due_date (due_date),
    INDEX idx_tasks_category_id (category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- 4. Table: notes
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS notes (
    note_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NULL,
    title VARCHAR(255) NOT NULL DEFAULT 'Untitled Note',
    content TEXT,
    color VARCHAR(20) NOT NULL DEFAULT 'yellow',
    is_pinned BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_notes_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_notes_user_id (user_id),
    INDEX idx_notes_pinned (is_pinned),
    INDEX idx_notes_updated (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- 5. Table: task_points
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS task_points (
    point_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NULL,
    task_id INT NOT NULL,
    task_title VARCHAR(255) NOT NULL,
    priority ENUM('Low', 'Medium', 'High') NOT NULL,
    points INT NOT NULL,
    earned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_task_points_task UNIQUE KEY (task_id),
    CONSTRAINT fk_task_points_task_user FOREIGN KEY (task_id, user_id) 
        REFERENCES tasks(task_id, user_id) 
        ON DELETE CASCADE 
        ON UPDATE CASCADE,
    INDEX idx_task_points_user_id (user_id),
    INDEX idx_task_points_earned_at (earned_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
