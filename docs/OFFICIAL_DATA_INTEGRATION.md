# CRRE Academic DMS — Official Data Integration & Faculty Directory

## Overview

This document details the official academic data integration for Sir C R Reddy College of Engineering (CRRE) Academic DMS for the academic year 2026-27 (2nd Year, 1st Semester). The integration synchronizes verified institutional roll lists, establishes immutable regular student allocations, provides controlled lateral-entry allotment, models official faculty profiles from `sircrrengg.ac.in`, introduces student certificate tracking with tamper-proof verifications, and safely preserves synthetic prototype datasets.

---

## 1. Official Student Enrollment & Section Breakdown

The database synchronizes official institutional Excel rosters:
- `2nd year 1st sem rolls list 2026-27.xlsx` (Regular student admissions)
- `LATERAL ENTRY  ROLLS LIST 26-27 2-1.xlsx` (Lateral-entry admissions)

### Summary Counts
- **Total Students in System**: **2,073**
  - **Official Students**: **723**
    - Regular Students: **656** (646 ACTIVE + 10 INACTIVE/Cancelled)
    - Lateral Students: **67** (59 Section-Allocated + 8 Unallocated)
  - **Synthetic Demo Students**: **1,350** (1,184 Regular + 166 Lateral, soft-deactivated with `is_synthetic = true`, `status = 'INACTIVE'`)

### Section Breakdown (Official Active Students)

| Program | Code | Section | Regular Active | Lateral Active | Cancelled / Inactive | Total Official |
|---|---|---|---|---|---|---|
| Artificial Intelligence & Data Science | `AIDS` | Section 1 | 65 | 6 | 1 | 72 |
| Artificial Intelligence & Data Science | `AIDS` | Section 2 | 65 | 6 | 1 | 72 |
| Artificial Intelligence & Machine Learning | `AIML` | Section 1 | 66 | 5 | 0 | 71 |
| Computer Science | `CS` | Section 1 | 66 | 6 | 0 | 72 |
| Computer Science & Engineering | `CSE` | Section 1 | 65 | 6 | 1 | 72 |
| Computer Science & Engineering | `CSE` | Section 2 | 65 | 6 | 1 | 72 |
| Computer Science & Engineering | `CSE` | Section 3 | 65 | 6 | 1 | 72 |
| Computer Science & Engineering | `CSE` | Section 4 | 65 | 6 | 1 | 72 |
| Computer Science & Engineering | `CSE` | Section 5 | 64 | 6 | 2 | 72 |
| Computer Science & Engineering | `CSE` | Section 6 | 60 | 6 | 2 | 68 |
| *Unallocated Lateral Pool* | *N/A* | *Unallocated* | 0 | 8 | 0 | 8 |
| **Total** | | | **646** | **67** | **10** | **723** |

### Unallocated Lateral Students (8 Students)
The eight unallocated lateral students (`26B85A0537` through `26B85A0544`) have active status, entry type `LATERAL`, and zero initial section enrollments. They can be assigned to sections by authorized personnel (HOD, Principal, Admin) via the lateral reassignment API.

### Retained Inactive / Cancelled Students (10 Students)
Students with official annotations in the institutional records (such as detained in 1st year 2nd semester, discontinued studies, or cancelled seats) are preserved with `status = 'INACTIVE'`:
- `25B81A4501` (AIDS Sec 1) — Detained in 1-2
- `25B81A45D2` (AIDS Sec 2) — Discontinued studies in 1-2
- `25B81A051E` (CSE Sec 1) — Detained in 1-2
- `25B81A0535` (CSE Sec 2) — Cancelled seat
- `25B81A05B8` (CSE Sec 3) — Detained in 1-2
- `25B81A05H1` (CSE Sec 4) — Detained in 1-2
- `25B81A05P0` (CSE Sec 5) — Cancelled
- `25B81A05S0` (CSE Sec 5) — Cancelled seat
- `25B81A05X3` (CSE Sec 6) — Cancelled
- `25B81A05Y8` (CSE Sec 6) — Detained in 1-2

---

## 2. Lateral Section Reassignment & Allotment Architecture

### Rules & Validation
1. **Regular Student Immutability**: Regular student section allotments are fixed by institutional decree and cannot be modified via the API. Any attempt returns HTTP 400 (`"Only lateral-entry students can have their section allotment modified. Regular-student section allotments are fixed."`).
2. **Program & Period Integrity**:
   - Reassigning an allocated lateral student across different academic programs (e.g. CSE to AIDS) or different semesters is strictly rejected.
   - For unallocated lateral students, the branch code embedded in the registration number (e.g., `05` for CSE, `42` for AIML, `45` for AIDS, `46` for CS) is verified against the target program.
