-- ============================================================================
-- V9__v2_3_role_decoupling_and_hod_integrity.sql
-- CRRE Academic DMS - Institutional Data Architecture V2.3
-- Scope:
--   1. Migrate users with role = 'HOD' to 'FACULTY' base role
--   2. Restrict chk_user_role to 4 base roles: PRINCIPAL, FACULTY, STUDENT, ADMIN
--   3. Enforce department_id IS NOT NULL for HOD_DEPT responsibilities
--   4. Enforce at most one active HOD per department via partial unique index
-- ============================================================================

-- 1. Migrate existing users with role 'HOD' to 'FACULTY'
UPDATE users
SET role = 'FACULTY',
    updated_at = CURRENT_TIMESTAMP
WHERE role = 'HOD';

-- 2. Replace chk_user_role check constraint on users table
ALTER TABLE users DROP CONSTRAINT chk_user_role;

ALTER TABLE users
    ADD CONSTRAINT chk_user_role
        CHECK (role IN ('PRINCIPAL', 'FACULTY', 'STUDENT', 'ADMIN'));

-- 3. Enforce non-null department for HOD responsibilities
ALTER TABLE responsibilities
    ADD CONSTRAINT chk_hod_dept_not_null
        CHECK (type <> 'HOD_DEPT' OR department_id IS NOT NULL);

-- 4. Enforce at most one active HOD per department via partial unique index
CREATE UNIQUE INDEX idx_unique_department_active_hod
    ON responsibilities (department_id)
    WHERE type = 'HOD_DEPT' AND status = 'ACTIVE';
