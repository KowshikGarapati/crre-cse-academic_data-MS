-- V5: Audit Correction: Faculty Profiles, Student Educational History, Counselling Assignments & Records, Timetable Room

-- 1. Add room_number to timetables
ALTER TABLE timetables ADD COLUMN IF NOT EXISTS room_number VARCHAR(50) DEFAULT 'LH-301';
UPDATE timetables SET room_number = CASE 
    WHEN period_number IN (5, 6, 7) AND subject_id IN (
        SELECT id FROM subjects WHERE type = 'LAB'
    ) THEN 'AI Lab - 2'
    ELSE 'LH-301'
END WHERE room_number IS NULL OR room_number = 'LH-301';

-- 2. Faculty Profiles Table (Separate from User, no Program field)
CREATE TABLE IF NOT EXISTS faculty_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    employee_number VARCHAR(50) NOT NULL UNIQUE,
    department_id UUID NOT NULL REFERENCES departments(id) ON DELETE CASCADE,
    designation VARCHAR(100) NOT NULL,
    qualification VARCHAR(100) NOT NULL,
    specialization VARCHAR(150),
    professional_experience VARCHAR(255),
    institutional_contact VARCHAR(100),
    active_status BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_faculty_profiles_dept ON faculty_profiles(department_id);

-- 3. Student Educational History (Generic, reusable for SSC, INTERMEDIATE, DIPLOMA, OTHER)
CREATE TABLE IF NOT EXISTS student_educational_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id UUID NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    qualification_type VARCHAR(50) NOT NULL CHECK (qualification_type IN ('SSC', 'INTERMEDIATE', 'DIPLOMA', 'OTHER')),
    board_or_university VARCHAR(150) NOT NULL,
    institution_name VARCHAR(255) NOT NULL,
    hall_ticket_number VARCHAR(50) NOT NULL,
    completion_year INT NOT NULL,
    stream VARCHAR(100),
    branch VARCHAR(100),
    maximum_marks NUMERIC(7,2),
    marks_obtained NUMERIC(7,2),
    percentage NUMERIC(5,2),
    additional_details TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_student_edu_student ON student_educational_history(student_id);

-- 4. Counselling Assignments Table (Maps faculty counsellor to specific assigned students)
CREATE TABLE IF NOT EXISTS counselling_assignments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    counsellor_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    student_id UUID NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    assigned_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT uk_counsellor_student UNIQUE(counsellor_user_id, student_id)
);

CREATE INDEX IF NOT EXISTS idx_counselling_counsellor ON counselling_assignments(counsellor_user_id);
CREATE INDEX IF NOT EXISTS idx_counselling_student ON counselling_assignments(student_id);

-- 5. Counselling Records Table (Session notes, observations, follow-ups)
CREATE TABLE IF NOT EXISTS counselling_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    counsellor_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    student_id UUID NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    meeting_date DATE NOT NULL DEFAULT CURRENT_DATE,
    record_type VARCHAR(50) NOT NULL DEFAULT 'GENERAL' CHECK (record_type IN ('GENERAL', 'ACADEMIC', 'ATTENDANCE_WARNING', 'CAREER', 'FOLLOW_UP')),
    remarks TEXT NOT NULL,
    action_plan TEXT,
    follow_up_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'RESOLVED', 'PENDING_FOLLOW_UP')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_counselling_records_student ON counselling_records(student_id);

-- ========================================================
-- SEED DATA FOR AUDIT CORRECTIONS
-- ========================================================

-- Seed Faculty Profiles (Belonging to CSE Department, NO Program field)
INSERT INTO faculty_profiles (user_id, employee_number, department_id, designation, qualification, specialization, professional_experience, institutional_contact, active_status)
VALUES
-- Ch. Rama Lakshmi (Faculty + Class Teacher)
('00000000-0000-0000-0000-000000000012', 'EMP-CSE-002', '20000000-0000-0000-0000-000000000001', 'Assistant Professor', 'M.Tech (CSE)', 'Discrete Mathematics & Graph Theory', '8 Years Teaching & Academic Mentorship', 'ramalakshmi@crre.edu', TRUE),
-- Dr. Rajesh Kumar (Normal Faculty without extra responsibilities)
('00000000-0000-0000-0000-000000000003', 'EMP-CSE-001', '20000000-0000-0000-0000-000000000001', 'Associate Professor', 'Ph.D (CSE)', 'Advanced Data Structures & Algorithms', '12 Years Teaching & Research', 'rajesh.kumar@crre.edu', TRUE),
-- S. V. Naga Lakshmi (Faculty + Counsellor)
('00000000-0000-0000-0000-000000000013', 'EMP-CSE-003', '20000000-0000-0000-0000-000000000001', 'Assistant Professor', 'M.Tech (AI)', 'Student Mentorship & AI Systems', '6 Years Academic Guidance', 'nagalakshmi.sv@crre.edu', TRUE),
-- Dr. M. Sreenivasa Rao (HOD CSE)
('00000000-0000-0000-0000-000000000011', 'EMP-CSE-HOD', '20000000-0000-0000-0000-000000000001', 'Professor & HOD', 'Ph.D (Computer Science)', 'Computer Networks & High Performance Computing', '22 Years Academic Leadership', 'hod.cse@crre.edu', TRUE)
ON CONFLICT (employee_number) DO UPDATE SET
    designation = EXCLUDED.designation,
    qualification = EXCLUDED.qualification,
    specialization = EXCLUDED.specialization;