3. **Single Active Enrollment Guarantee**:
   - When a reassignment occurs, all existing enrollments for the student have `is_current` set to `false`.
   - A new `StudentEnrollment` is persisted with `is_current = true`.
4. **Audit Trail**: Every reassignment is committed to `system_audit_logs` (`REASSIGN_LATERAL_SECTION` or `ASSIGN_LATERAL_SECTION`) recording previous section, new section, user reason, actor ID, client IP address, and user agent.
5. **Authorization Boundary**: Limited to `ADMIN`, `PRINCIPAL`, and `HOD` (restricted to sections in the HOD's department).

---

## 3. Student Certificate Registry

- Table: `student_certificates`
- Enforces strict ownership checks (`certificate.student.id == student.id`).
- Supports certificate lifecycle statuses: `SUBMITTED`, `PENDING`, `VERIFIED`, `REJECTED`.
- **Tamper-Proof Verifier Metadata**:
  - Client request payloads (`UpsertCertificateRequest`) cannot supply verifier IDs or verification dates.
  - Verification metadata (`verified_by`, `verification_date`) is automatically stamped from the authenticated `caller` session upon transition to `VERIFIED`.
- **Foreign Key Behavior**:
  - `student_certificates.student_id` &rarr; `students(id)` `ON DELETE CASCADE`.
  - `student_certificates.verified_by` &rarr; `users(id)` `ON DELETE SET NULL` (verifications persist even if a faculty or staff account is deactivated).
- **File Upload Scope**: Confirmed metadata-only registry; document file upload is not implemented unless required by policy.

---

## 4. Official Faculty Profiles & Public Directory

### Data Sourcing
22 official Computer Science & Engineering faculty members scraped from the institutional website (`https://sircrrengg.ac.in/departments/computer-science-and-engineering/faculty`):
- `CRRE-FAC-OFF-001` (Dr. A. Yesubabu, Professor & HOD) through `CRRE-FAC-OFF-022` (P. Pavani, Assistant Professor).
- Maintained in `faculty_profiles` with `user_id = NULL` and `is_synthetic = false`.

### Governance & Boundaries
- Public directory entries do not automatically generate login accounts or assign teaching responsibilities.
- Profile maintenance (`PUT /api/v1/faculty/profiles/{id}` or `PUT /api/v1/faculty-profiles/{id}`) updates biographical, qualification, experience, and contact metadata.
- Updates cannot generate user credentials or modify records in the `responsibilities` table.
- Accessible at both `/api/v1/faculty/profiles` and alias `/api/v1/faculty-profiles`.

---

## 5. Foreign Key & Deletion Integrity Matrix

| Dependent Table | Foreign Key Column | Target Table | On Delete Rule | Behavioral Guarantee |
|---|---|---|---|---|
| `students` | `user_id` | `users(id)` | `SET NULL` | User account removal never deletes student record |
| `student_certificates` | `verified_by` | `users(id)` | `SET NULL` | Verifications preserved when verifier user deleted |
| `system_audit_logs` | `actor_id` | `users(id)` | `SET NULL` | Audit records preserved when actor account removed |
| `student_enrollments` | `student_id` | `students(id)` | `CASCADE` | Enrollment history tied to student lifecycle |
| `student_educational_history` | `student_id` | `students(id)` | `CASCADE` | Academic history tied to student lifecycle |
| `counselling_assignments` | `student_id` | `students(id)` | `CASCADE` | Assignments cleaned if student deleted |
| `counselling_records` | `student_id` | `students(id)` | `CASCADE` | Meeting notes cleaned if student deleted |

---

## 6. Verification & Test Execution Summary

- **Frontend Production Build**:
  - `npm --prefix frontend run build` &rarr; **Zero TypeScript/Vite errors**, production artifacts generated.
- **Backend Test Suite**:
  - `LateralAllotmentFacultyAndCertificateIntegrationTest` (8 tests): Passed.
  - `StudentImportIntegrationTest` (7 tests): Passed.
  - Complete backend test suite (9 test classes, 71 tests):
    - `AcademicIntegrityIntegrationTest` (3 tests)
    - `AuthControllerIntegrationTest` (24 tests)
    - `CrreAcademicDmsApplicationTests` (1 test)
    - `HodAuthorizationIntegrationTest` (2 tests)
    - `LateralAllotmentFacultyAndCertificateIntegrationTest` (8 tests)
    - `PasswordHashTest` (1 test)
    - `ScopeAndCurriculumIntegrationTest` (10 tests)
    - `StudentAndCounsellorManagementIntegrationTest` (5 tests)
    - `StudentImportIntegrationTest` (7 tests)
  - **Result: `Tests run: 71, Failures: 0, Errors: 0, Skipped: 0 — BUILD SUCCESS`**.
