package com.crre.academicdms.features.student.service;

import com.crre.academicdms.features.academic.dto.AcademicDtos.StudentDto;
import com.crre.academicdms.features.academic.model.Department;
import com.crre.academicdms.features.academic.model.Section;
import com.crre.academicdms.features.academic.repository.SectionRepository;
import com.crre.academicdms.features.academic.service.HodService;
import com.crre.academicdms.features.counselling.model.CounsellingAssignment;
import com.crre.academicdms.features.counselling.repository.CounsellingAssignmentRepository;
import com.crre.academicdms.features.responsibility.model.Responsibility;
import com.crre.academicdms.features.responsibility.repository.ResponsibilityRepository;
import com.crre.academicdms.features.student.model.Student;
import com.crre.academicdms.features.student.model.StudentEnrollment;
import com.crre.academicdms.features.student.repository.StudentEnrollmentRepository;
import com.crre.academicdms.features.student.repository.StudentRepository;
import com.crre.academicdms.features.user.model.Role;
import com.crre.academicdms.security.UserPrincipal;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class StudentScopeService {

    private final StudentRepository studentRepository;
    private final StudentEnrollmentRepository enrollmentRepository;
    private final ResponsibilityRepository responsibilityRepository;
    private final CounsellingAssignmentRepository counsellingAssignmentRepository;
    private final SectionRepository sectionRepository;
    private final HodService hodService;

    public StudentScopeService(
        StudentRepository studentRepository,
        StudentEnrollmentRepository enrollmentRepository,
        ResponsibilityRepository responsibilityRepository,
        CounsellingAssignmentRepository counsellingAssignmentRepository,
        SectionRepository sectionRepository,
        HodService hodService
    ) {
        this.studentRepository = studentRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.responsibilityRepository = responsibilityRepository;
        this.counsellingAssignmentRepository = counsellingAssignmentRepository;
        this.sectionRepository = sectionRepository;
        this.hodService = hodService;
    }

    /**
     * Resolves a student by UUID, User UUID, or Registration Number (case-insensitive).
     */
    @Transactional(readOnly = true)
    public Student resolveStudent(String identifier) {
        if (identifier == null || identifier.trim().isEmpty()) {
            throw new IllegalArgumentException("Student identifier cannot be blank");
        }
        String cleanId = identifier.trim();

        // 1. Try UUID parse
        try {
            UUID uuid = UUID.fromString(cleanId);
            Optional<Student> byId = studentRepository.findById(uuid);
            if (byId.isPresent()) {
                return byId.get();
            }
            Optional<Student> byUserId = studentRepository.findByUserId(uuid);
            if (byUserId.isPresent()) {
                return byUserId.get();
            }
        } catch (IllegalArgumentException ignored) {
            // Not a UUID format, proceed to registration number lookup
        }

        // 2. Try Registration Number
        return studentRepository.findByRegistrationNumberIgnoreCase(cleanId)
            .orElseThrow(() -> new IllegalArgumentException("Student not found with identifier: " + cleanId));
    }

    /**
     * Authoritative check whether a caller has legitimate access to a specific student.
     */
    @Transactional(readOnly = true)
    public boolean isStudentAccessible(Student student, UserPrincipal caller) {
        if (caller == null || student == null) {
            return false;
        }

        Role role = caller.getRole();
        if (role == Role.ADMIN || role == Role.PRINCIPAL) {
            return true;
        }

        // Student's active or latest enrollment section
        Section section = resolveStudentSection(student);

        if (role == Role.HOD || caller.isHod()) {
            Department hodDept = hodService.resolveHodDepartment(caller);
            if (section != null && section.getSemester() != null
                && section.getSemester().getProgram() != null
                && section.getSemester().getProgram().getDepartment() != null) {
                return section.getSemester().getProgram().getDepartment().getId().equals(hodDept.getId());
            }
            return false;
        }

        if (role == Role.STUDENT) {
            boolean isOwn = (student.getUser() != null && student.getUser().getId().equals(caller.getId()))
                || (student.getRegistrationNumber() != null && student.getRegistrationNumber().equalsIgnoreCase(caller.getUsername()));
            return isOwn;
        }

        // FACULTY, CLASS_TEACHER, COUNSELLOR
        // 1. Active assigned counsellor for this student?
        if (counsellingAssignmentRepository.existsByCounsellorIdAndStudentIdAndStatus(caller.getId(), student.getId(), "ACTIVE")) {
            return true;
        }

        // 2. Class teacher or teaching faculty for student's section?
        if (section != null) {
            boolean isClassTeacher = responsibilityRepository.existsActiveSectionRole(caller.getId(), "CLASS_TEACHER", section.getId());
            if (isClassTeacher) {
                return true;
            }
            boolean isTeaching = responsibilityRepository.existsByUserIdAndTypeAndSectionIdAndStatus(caller.getId(), "TEACHING", section.getId(), "ACTIVE");
            if (isTeaching) {
                return true;
            }
        }

        return false;
    }

    /**
     * Authoritative check whether caller is permitted to view confidential counselling records.
     */
    @Transactional(readOnly = true)
    public boolean canViewStudentCounselling(Student student, UserPrincipal caller) {
        if (caller == null || student == null) {
            return false;
        }

        Role role = caller.getRole();
        if (role == Role.ADMIN || role == Role.PRINCIPAL) {
            return true;
        }

        if (role == Role.HOD || caller.isHod()) {
            Department hodDept = hodService.resolveHodDepartment(caller);
            Section section = resolveStudentSection(student);
            if (section != null && section.getSemester() != null
                && section.getSemester().getProgram() != null
                && section.getSemester().getProgram().getDepartment() != null) {
                return section.getSemester().getProgram().getDepartment().getId().equals(hodDept.getId());
            }
            return false;
        }

        if (role == Role.STUDENT) {
            return (student.getUser() != null && student.getUser().getId().equals(caller.getId()))
                || (student.getRegistrationNumber() != null && student.getRegistrationNumber().equalsIgnoreCase(caller.getUsername()));
        }

        // Strictly assigned counsellor only for faculty! Ordinary teaching faculty CANNOT view records!
        return counsellingAssignmentRepository.existsByCounsellorIdAndStudentIdAndStatus(caller.getId(), student.getId(), "ACTIVE");
    }

    /**
     * Authoritative check whether caller is authorized to edit a student's profile and academic data.
     */
    @Transactional(readOnly = true)
    public boolean canEditStudent(Student student, UserPrincipal caller) {
        if (caller == null || student == null) {
            return false;
        }

        Role role = caller.getRole();
        if (role == Role.STUDENT) {
            return false;
        }

        if (role == Role.ADMIN || role == Role.PRINCIPAL) {
            return true;
        }

        Section section = resolveStudentSection(student);

        if (role == Role.HOD || caller.isHod()) {
            Department hodDept = hodService.resolveHodDepartment(caller);
            if (section != null && section.getSemester() != null
                && section.getSemester().getProgram() != null
                && section.getSemester().getProgram().getDepartment() != null) {
                return section.getSemester().getProgram().getDepartment().getId().equals(hodDept.getId());
            }
            return false;
        }

        // Faculty: Class teacher of section, Teaching faculty of section, or assigned Counsellor
        if (section != null) {
            if (responsibilityRepository.existsActiveSectionRole(caller.getId(), "CLASS_TEACHER", section.getId())) {
                return true;
            }
            if (responsibilityRepository.existsByUserIdAndTypeAndSectionIdAndStatus(caller.getId(), "TEACHING", section.getId(), "ACTIVE")) {
                return true;
            }
        }

        if (counsellingAssignmentRepository.existsByCounsellorIdAndStudentIdAndStatus(caller.getId(), student.getId(), "ACTIVE")) {
            return true;
        }

        return false;
    }

    @Transactional(readOnly = true)
    public List<StudentDto> getAccessibleStudents(UUID sectionId, UserPrincipal caller) {
        if (sectionId == null) {
            return getAccessibleStudents(caller);
        }
        if (caller == null) {
            throw new AccessDeniedException("Authentication required");
        }

        Role role = caller.getRole();
        boolean authorized = (role == Role.ADMIN || role == Role.PRINCIPAL);

        if (!authorized && (role == Role.HOD || caller.isHod())) {
            Department hodDept = hodService.resolveHodDepartment(caller);
            Section sec = sectionRepository.findById(sectionId).orElse(null);
            if (sec != null && sec.getSemester() != null
                && sec.getSemester().getProgram() != null
                && sec.getSemester().getProgram().getDepartment() != null) {
                authorized = sec.getSemester().getProgram().getDepartment().getId().equals(hodDept.getId());
            }
        }

        if (!authorized) {
            boolean isClassTeacher = responsibilityRepository.existsActiveSectionRole(caller.getId(), "CLASS_TEACHER", sectionId);
            boolean isTeaching = responsibilityRepository.existsByUserIdAndTypeAndSectionIdAndStatus(caller.getId(), "TEACHING", sectionId, "ACTIVE");
            authorized = isClassTeacher || isTeaching;
        }

        if (!authorized) {
            throw new AccessDeniedException("Access denied: You are not authorized to view students for section " + sectionId);
        }

        List<StudentEnrollment> enrollments = enrollmentRepository.findBySectionIdAndCurrentTrueOrderByStudentRegistrationNumberAsc(sectionId);
        if (enrollments.isEmpty()) {
            enrollments = enrollmentRepository.findBySectionId(sectionId);
        }
        return mapEnrollmentsToDtos(enrollments);
    }

    /**
     * Returns all students legitimately accessible to caller according to role and responsibilities.
     */
    @Transactional(readOnly = true)
    public List<StudentDto> getAccessibleStudents(UserPrincipal caller) {
        if (caller == null) {
            throw new AccessDeniedException("Authentication required");
        }

        Role role = caller.getRole();

        // 1. PRINCIPAL or ADMIN -> All students in institution
        if (role == Role.PRINCIPAL || role == Role.ADMIN) {
            List<Student> allStudents = studentRepository.findAllByOrderByRegistrationNumberAsc();
            return mapStudentsToDtos(allStudents);
        }

        // 2. HOD -> All students in programs under HOD's department
        if (role == Role.HOD || caller.isHod()) {
            Department hodDept = hodService.resolveHodDepartment(caller);
            List<Section> deptSections = sectionRepository.findByDepartmentId(hodDept.getId());
            List<UUID> secIds = deptSections.stream().map(Section::getId).toList();
            if (secIds.isEmpty()) {
                return List.of();
            }
            List<StudentEnrollment> enrollments = enrollmentRepository.findBySectionIdInAndCurrentTrue(secIds);
            return mapEnrollmentsToDtos(enrollments);
        }

        // 3. STUDENT -> Self only
        if (role == Role.STUDENT) {
            Student self = studentRepository.findByUserId(caller.getId())
                .orElseGet(() -> studentRepository.findByRegistrationNumberIgnoreCase(caller.getUsername())
                    .orElse(null));
            if (self == null) return List.of();
            return mapStudentsToDtos(List.of(self));
        }

        // 4. FACULTY, CLASS_TEACHER, COUNSELLOR
        // Union of:
        //  - Sections where caller is Class Teacher
        //  - Sections where caller has active Teaching responsibility
        //  - Specific students assigned as Counsellor mentees
        Set<UUID> targetSectionIds = new HashSet<>();
        List<Responsibility> resps = responsibilityRepository.findByUserIdAndStatus(caller.getId(), "ACTIVE");
        for (Responsibility r : resps) {
            if (("CLASS_TEACHER".equals(r.getType()) || "TEACHING".equals(r.getType())) && r.getSection() != null) {
                targetSectionIds.add(r.getSection().getId());
            }
        }

        List<CounsellingAssignment> counsellorAssignments = counsellingAssignmentRepository.findByCounsellorIdAndStatus(caller.getId(), "ACTIVE");
        Set<UUID> menteeStudentIds = counsellorAssignments.stream()
            .map(ca -> ca.getStudent().getId())
            .collect(Collectors.toSet());

        // Fetch enrollments for sections
        Map<UUID, StudentDto> studentDtoMap = new LinkedHashMap<>();
        if (!targetSectionIds.isEmpty()) {
            List<StudentEnrollment> sectionEnrollments = enrollmentRepository.findBySectionIdInAndCurrentTrue(new ArrayList<>(targetSectionIds));
            for (StudentEnrollment se : sectionEnrollments) {
                Student s = se.getStudent();
                Section sec = se.getSection();
                studentDtoMap.put(s.getId(), toStudentDto(s, sec));
            }
        }

        // Fetch mentees if any aren't already included
        if (!menteeStudentIds.isEmpty()) {
            List<UUID> missingIds = menteeStudentIds.stream()
                .filter(id -> !studentDtoMap.containsKey(id))
                .toList();
            if (!missingIds.isEmpty()) {
                List<StudentEnrollment> menteeEnrollments = enrollmentRepository.findByStudentIdInAndCurrentTrue(missingIds);
                for (StudentEnrollment me : menteeEnrollments) {
                    Student s = me.getStudent();
                    Section sec = me.getSection();
                    studentDtoMap.put(s.getId(), toStudentDto(s, sec));
                }
                // Even if not enrolled in current semester, include the student record
                for (UUID id : missingIds) {
                    if (!studentDtoMap.containsKey(id)) {
                        studentRepository.findById(id).ifPresent(s -> {
                            studentDtoMap.put(s.getId(), toStudentDto(s, null));
                        });
                    }
                }
            }
        }

        return studentDtoMap.values().stream()
            .sorted(Comparator.comparing(StudentDto::registrationNumber))
            .toList();
    }

    private List<StudentDto> mapStudentsToDtos(List<Student> students) {
        if (students.isEmpty()) return List.of();
        List<UUID> ids = students.stream().map(Student::getId).toList();
        List<StudentEnrollment> enrollments = enrollmentRepository.findByStudentIdInAndCurrentTrue(ids);
        Map<UUID, Section> studentSectionMap = enrollments.stream()
            .collect(Collectors.toMap(e -> e.getStudent().getId(), StudentEnrollment::getSection, (existing, replacement) -> existing));

        return students.stream()
            .sorted(Comparator.comparing(Student::getRegistrationNumber))
            .map(s -> toStudentDto(s, studentSectionMap.get(s.getId())))
            .toList();
    }

    private List<StudentDto> mapEnrollmentsToDtos(List<StudentEnrollment> enrollments) {
        return enrollments.stream()
            .sorted(Comparator.comparing(e -> e.getStudent().getRegistrationNumber()))
            .map(e -> toStudentDto(e.getStudent(), e.getSection()))
            .toList();
    }

    private Section resolveStudentSection(Student student) {
        if (student == null) {
            return null;
        }
        return enrollmentRepository.findByStudentIdAndCurrentTrue(student.getId())
            .map(StudentEnrollment::getSection)
            .orElseGet(() -> {
                List<StudentEnrollment> anyEnrollments = enrollmentRepository.findByStudentId(student.getId());
                return anyEnrollments.isEmpty() ? null : anyEnrollments.get(0).getSection();
            });
    }

    private StudentDto toStudentDto(Student student, Section section) {
        String secName = section != null ? section.getSectionName() : null;
        UUID secId = section != null ? section.getId() : null;
        String progName = (section != null && section.getSemester() != null && section.getSemester().getProgram() != null)
            ? section.getSemester().getProgram().getName()
            : null;
        String semName = (section != null && section.getSemester() != null)
            ? section.getSemester().getTermName()
            : null;

        return new StudentDto(
            student.getId(),
            student.getRegistrationNumber(),
            student.getFullName(),
            student.getEntryType(),
            student.getStatus(),
            secId,
            secName,
            progName,
            semName
        );
    }
}
