-- ============================================================================
-- Migration V7: Academic Timetable Integrity & Assignment Context (V2.2)
-- Enforces:
-- 1. Full academic context on responsibilities (academic_year_id, semester_id)
-- 2. Clean duplicate inactive/stray responsibilities
-- 3. Unique Class Teacher per section
-- 4. Canonical period times (P1..P7)
-- 5. Timetable teaching assignment link & session type
-- 6. Strict database-level faculty conflict prevention (UNIQUE faculty_user_id, day_of_week, period_number)
-- 7. Update CS program name to 'Cyber Security'
-- ============================================================================

-- 1. Update CS Program Name to 'Cyber Security'
UPDATE programs
SET name = 'Cyber Security'
WHERE code = 'CS' AND (name = 'Computer Science' OR name = 'B.Tech Computer Science');

-- 2. Add academic context columns to responsibilities
ALTER TABLE responsibilities
ADD COLUMN IF NOT EXISTS academic_year_id UUID REFERENCES academic_years(id) ON DELETE SET NULL,
ADD COLUMN IF NOT EXISTS semester_id UUID REFERENCES semesters(id) ON DELETE SET NULL;

-- Backfill academic_year_id and semester_id from section relationship
UPDATE responsibilities r
SET semester_id = s.semester_id,
    academic_year_id = sem.academic_year_id
FROM sections s
JOIN semesters sem ON s.semester_id = sem.id
WHERE r.section_id = s.id AND r.semester_id IS NULL;

-- 3. Clean duplicate INACTIVE responsibilities created during previous test runs
DELETE FROM responsibilities
WHERE status = 'INACTIVE'
  AND id IN (
    SELECT id FROM (
      SELECT id, ROW_NUMBER() OVER (
        PARTITION BY user_id, type, COALESCE(section_id, '00000000-0000-0000-0000-000000000000'), COALESCE(subject_id, '00000000-0000-0000-0000-000000000000')
        ORDER BY created_at ASC, id ASC
      ) as rn
      FROM responsibilities
      WHERE status = 'INACTIVE'
    ) t
    WHERE t.rn > 1
  );

-- 4. Ensure exactly one active CLASS_TEACHER per section
CREATE UNIQUE INDEX IF NOT EXISTS idx_unique_section_class_teacher
ON responsibilities (section_id)
WHERE type = 'CLASS_TEACHER' AND status = 'ACTIVE';

-- 5. Add columns to timetables
ALTER TABLE timetables
ADD COLUMN IF NOT EXISTS teaching_assignment_id UUID REFERENCES responsibilities(id) ON DELETE SET NULL,
ADD COLUMN IF NOT EXISTS session_type VARCHAR(20) NOT NULL DEFAULT 'THEORY';

-- 6. Canonical institutional period timings (P1..P7)
UPDATE timetables SET start_time = '09:30:00', end_time = '10:20:00' WHERE period_number = 1;
UPDATE timetables SET start_time = '10:20:00', end_time = '11:10:00' WHERE period_number = 2;
UPDATE timetables SET start_time = '11:30:00', end_time = '12:20:00' WHERE period_number = 3;
UPDATE timetables SET start_time = '12:20:00', end_time = '13:10:00' WHERE period_number = 4;
UPDATE timetables SET start_time = '14:10:00', end_time = '15:00:00' WHERE period_number = 5;
UPDATE timetables SET start_time = '15:00:00', end_time = '15:50:00' WHERE period_number = 6;
UPDATE timetables SET start_time = '15:50:00', end_time = '16:30:00' WHERE period_number = 7;

-- 7. Remove conflicting duplicate faculty timetable rows before adding unique constraint
DELETE FROM timetables
WHERE id IN (
  '67a3e67f-065c-4904-81c4-2491bf337d30',
  '6a013542-aec4-472b-b531-5eb309e62044',
  '4cf00ac5-2f16-4e87-b239-00c0c83167ed'
);

-- Delete any other potential duplicate faculty conflicts if present
DELETE FROM timetables t1
USING timetables t2
WHERE t1.id > t2.id
  AND t1.faculty_user_id IS NOT NULL
  AND t1.faculty_user_id = t2.faculty_user_id
  AND t1.day_of_week = t2.day_of_week
  AND t1.period_number = t2.period_number;

-- 8. Add critical unique constraint: Faculty cannot teach two sections during same day + period
ALTER TABLE timetables
DROP CONSTRAINT IF EXISTS uk_timetable_faculty_day_period;

ALTER TABLE timetables
ADD CONSTRAINT uk_timetable_faculty_day_period
UNIQUE (faculty_user_id, day_of_week, period_number);

-- 9. Link timetable entries to active teaching responsibilities
UPDATE timetables t
SET teaching_assignment_id = r.id
FROM responsibilities r
WHERE r.user_id = t.faculty_user_id
  AND r.section_id = t.section_id
  AND r.subject_id = t.subject_id
  AND r.type = 'TEACHING'
  AND r.status = 'ACTIVE'
  AND t.teaching_assignment_id IS NULL;
