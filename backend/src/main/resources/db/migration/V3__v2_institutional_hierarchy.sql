-- V3: Institutional Hierarchy, Programs, Sections, Subjects, Responsibilities, Students, Timetable
-- 1. Update user roles to include PRINCIPAL and CLASS_TEACHER
ALTER TABLE users DROP CONSTRAINT IF EXISTS chk_user_role;
ALTER TABLE users ADD CONSTRAINT chk_user_role CHECK (role IN ('PRINCIPAL', 'HOD', 'FACULTY', 'COUNSELLOR', 'CLASS_TEACHER', 'STUDENT', 'ADMIN'));

-- 2. Institution table
CREATE TABLE IF NOT EXISTS institutions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 3. Department table (Supports multiple programs; do not hardcode programs here)
CREATE TABLE IF NOT EXISTS departments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL REFERENCES institutions(id) ON DELETE CASCADE,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_departments_institution ON departments(institution_id);

-- 4. Program table (e.g. AI&ML under CSE)
CREATE TABLE IF NOT EXISTS programs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    department_id UUID NOT NULL REFERENCES departments(id) ON DELETE CASCADE,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    degree VARCHAR(50) NOT NULL DEFAULT 'B.Tech',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_programs_department ON programs(department_id);

-- 5. Academic Year table
CREATE TABLE IF NOT EXISTS academic_years (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    year_name VARCHAR(50) NOT NULL UNIQUE,
    year_number INT NOT NULL,
    is_current BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 6. Semester table
CREATE TABLE IF NOT EXISTS semesters (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    program_id UUID NOT NULL REFERENCES programs(id) ON DELETE CASCADE,
    academic_year_id UUID NOT NULL REFERENCES academic_years(id) ON DELETE CASCADE,
    semester_number INT NOT NULL,
    term_name VARCHAR(50) NOT NULL,
    is_current BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_semester_program_year_num UNIQUE(program_id, academic_year_id, semester_number)
);

CREATE INDEX IF NOT EXISTS idx_semesters_prog_year ON semesters(program_id, academic_year_id);

-- 7. Section table
CREATE TABLE IF NOT EXISTS sections (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    semester_id UUID NOT NULL REFERENCES semesters(id) ON DELETE CASCADE,
    section_number INT NOT NULL,
    section_name VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_section_semester_number UNIQUE(semester_id, section_number)
);

CREATE INDEX IF NOT EXISTS idx_sections_semester ON sections(semester_id);

-- 8. Subjects table (Theory = 3.0 credits, Lab = 1.5 credits)
CREATE TABLE IF NOT EXISTS subjects (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    program_id UUID NOT NULL REFERENCES programs(id) ON DELETE CASCADE,
    semester_id UUID NOT NULL REFERENCES semesters(id) ON DELETE CASCADE,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(20) NOT NULL CHECK (type IN ('THEORY', 'LAB')),
    credits NUMERIC(3,1) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_subjects_semester ON subjects(semester_id);

-- 9. Responsibilities (Assignments) table
-- Independent assignment lifecycle: ADD, REMOVE, REASSIGN, MODIFY without user recreation
CREATE TABLE IF NOT EXISTS responsibilities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL CHECK (type IN ('TEACHING', 'CLASS_TEACHER', 'HOD_DEPT', 'COUNSELLOR', 'PRINCIPAL_INSTITUTION')),
    department_id UUID REFERENCES departments(id) ON DELETE SET NULL,
    program_id UUID REFERENCES programs(id) ON DELETE SET NULL,
    subject_id UUID REFERENCES subjects(id) ON DELETE SET NULL,
    section_id UUID REFERENCES sections(id) ON DELETE SET NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')),
    description VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_responsibilities_user ON responsibilities(user_id);
CREATE INDEX IF NOT EXISTS idx_responsibilities_dept ON responsibilities(department_id);
CREATE INDEX IF NOT EXISTS idx_responsibilities_section ON responsibilities(section_id);
CREATE INDEX IF NOT EXISTS idx_responsibilities_subject ON responsibilities(subject_id);

-- Enforce uniqueness on active assignments to prevent duplicate active roles
CREATE UNIQUE INDEX IF NOT EXISTS idx_unique_active_teaching
    ON responsibilities (user_id, subject_id, section_id)
    WHERE type = 'TEACHING' AND status = 'ACTIVE';

CREATE UNIQUE INDEX IF NOT EXISTS idx_unique_active_class_teacher
    ON responsibilities (user_id, section_id)
    WHERE type = 'CLASS_TEACHER' AND status = 'ACTIVE';

CREATE UNIQUE INDEX IF NOT EXISTS idx_unique_active_hod
    ON responsibilities (user_id, department_id)
    WHERE type = 'HOD_DEPT' AND status = 'ACTIVE';

CREATE UNIQUE INDEX IF NOT EXISTS idx_unique_active_counsellor
    ON responsibilities (user_id, section_id)
    WHERE type = 'COUNSELLOR' AND status = 'ACTIVE';

-- 10. Students table
CREATE TABLE IF NOT EXISTS students (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    registration_number VARCHAR(20) NOT NULL UNIQUE,
    full_name VARCHAR(100) NOT NULL,
    entry_type VARCHAR(20) NOT NULL DEFAULT 'REGULAR' CHECK (entry_type IN ('REGULAR', 'LATERAL')),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_students_reg_no ON students(registration_number);

-- 11. Student Enrollments table (captures student placement in academic context)
CREATE TABLE IF NOT EXISTS student_enrollments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id UUID NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    section_id UUID NOT NULL REFERENCES sections(id) ON DELETE CASCADE,
    is_current BOOLEAN NOT NULL DEFAULT TRUE,
    enrolled_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_student_enrollment UNIQUE(student_id, section_id)
);

CREATE INDEX IF NOT EXISTS idx_student_enrollments_section ON student_enrollments(section_id);

-- 12. Timetable table
CREATE TABLE IF NOT EXISTS timetables (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    section_id UUID NOT NULL REFERENCES sections(id) ON DELETE CASCADE,
    subject_id UUID NOT NULL REFERENCES subjects(id) ON DELETE CASCADE,
    faculty_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    day_of_week VARCHAR(20) NOT NULL CHECK (day_of_week IN ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY')),
    period_number INT NOT NULL CHECK (period_number BETWEEN 1 AND 7),
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_timetable_section_day_period UNIQUE(section_id, day_of_week, period_number)
);

CREATE INDEX IF NOT EXISTS idx_timetables_section ON timetables(section_id);
CREATE INDEX IF NOT EXISTS idx_timetables_faculty ON timetables(faculty_user_id);

-- 13. Academic Results (Grade structure for future Excel data import)
CREATE TABLE IF NOT EXISTS academic_results (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id UUID NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    subject_id UUID NOT NULL REFERENCES subjects(id) ON DELETE CASCADE,
    internal_marks NUMERIC(5,2),
    external_marks NUMERIC(5,2),
    total_marks NUMERIC(5,2),
    grade_letter VARCHAR(5),
    grade_points NUMERIC(3,1),
    credits_earned NUMERIC(3,1),
    is_pass BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_academic_result_student_subject UNIQUE(student_id, subject_id)
);

CREATE INDEX IF NOT EXISTS idx_academic_results_student ON academic_results(student_id);
