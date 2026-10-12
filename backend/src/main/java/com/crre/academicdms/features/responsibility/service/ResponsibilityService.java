package com.crre.academicdms.features.responsibility.service;

import com.crre.academicdms.features.academic.model.Department;
import com.crre.academicdms.features.academic.model.Program;
import com.crre.academicdms.features.academic.model.Section;
import com.crre.academicdms.features.academic.model.Subject;
import com.crre.academicdms.features.academic.repository.DepartmentRepository;
import com.crre.academicdms.features.academic.repository.ProgramRepository;
import com.crre.academicdms.features.academic.repository.SectionRepository;
import com.crre.academicdms.features.academic.repository.SubjectRepository;
import com.crre.academicdms.features.academic.service.HodService;
import com.crre.academicdms.features.counselling.model.CounsellingAssignment;
import com.crre.academicdms.features.counselling.repository.CounsellingAssignmentRepository;
import com.crre.academicdms.features.responsibility.dto.CounsellorStudentDto;
import com.crre.academicdms.features.responsibility.dto.ResponsibilityRequest;
import com.crre.academicdms.features.responsibility.dto.ResponsibilitySummaryDto;
import com.crre.academicdms.features.responsibility.model.Responsibility;
import com.crre.academicdms.features.responsibility.repository.ResponsibilityRepository;
import com.crre.academicdms.features.student.model.Student;
import com.crre.academicdms.features.student.model.StudentEnrollment;
import com.crre.academicdms.features.student.repository.StudentEnrollmentRepository;
import com.crre.academicdms.features.student.repository.StudentRepository;
import com.crre.academicdms.features.user.model.Role;
import com.crre.academicdms.features.user.model.User;
import com.crre.academicdms.features.user.repository.UserRepository;
import com.crre.academicdms.security.UserPrincipal;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ResponsibilityService {

    private final ResponsibilityRepository responsibilityRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final ProgramRepository programRepository;
    private final SubjectRepository subjectRepository;
    private final SectionRepository sectionRepository;
    private final CounsellingAssignmentRepository counsellingAssignmentRepository;
    private final StudentRepository studentRepository;
    private final StudentEnrollmentRepository enrollmentRepository;
    private final HodService hodService;

    public ResponsibilityService(
        ResponsibilityRepository responsibilityRepository,
        UserRepository userRepository,
        DepartmentRepository departmentRepository,
        ProgramRepository programRepository,
        SubjectRepository subjectRepository,
        SectionRepository sectionRepository,
        CounsellingAssignmentRepository counsellingAssignmentRepository,
        StudentRepository studentRepository,
        StudentEnrollmentRepository enrollmentRepository,
        HodService hodService
    ) {
        this.responsibilityRepository = responsibilityRepository;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.programRepository = programRepository;
        this.subjectRepository = subjectRepository;
        this.sectionRepository = sectionRepository;
        this.counsellingAssignmentRepository = counsellingAssignmentRepository;
        this.studentRepository = studentRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.hodService = hodService;
    }

    @Transactional(readOnly = true)
    public List<ResponsibilitySummaryDto> getActiveResponsibilitiesForUser(UUID userId) {
        return responsibilityRepository.findByUserIdAndStatus(userId, "ACTIVE")
            .stream()
            .map(r -> {
                Integer count = null;
                if ("COUNSELLOR".equalsIgnoreCase(r.getType())) {
                    count = counsellingAssignmentRepository.findByCounsellorIdAndStatus(userId, "ACTIVE").size();
                }
                return ResponsibilitySummaryDto.fromEntity(r, count);
            })
            .toList();
    }

    @Transactional(readOnly = true)
    public List<ResponsibilitySummaryDto> getAllResponsibilitiesForUser(UUID userId) {
        return responsibilityRepository.findByUserId(userId)
            .stream()
            .map(r -> {
                Integer count = null;
                if ("COUNSELLOR".equalsIgnoreCase(r.getType())) {
                    count = counsellingAssignmentRepository.findByCounsellorIdAndStatus(userId, "ACTIVE").size();
                }
                return ResponsibilitySummaryDto.fromEntity(r, count);
            })
            .toList();
    }

    @Transactional
    public ResponsibilitySummaryDto addResponsibility(UUID userId, ResponsibilityRequest req) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

        // Enforce duplicate active assignments prevention
        if ("TEACHING".equalsIgnoreCase(req.type()) && req.subjectId() != null && req.sectionId() != null) {
            if (responsibilityRepository.existsActiveTeaching(userId, req.subjectId(), req.sectionId())) {
                throw new IllegalArgumentException("Duplicate active teaching assignment already exists for this subject and section");
            }
        } else if ("CLASS_TEACHER".equalsIgnoreCase(req.type()) && req.sectionId() != null) {
            if (responsibilityRepository.existsActiveSectionRole(userId, "CLASS_TEACHER", req.sectionId())) {
                throw new IllegalArgumentException("Duplicate active class teacher assignment already exists for this section");
            }
        } else if ("HOD_DEPT".equalsIgnoreCase(req.type()) && req.departmentId() != null) {
            if (responsibilityRepository.existsActiveDeptRole(userId, "HOD_DEPT", req.departmentId())) {
                throw new IllegalArgumentException("Duplicate active HOD assignment already exists for this department");
            }
        }

        Department department = req.departmentId() != null ? departmentRepository.findById(req.departmentId()).orElse(null) : null;
        Program program = req.programId() != null ? programRepository.findById(req.programId()).orElse(null) : null;
        Subject subject = req.subjectId() != null ? subjectRepository.findById(req.subjectId()).orElse(null) : null;
        Section section = req.sectionId() != null ? sectionRepository.findById(req.sectionId()).orElse(null) : null;

        String desc = req.description();
        if (desc == null || desc.isBlank()) {
            if (subject != null && section != null) {
                desc = subject.getName() + " - " + section.getSectionName();
            } else if ("CLASS_TEACHER".equalsIgnoreCase(req.type()) && section != null) {
                desc = "Class Teacher - " + section.getSectionName();
            } else if ("HOD_DEPT".equalsIgnoreCase(req.type()) && department != null) {
                desc = "HOD - " + department.getName();
            } else if ("COUNSELLOR".equalsIgnoreCase(req.type()) && section != null) {
                desc = "Counsellor - " + section.getSectionName();
            } else {
                desc = req.type();
            }
        }

        Responsibility responsibility = new Responsibility(
            user,
            req.type().toUpperCase(),
            department,
            program,
            subject,
            section,
            desc
        );

        Responsibility saved = responsibilityRepository.save(responsibility);

        // If COUNSELLOR and initial students are selected, assign them
        if ("COUNSELLOR".equalsIgnoreCase(req.type()) && req.studentIds() != null && !req.studentIds().isEmpty()) {
            for (UUID sId : req.studentIds()) {
                assignStudentToCounsellorInternal(user, sId);
            }
        }

        Integer count = null;
        if ("COUNSELLOR".equalsIgnoreCase(saved.getType())) {
            count = counsellingAssignmentRepository.findByCounsellorIdAndStatus(userId, "ACTIVE").size();
        }
        return ResponsibilitySummaryDto.fromEntity(saved, count);
    }

    @Transactional
    public ResponsibilitySummaryDto updateResponsibility(UUID responsibilityId, ResponsibilityRequest req) {
        Responsibility r = responsibilityRepository.findById(responsibilityId)
            .orElseThrow(() -> new IllegalArgumentException("Responsibility not found with id: " + responsibilityId));

        if (req.departmentId() != null) {
            r.setDepartment(departmentRepository.findById(req.departmentId()).orElse(null));
        }
        if (req.programId() != null) {
            r.setProgram(programRepository.findById(req.programId()).orElse(null));
        }
        if (req.subjectId() != null) {
            r.setSubject(subjectRepository.findById(req.subjectId()).orElse(null));
        }
        if (req.sectionId() != null) {
            r.setSection(sectionRepository.findById(req.sectionId()).orElse(null));
        }
        if (req.description() != null && !req.description().isBlank()) {
            r.setDescription(req.description());
        }

        Responsibility saved = responsibilityRepository.save(r);
        Integer count = null;
        if ("COUNSELLOR".equalsIgnoreCase(saved.getType())) {
            count = counsellingAssignmentRepository.findByCounsellorIdAndStatus(saved.getUser().getId(), "ACTIVE").size();
        }
        return ResponsibilitySummaryDto.fromEntity(saved, count);
    }

    @Transactional
    public void removeResponsibility(UUID responsibilityId) {
        Responsibility r = responsibilityRepository.findById(responsibilityId)
            .orElseThrow(() -> new IllegalArgumentException("Responsibility not found with id: " + responsibilityId));

        // Mark INACTIVE rather than deleting underlying records; this preserves historical integrity
        r.setStatus("INACTIVE");
        responsibilityRepository.save(r);
    }

    @Transactional(readOnly = true)
    public List<CounsellorStudentDto> getAssignedStudentsForResponsibility(UUID responsibilityId, UserPrincipal caller) {
        Responsibility r = responsibilityRepository.findById(responsibilityId)
            .orElseThrow(() -> new IllegalArgumentException("Responsibility not found with id: " + responsibilityId));

        if (!"COUNSELLOR".equalsIgnoreCase(r.getType())) {
            throw new IllegalArgumentException("Responsibility is not of type COUNSELLOR");
        }

        authorizeCounsellorManagement(r, caller);

        User counsellor = r.getUser();
        List<CounsellingAssignment> assignments = counsellingAssignmentRepository.findByCounsellorIdAndStatus(counsellor.getId(), "ACTIVE");

        return assignments.stream()
            .map(this::mapToCounsellorStudentDto)
            .sorted(Comparator.comparing(CounsellorStudentDto::registrationNumber))
            .toList();
    }

    @Transactional
    public List<CounsellorStudentDto> addStudentsToCounsellor(UUID responsibilityId, List<UUID> studentIds, UserPrincipal caller) {
        Responsibility r = responsibilityRepository.findById(responsibilityId)
            .orElseThrow(() -> new IllegalArgumentException("Responsibility not found with id: " + responsibilityId));

        if (!"COUNSELLOR".equalsIgnoreCase(r.getType())) {
            throw new IllegalArgumentException("Responsibility is not of type COUNSELLOR");
        }

        authorizeCounsellorManagement(r, caller);

        User counsellor = r.getUser();

        // Scope check for HOD: ensure all students belong to HOD's department
        if (caller.getRole() == Role.HOD || caller.isHod()) {
            com.crre.academicdms.features.academic.model.Department hodDept = hodService.resolveHodDepartment(caller);
            for (UUID sId : studentIds) {
                Student s = studentRepository.findById(sId)
                    .orElseThrow(() -> new IllegalArgumentException("Student not found with id: " + sId));
                List<StudentEnrollment> enrollments = enrollmentRepository.findByStudentId(s.getId());
                Section sec = enrollments.isEmpty() ? null : enrollments.get(0).getSection();
                if (sec != null && sec.getSemester() != null && sec.getSemester().getProgram() != null && sec.getSemester().getProgram().getDepartment() != null) {
                    if (!sec.getSemester().getProgram().getDepartment().getId().equals(hodDept.getId())) {
                        throw new AccessDeniedException("HOD cannot assign student outside their department");
                    }
                }
            }
        }

        for (UUID sId : studentIds) {
            assignStudentToCounsellorInternal(counsellor, sId);
        }

        return getAssignedStudentsForResponsibility(responsibilityId, caller);
    }

    @Transactional
    public void removeStudentFromCounsellor(UUID responsibilityId, UUID studentId, UserPrincipal caller) {
        Responsibility r = responsibilityRepository.findById(responsibilityId)
            .orElseThrow(() -> new IllegalArgumentException("Responsibility not found with id: " + responsibilityId));

        if (!"COUNSELLOR".equalsIgnoreCase(r.getType())) {
            throw new IllegalArgumentException("Responsibility is not of type COUNSELLOR");
        }

        authorizeCounsellorManagement(r, caller);

        User counsellor = r.getUser();
        Optional<CounsellingAssignment> existing = counsellingAssignmentRepository.findByCounsellorIdAndStudentId(counsellor.getId(), studentId);
        if (existing.isPresent()) {
            CounsellingAssignment ca = existing.get();
            ca.setStatus("INACTIVE");
            counsellingAssignmentRepository.save(ca);
        }
    }

    private void assignStudentToCounsellorInternal(User counsellor, UUID studentId) {
        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new IllegalArgumentException("Student not found with id: " + studentId));

        Optional<CounsellingAssignment> existing = counsellingAssignmentRepository.findByCounsellorIdAndStudentId(counsellor.getId(), student.getId());
        if (existing.isPresent()) {
            CounsellingAssignment ca = existing.get();
            if (!"ACTIVE".equalsIgnoreCase(ca.getStatus())) {
                ca.setStatus("ACTIVE");
                counsellingAssignmentRepository.save(ca);
            }
            // If already ACTIVE, preserve existing assignment safely (duplicate prevention)
        } else {
            CounsellingAssignment newCa = new CounsellingAssignment(counsellor, student, "ACTIVE");
            counsellingAssignmentRepository.save(newCa);
        }
    }

    private void authorizeCounsellorManagement(Responsibility r, UserPrincipal caller) {
        if (caller == null) {
            throw new AccessDeniedException("Authentication required");
        }
        Role role = caller.getRole();
        if (role == Role.ADMIN || role == Role.PRINCIPAL) {
            return;
        }
        if (role == Role.HOD || caller.isHod()) {
            com.crre.academicdms.features.academic.model.Department hodDept = hodService.resolveHodDepartment(caller);
            if (r.getDepartment() != null && r.getDepartment().getId().equals(hodDept.getId())) {
                return;
            }
            throw new AccessDeniedException("HOD can only manage counsellor responsibilities within their department");
        }
        throw new AccessDeniedException("You are not authorized to manage counsellor assignments");
    }

    private CounsellorStudentDto mapToCounsellorStudentDto(CounsellingAssignment a) {
        Student s = a.getStudent();
        List<StudentEnrollment> enrollments = enrollmentRepository.findByStudentId(s.getId());
        Section sec = enrollments.isEmpty() ? null : enrollments.get(0).getSection();
        return new CounsellorStudentDto(
            a.getId(),
            s.getId(),
            s.getRegistrationNumber(),
            s.getFullName(),
            s.getEntryType(),
            sec != null ? sec.getSectionName() : "N/A",
            sec != null && sec.getSemester() != null && sec.getSemester().getProgram() != null ? sec.getSemester().getProgram().getName() : "N/A",
            sec != null && sec.getSemester() != null ? sec.getSemester().getTermName() : "N/A",
            a.getAssignedAt(),
            a.getStatus()
        );
    }
}
