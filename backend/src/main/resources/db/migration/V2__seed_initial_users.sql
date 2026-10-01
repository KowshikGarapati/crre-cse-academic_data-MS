-- V2: Seed initial test accounts for ADMIN, STUDENT, and FACULTY roles
-- Passwords:
-- admin / admin@crre.edu / ADM20260001 -> Admin@123
-- 22b91a0501 / student@crre.edu / 22B91A0501 / CRRE20220501 -> Student@123
-- faculty.cse / faculty@crre.edu / FAC2020CSE01 -> Faculty@123

INSERT INTO users (
    id,
    username,
    email,
    password_hash,
    full_name,
    role,
    registration_number,
    unique_id,
    aadhar_number,
    apaar_number,
    enabled,
    created_at,
    updated_at
) VALUES
(
    '00000000-0000-0000-0000-000000000001',
    'admin',
    'admin@crre.edu',
    '$2a$10$D9iExUHGoIpia0tRLItptOa/xBjSjEMHMdig4gxWlcbNbh.Bo4c62',
    'System Administrator',
    'ADMIN',
    NULL,
    'ADM20260001',
    NULL,
    NULL,
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),
(
    '00000000-0000-0000-0000-000000000002',
    '22b91a0501',
    'student@crre.edu',
    '$2a$10$R7oDdzga0IX2qxxSKSbISe40uQCa4gki8pEx0c2j8nJLe6LKSxNo.',
    'Kowshik Garapati',
    'STUDENT',
    '22B91A0501',
    'CRRE20220501',
    789012345678,
    987654321012,
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),
(
    '00000000-0000-0000-0000-000000000003',
    'faculty.cse',
    'faculty@crre.edu',
    '$2a$10$Jhq2rvJVYIGXNMzBzRZVJOXnVJ2XWXLh0EFu4GL4ZodpQemYPHK2m',
    'Dr. Rajesh Kumar',
    'FACULTY',
    NULL,
    'FAC2020CSE01',
    654321098765,
    876543210987,
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (username) DO UPDATE SET
    email = EXCLUDED.email,
    password_hash = EXCLUDED.password_hash,
    full_name = EXCLUDED.full_name,
    role = EXCLUDED.role,
    registration_number = EXCLUDED.registration_number,
    unique_id = EXCLUDED.unique_id,
    aadhar_number = EXCLUDED.aadhar_number,
    apaar_number = EXCLUDED.apaar_number,
    updated_at = CURRENT_TIMESTAMP;
