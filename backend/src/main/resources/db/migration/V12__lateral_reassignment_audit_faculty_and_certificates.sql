-- ==============================================================================
-- V12: Lateral Section Management, System Audit Logs, Faculty Web Profiles & Student Certificates
-- ==============================================================================

-- 1. Faculty Profiles Schema Enhancement:
-- Allow published institutional faculty profiles from sircrrengg.ac.in without requiring login accounts.
ALTER TABLE faculty_profiles ALTER COLUMN user_id DROP NOT NULL;
ALTER TABLE faculty_profiles ADD COLUMN IF NOT EXISTS faculty_name VARCHAR(150);
ALTER TABLE faculty_profiles ADD COLUMN IF NOT EXISTS source_url VARCHAR(500);
ALTER TABLE faculty_profiles ADD COLUMN IF NOT EXISTS is_synthetic BOOLEAN NOT NULL DEFAULT false;

-- Backfill faculty_name from linked user accounts for existing synthetic faculty
UPDATE faculty_profiles fp
SET faculty_name = u.full_name,
    is_synthetic = true
FROM users u
WHERE fp.user_id = u.id AND (fp.faculty_name IS NULL OR fp.faculty_name = '');

-- Index for searching faculty profiles by department and synthetic status
CREATE INDEX IF NOT EXISTS idx_faculty_profiles_dept_synthetic ON faculty_profiles(department_id, is_synthetic);

-- 2. Student Certificates Schema:
-- Normalized student-linked certificate tracking model to record submission, verification, and audit of admission certificates.
CREATE TABLE IF NOT EXISTS student_certificates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id UUID NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    certificate_type VARCHAR(50) NOT NULL,
    certificate_name VARCHAR(150) NOT NULL,
    certificate_identifier VARCHAR(100) NULL,
    issuing_authority VARCHAR(150) NULL,
    issue_year INTEGER NULL,
    issue_date DATE NULL,
    submission_status VARCHAR(50) NOT NULL DEFAULT 'SUBMITTED',
    verified_by UUID NULL REFERENCES users(id) ON DELETE SET NULL,
    verification_date TIMESTAMP WITH TIME ZONE NULL,
    remarks TEXT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_certificate_status CHECK (submission_status IN ('SUBMITTED', 'VERIFIED', 'PENDING', 'RETURNED'))
);

CREATE INDEX IF NOT EXISTS idx_student_certificates_student_id ON student_certificates(student_id);
CREATE INDEX IF NOT EXISTS idx_student_certificates_type ON student_certificates(certificate_type);
CREATE INDEX IF NOT EXISTS idx_student_certificates_status ON student_certificates(submission_status);