-- Ensure S. V. Naga Lakshmi has role 'FACULTY' with a Counsellor Assignment rather than a disconnected role
UPDATE users SET role = 'FACULTY' WHERE id = '00000000-0000-0000-0000-000000000013';

-- Update a student as LATERAL ENTRY (e.g. 25B81A4266)
UPDATE students 
SET entry_type = 'LATERAL' 
WHERE registration_number = '25B81A4266';

-- Seed Student Educational History (Synthetic data, no sensitive personal numbers)
-- Student 1: 25B81A4201 (Akhil Shariff - Regular Student with SSC + Intermediate)
INSERT INTO student_educational_history (student_id, qualification_type, board_or_university, institution_name, hall_ticket_number, completion_year, stream, maximum_marks, marks_obtained, percentage)
SELECT id, 'SSC', 'Board of Secondary Education AP', 'Z.P. High School, Eluru', 'SYN-SSC-2021-01', 2021, 'General', 600, 538, 89.67
FROM students WHERE registration_number = '25B81A4201'
ON CONFLICT DO NOTHING;

INSERT INTO student_educational_history (student_id, qualification_type, board_or_university, institution_name, hall_ticket_number, completion_year, stream, maximum_marks, marks_obtained, percentage)
SELECT id, 'INTERMEDIATE', 'Board of Intermediate Education AP', 'Sri Chaitanya Junior College, Eluru', 'SYN-IPE-2023-01', 2023, 'MPC', 1000, 924, 92.40
FROM students WHERE registration_number = '25B81A4201'
ON CONFLICT DO NOTHING;

-- Student 2: 25B81A4202 (Tejash - Regular Student)
INSERT INTO student_educational_history (student_id, qualification_type, board_or_university, institution_name, hall_ticket_number, completion_year, stream, maximum_marks, marks_obtained, percentage)
SELECT id, 'SSC', 'Board of Secondary Education AP', 'St. Xavier High School, Vijayawada', 'SYN-SSC-2021-02', 2021, 'General', 600, 510, 85.00
FROM students WHERE registration_number = '25B81A4202'
ON CONFLICT DO NOTHING;

INSERT INTO student_educational_history (student_id, qualification_type, board_or_university, institution_name, hall_ticket_number, completion_year, stream, maximum_marks, marks_obtained, percentage)
SELECT id, 'INTERMEDIATE', 'Board of Intermediate Education AP', 'Narayana Junior College, Vijayawada', 'SYN-IPE-2023-02', 2023, 'MPC', 1000, 880, 88.00
FROM students WHERE registration_number = '25B81A4202'
ON CONFLICT DO NOTHING;

-- Student 66: 25B81A4266 (Yugandar Patro - Lateral Entry Student with SSC + Diploma)
INSERT INTO student_educational_history (student_id, qualification_type, board_or_university, institution_name, hall_ticket_number, completion_year, branch, maximum_marks, marks_obtained, percentage)
SELECT id, 'SSC', 'Board of Secondary Education AP', 'Municipal High School, Eluru', 'SYN-SSC-2020-66', 2020, 'General', 600, 492, 82.00
FROM students WHERE registration_number = '25B81A4266'
ON CONFLICT DO NOTHING;

INSERT INTO student_educational_history (student_id, qualification_type, board_or_university, institution_name, hall_ticket_number, completion_year, branch, maximum_marks, marks_obtained, percentage)
SELECT id, 'DIPLOMA', 'SBTET Andhra Pradesh', 'Govt Polytechnic College, Vijayawada', 'SYN-DIP-2023-66', 2023, 'Computer Engineering', 1000, 895, 89.50
FROM students WHERE registration_number = '25B81A4266'
ON CONFLICT DO NOTHING;

-- Seed Counselling Assignments (Assign students 25B81A4201 to 25B81A4215 to counsellor S. V. Naga Lakshmi)
INSERT INTO counselling_assignments (counsellor_user_id, student_id, status)
SELECT '00000000-0000-0000-0000-000000000013', id, 'ACTIVE'
FROM students
WHERE registration_number BETWEEN '25B81A4201' AND '25B81A4215'
ON CONFLICT (counsellor_user_id, student_id) DO NOTHING;

-- Seed Sample Counselling Records for Demonstration
INSERT INTO counselling_records (counsellor_user_id, student_id, meeting_date, record_type, remarks, action_plan, follow_up_date, status)
SELECT 
    '00000000-0000-0000-0000-000000000013',
    id,
    CURRENT_DATE - INTERVAL '14 days',
    'ACADEMIC',
    'Reviewed 3rd semester subject readiness. Strong interest in Advanced Data Structures and AI algorithms.',
    'Recommended attending AI Tools practice sessions in the lab.',
    CURRENT_DATE + INTERVAL '14 days',
    'OPEN'
FROM students WHERE registration_number = '25B81A4201'
ON CONFLICT DO NOTHING;

INSERT INTO counselling_records (counsellor_user_id, student_id, meeting_date, record_type, remarks, action_plan, follow_up_date, status)
SELECT 
    '00000000-0000-0000-0000-000000000013',
    id,
    CURRENT_DATE - INTERVAL '7 days',
    'ATTENDANCE_WARNING',
    'Attendance in period 1 monitored. Advised student regarding morning commute timing.',
    'Student agreed to report 10 minutes prior to 09:30 AM start.',
    CURRENT_DATE + INTERVAL '7 days',
    'PENDING_FOLLOW_UP'
FROM students WHERE registration_number = '25B81A4204'
ON CONFLICT DO NOTHING;
