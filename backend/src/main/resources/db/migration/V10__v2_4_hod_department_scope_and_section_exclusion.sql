-- ============================================================================
-- V10__v2_4_hod_department_scope_and_section_exclusion.sql
-- CRRE Academic DMS - Institutional Data Architecture V2.4
-- Scope:
--   1. Enforce department-level scope for HOD responsibilities (nullify section_id & program_id)
--   2. Add check constraint chk_hod_dept_section_null to enforce section_id IS NULL for HOD_DEPT
-- ============================================================================

-- 1. Ensure all HOD_DEPT responsibilities are strictly department-scoped (no section or program association)
UPDATE responsibilities
SET section_id = NULL,
    program_id = NULL,
    updated_at = CURRENT_TIMESTAMP
WHERE type = 'HOD_DEPT';

-- 2. Add check constraint on responsibilities table to prevent section-specific HOD assignments
ALTER TABLE responsibilities
    ADD CONSTRAINT chk_hod_dept_section_null
        CHECK (type <> 'HOD_DEPT' OR section_id IS NULL);
