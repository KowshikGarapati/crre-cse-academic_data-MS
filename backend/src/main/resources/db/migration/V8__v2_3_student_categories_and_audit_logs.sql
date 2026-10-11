-- ============================================================================
-- V8__v2_3_student_categories_and_audit_logs.sql
-- CRRE Academic DMS - Institutional Data Architecture V2.3
-- Scope:
--   1. Add nullable admission_category and tuition_category to students
--   2. Enforce domain check constraints for allowed category enumerations
--   3. Create system_audit_logs table with indexes for governance
-- ============================================================================

-- 1. Add nullable student admission and tuition category columns
ALTER TABLE students
    ADD COLUMN admission_category VARCHAR(30) NULL,
    ADD COLUMN tuition_category VARCHAR(30) NULL;

-- 2. Add domain check constraints for admission and tuition categories
ALTER TABLE students
    ADD CONSTRAINT chk_admission_category
        CHECK (admission_category IS NULL OR admission_category IN ('CONVENOR', 'MANAGEMENT')),
    ADD CONSTRAINT chk_tuition_category
        CHECK (tuition_category IS NULL OR tuition_category IN ('RTF', 'SELF_FINANCE'));

-- 3. Create system_audit_logs table for administrative accountability
CREATE TABLE system_audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_id UUID NULL REFERENCES users(id) ON DELETE SET NULL,
    actor_username VARCHAR(100) NOT NULL,
    actor_role VARCHAR(50) NOT NULL,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id VARCHAR(100) NULL,
    old_value JSONB NULL,
    new_value JSONB NULL,
    ip_address VARCHAR(45) NULL,
    user_agent VARCHAR(500) NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 4. Create performance and traceability indexes on system_audit_logs
CREATE INDEX idx_audit_logs_actor_id ON system_audit_logs(actor_id);
CREATE INDEX idx_audit_logs_entity ON system_audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_logs_created_at ON system_audit_logs(created_at DESC);
