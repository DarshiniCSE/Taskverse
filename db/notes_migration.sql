-- =====================================================================
-- TaskVerse — Sticky Notes System Database Migration
-- Database: taskverse_db
-- Compatibility: MySQL Server 8.0+ / MySQL Workbench 8.0
-- Safe & Idempotent: Does not alter existing tasks, categories, or task_points.
-- =====================================================================

USE taskverse_db;

-- Step 1: Create notes table
CREATE TABLE IF NOT EXISTS notes (
    note_id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL DEFAULT 'Untitled Note',
    content TEXT,
    color VARCHAR(20) NOT NULL DEFAULT 'yellow',
    is_pinned BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_notes_pinned (is_pinned),
    INDEX idx_notes_updated (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Step 2: Seed initial sample note if table is empty
INSERT INTO notes (title, content, color, is_pinned)
SELECT 'Welcome to Sticky Notes! 📝', 'Pin your quick thoughts, project brainstorms, and daily reminders here. You can change colors, pin important notes, and edits auto-save seamlessly!', 'yellow', TRUE
WHERE NOT EXISTS (SELECT 1 FROM notes LIMIT 1);

INSERT INTO notes (title, content, color, is_pinned)
SELECT 'College Presentation Tips 💡', '1. Keep slide bullet points concise.\n2. Emphasize DAO architecture & MySQL transaction security.\n3. Demonstrate drag-and-drop & points reward live!', 'blue', FALSE
WHERE (SELECT COUNT(*) FROM notes) < 2;

-- Step 3: Verification Query
SELECT * FROM notes ORDER BY is_pinned DESC, updated_at DESC;
