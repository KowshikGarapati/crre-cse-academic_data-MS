-- ============================================================================
-- V11__official_student_profile_and_entrance_fields.sql
-- CRRE Academic DMS - Official Student Master & Certificate Schema
-- Scope:
--   1. Make hall_ticket_number nullable in student_educational_history
--   2. Add personal bio and entrance exam columns to students table
--   3. Add is_synthetic flag for isolating prototype data
-- ============================================================================

-- 1. Allow optional hall ticket numbers for incomplete historical submissions
ALTER TABLE student_educational_history
    ALTER COLUMN hall_ticket_number DROP NOT NULL;

-- 2. Add entrance exam and official profile fields to students table
ALTER TABLE students
    ADD COLUMN IF NOT EXISTS date_of_birth DATE NULL,
    ADD COLUMN IF NOT EXISTS gender VARCHAR(20) NULL,
    ADD COLUMN IF NOT EXISTS entrance_exam_name VARCHAR(50) NULL,
    ADD COLUMN IF NOT EXISTS entrance_hall_ticket VARCHAR(50) NULL,
    ADD COLUMN IF NOT EXISTS entrance_rank INT NULL,
    ADD COLUMN IF NOT EXISTS is_synthetic BOOLEAN NOT NULL DEFAULT FALSE;

-- 3. Create index for fast synthetic record filtering and isolation
CREATE INDEX IF NOT EXISTS idx_students_synthetic ON students(is_synthetic);
