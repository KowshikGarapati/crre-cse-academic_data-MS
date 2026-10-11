-- V6: V2.1 Curriculum, Units, Concepts, Learning Resources & User Flag
-- 1. Add must_change_password flag to users
ALTER TABLE users ADD COLUMN IF NOT EXISTS must_change_password BOOLEAN NOT NULL DEFAULT FALSE;

-- 2. Relax global subjects code uniqueness to per-program code uniqueness
ALTER TABLE subjects DROP CONSTRAINT IF EXISTS subjects_code_key;
ALTER TABLE subjects DROP CONSTRAINT IF EXISTS uk_subjects_program_code;
ALTER TABLE subjects ADD CONSTRAINT uk_subjects_program_code UNIQUE (program_id, code);

-- 3. Units Table (5 units per subject in JNTUK curriculum)
CREATE TABLE IF NOT EXISTS units (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    subject_id UUID NOT NULL REFERENCES subjects(id) ON DELETE CASCADE,
    unit_number INT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_units_subject_number UNIQUE (subject_id, unit_number)
);

CREATE INDEX IF NOT EXISTS idx_units_subject ON units(subject_id);

-- 4. Concepts Table (Subtopics / learning modules within a unit)
CREATE TABLE IF NOT EXISTS concepts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    unit_id UUID NOT NULL REFERENCES units(id) ON DELETE CASCADE,
    concept_order INT NOT NULL DEFAULT 1,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_concepts_unit_order UNIQUE (unit_id, concept_order)
);

CREATE INDEX IF NOT EXISTS idx_concepts_unit ON concepts(unit_id);

-- 5. Learning Resources Table (YouTube educational videos, PDF notes, syllabus copies)
CREATE TABLE IF NOT EXISTS resources (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    concept_id UUID NOT NULL REFERENCES concepts(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    resource_type VARCHAR(50) NOT NULL CHECK (resource_type IN ('YOUTUBE_VIDEO', 'PDF_DOCUMENT', 'LINK', 'DOCUMENT')),
    url VARCHAR(1000) NOT NULL,
    file_size_bytes BIGINT,
    uploaded_by_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_resources_concept ON resources(concept_id);
CREATE INDEX IF NOT EXISTS idx_resources_uploader ON resources(uploaded_by_user_id);
