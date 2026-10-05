package com.crre.academicdms.features.user.service;

import com.crre.academicdms.features.academic.model.Department;
import com.crre.academicdms.features.academic.model.Section;
import com.crre.academicdms.features.academic.repository.DepartmentRepository;
import com.crre.academicdms.features.academic.repository.SectionRepository;
import com.crre.academicdms.features.academic.service.HodService;
import com.crre.academicdms.features.auth.dto.UserSummaryDto;
import com.crre.academicdms.features.faculty.model.FacultyProfile;
import com.crre.academicdms.features.faculty.repository.FacultyProfileRepository;
import com.crre.academicdms.features.responsibility.dto.ResponsibilityRequest;
import com.crre.academicdms.features.responsibility.dto.ResponsibilitySummaryDto;
import com.crre.academicdms.features.responsibility.service.ResponsibilityService;
import com.crre.academicdms.features.student.model.Student;
import com.crre.academicdms.features.student.model.StudentEducationalHistory;
import com.crre.academicdms.features.student.model.StudentEnrollment;
import com.crre.academicdms.features.student.repository.StudentEducationalHistoryRepository;
import com.crre.academicdms.features.student.repository.StudentEnrollmentRepository;
import com.crre.academicdms.features.student.repository.StudentRepository;
import com.crre.academicdms.features.user.dto.CreateUserRequest;
import com.crre.academicdms.features.user.dto.ScopeAwareUserDtos.*;
import com.crre.academicdms.features.user.dto.UpdateUserRequest;
import com.crre.academicdms.features.user.model.Role;
import com.crre.academicdms.features.user.model.User;
import com.crre.academicdms.features.user.repository.UserRepository;
import com.crre.academicdms.security.UserPrincipal;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final ResponsibilityService responsibilityService;
    private final PasswordEncoder passwordEncoder;
    private final HodService hodService;
    private final DepartmentRepository departmentRepository;
    private final SectionRepository sectionRepository;
    private final StudentRepository studentRepository;
    private final StudentEnrollmentRepository studentEnrollmentRepository;
    private final StudentEducationalHistoryRepository educationalHistoryRepository;
    private final FacultyProfileRepository facultyProfileRepository;

    public UserService(
        UserRepository userRepository,
        ResponsibilityService responsibilityService,
        PasswordEncoder passwordEncoder,
        HodService hodService,
        DepartmentRepository departmentRepository,
        SectionRepository sectionRepository,
        StudentRepository studentRepository,
        StudentEnrollmentRepository studentEnrollmentRepository,
        StudentEducationalHistoryRepository educationalHistoryRepository,
        FacultyProfileRepository facultyProfileRepository
    ) {
        this.userRepository = userRepository;
        this.responsibilityService = responsibilityService;
        this.passwordEncoder = passwordEncoder;
        this.hodService = hodService;
        this.departmentRepository = departmentRepository;
        this.sectionRepository = sectionRepository;
        this.studentRepository = studentRepository;
        this.studentEnrollmentRepository = studentEnrollmentRepository;
        this.educationalHistoryRepository = educationalHistoryRepository;
        this.facultyProfileRepository = facultyProfileRepository;
    }

    @Transactional(readOnly = true)
    public List<UserSummaryDto> getAllUsers(UserPrincipal caller) {
        enforceAdminOrHod(caller);

        List<User> users;
        if (caller.getRole() == Role.HOD) {
            Department hodDept = hodService.resolveHodDepartment(caller);
            // HOD sees users belonging to or assigned to their department
            users = userRepository.findAll().stream()
                .filter(u -> {
                    if (u.getRole() == Role.PRINCIPAL || u.getRole() == Role.ADMIN) return false;
                    return true;
                })
                .toList();
        } else {
            users = userRepository.findAll();
        }

        return users.stream()
            .map(u -> {
                List<ResponsibilitySummaryDto> resps = responsibilityService.getActiveResponsibilitiesForUser(u.getId());
                return UserSummaryDto.fromEntity(u, resps);
            })
            .toList();
    }

    @Transactional
    public UserSummaryDto createStudentUser(CreateStudentUserRequest req, UserPrincipal caller) {
        enforceAdminOrHod(caller);

        // Validate scope
        if (caller.getRole() == Role.HOD) {
            Department hodDept = hodService.resolveHodDepartment(caller);
            if (!req.departmentId().equals(hodDept.getId())) {
                throw new AccessDeniedException("HOD cannot create students outside their authorized department scope");
            }
        }

        if (userRepository.existsByUsername(req.username())) {
            throw new IllegalArgumentException("Username already exists: " + req.username());
        }
        if (userRepository.existsByEmail(req.email())) {
            throw new IllegalArgumentException("Email already exists: " + req.email());
        }
        if (userRepository.existsByRegistrationNumber(req.registrationNumber())) {
            throw new IllegalArgumentException("Registration number already exists: " + req.registrationNumber());
        }

        String rawPassword = req.password() != null ? req.password().trim() : "";
        if (rawPassword.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be blank");
        }

        // 1. Create User
        User user = new User(
            req.username().trim(),
            req.email().trim(),
            passwordEncoder.encode(rawPassword),
            req.fullName().trim(),
            Role.STUDENT
        );
        user.setRegistrationNumber(req.registrationNumber().trim());
        user.setUniqueId(req.uniqueId() != null ? req.uniqueId().trim() : null);
        user.setAadharNumber(req.aadharNumber());
        user.setApaarNumber(req.apaarNumber());
        user.setEnabled(true);
        User savedUser = userRepository.save(user);

        // 2. Create Student
        Student student = new Student(
            req.registrationNumber().trim(),
            req.fullName().trim(),
            req.entryType() != null ? req.entryType().toUpperCase() : "REGULAR"
        );
        student.setUser(savedUser);
        Student savedStudent = studentRepository.save(student);

        // 3. Create StudentEnrollment
        Section section = sectionRepository.findById(req.sectionId())
            .orElseThrow(() -> new IllegalArgumentException("Section not found"));
        StudentEnrollment enrollment = new StudentEnrollment(savedStudent, section, true);
        studentEnrollmentRepository.save(enrollment);

        // 4. Save Educational History if provided
        if (req.educationalHistory() != null) {
            for (CreateEducationalHistoryRequest edu : req.educationalHistory()) {
                StudentEducationalHistory history = new StudentEducationalHistory(
                    savedStudent,
                    edu.qualificationType(),
                    edu.boardOrUniversity(),
                    edu.institutionName(),
                    edu.hallTicketNumber(),
                    edu.completionYear(),
                    edu.stream(),
                    edu.branch(),
                    edu.maximumMarks(),
                    edu.marksObtained(),
                    edu.percentage(),
                    edu.additionalDetails()
                );
                educationalHistoryRepository.save(history);
            }
        }

        return UserSummaryDto.fromEntity(savedUser, List.of());
    }

    @Transactional
    public UserSummaryDto createFacultyUser(CreateFacultyUserRequest req, UserPrincipal caller) {
        enforceAdminOrHod(caller);

        // Validate scope
        if (caller.getRole() == Role.HOD) {
            Department hodDept = hodService.resolveHodDepartment(caller);
            if (!req.departmentId().equals(hodDept.getId())) {
                throw new AccessDeniedException("HOD cannot create faculty outside their authorized department scope");
            }
        }

        if (userRepository.existsByUsername(req.username())) {
            throw new IllegalArgumentException("Username already exists: " + req.username());
        }
        if (userRepository.existsByEmail(req.email())) {
            throw new IllegalArgumentException("Email already exists: " + req.email());
        }
        if (facultyProfileRepository.findByEmployeeNumber(req.employeeNumber()).isPresent()) {
            throw new IllegalArgumentException("Employee number already exists: " + req.employeeNumber());
        }

        Department dept = departmentRepository.findById(req.departmentId())
            .orElseThrow(() -> new IllegalArgumentException("Department not found"));

        String rawPassword = req.password() != null ? req.password().trim() : "";
        if (rawPassword.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be blank");
        }

        // 1. Create User (Role = FACULTY)
        User user = new User(
            req.username().trim(),
            req.email().trim(),
            passwordEncoder.encode(rawPassword),
            req.fullName().trim(),
            Role.FACULTY
        );
        user.setUniqueId(req.employeeNumber().trim());
        user.setEnabled(true);
        User savedUser = userRepository.save(user);

        // 2. Create FacultyProfile (NO PROGRAM FIELD!)
        FacultyProfile profile = new FacultyProfile(
            savedUser,
            req.employeeNumber().trim(),
            dept,
            req.designation().trim(),
            req.qualification().trim(),
            req.specialization(),
            req.professionalExperience(),
            req.institutionalContact()
        );
        facultyProfileRepository.save(profile);

        // 3. Assign Teaching Responsibilities
        if (req.teachingAssignments() != null) {
            for (TeachingAssignmentInput ta : req.teachingAssignments()) {
                responsibilityService.addResponsibility(
                    savedUser.getId(),
                    new ResponsibilityRequest("TEACHING", dept.getId(), null, ta.subjectId(), ta.sectionId(), "Teaching Assignment")
                );
            }
        }

        // 4. Assign Class Teacher if specified
        if (req.classTeacherSectionId() != null) {
            responsibilityService.addResponsibility(
                savedUser.getId(),
                new ResponsibilityRequest("CLASS_TEACHER", dept.getId(), null, null, req.classTeacherSectionId(), "Class Teacher Assignment")
            );
        }

        // 5. Assign Counsellor if specified
        if (req.isCounsellor()) {
            responsibilityService.addResponsibility(
                savedUser.getId(),
                new ResponsibilityRequest("COUNSELLOR", dept.getId(), null, null, null, "Counsellor Assignment")
            );
        }

        List<ResponsibilitySummaryDto> resps = responsibilityService.getActiveResponsibilitiesForUser(savedUser.getId());
        return UserSummaryDto.fromEntity(savedUser, resps);
    }

    @Transactional
    public UserSummaryDto createUser(CreateUserRequest req, UserPrincipal caller) {
        enforceDownwardAuthority(caller, req.role());

        if (userRepository.existsByUsername(req.username())) {
            throw new IllegalArgumentException("Username already exists: " + req.username());
        }
        if (userRepository.existsByEmail(req.email())) {
            throw new IllegalArgumentException("Email already exists: " + req.email());
        }

        String rawPassword = req.password() != null ? req.password().trim() : "";
        if (rawPassword.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be blank");
        }

        User user = new User(
            req.username().trim(),
            req.email().trim(),
            passwordEncoder.encode(rawPassword),
            req.fullName().trim(),
            req.role()
        );
        user.setRegistrationNumber(req.registrationNumber());
        user.setUniqueId(req.uniqueId());
        user.setAadharNumber(req.aadharNumber());
        user.setApaarNumber(req.apaarNumber());
        user.setEnabled(true);

        User savedUser = userRepository.save(user);

        // Process initial role-specific assignments if provided
        if (req.role() == Role.FACULTY) {
            if (req.subjectId() != null && req.sectionId() != null) {
                responsibilityService.addResponsibility(
                    savedUser.getId(),
                    new ResponsibilityRequest("TEACHING", req.departmentId(), req.programId(), req.subjectId(), req.sectionId(), null)
                );
            }
            if (req.classTeacher() && req.sectionId() != null) {
                responsibilityService.addResponsibility(
                    savedUser.getId(),
                    new ResponsibilityRequest("CLASS_TEACHER", req.departmentId(), req.programId(), null, req.sectionId(), null)
                );
            }
        } else if (req.role() == Role.HOD && req.departmentId() != null) {
            responsibilityService.addResponsibility(
                savedUser.getId(),
                new ResponsibilityRequest("HOD_DEPT", req.departmentId(), req.programId(), null, null, null)
            );
        } else if (req.role() == Role.COUNSELLOR && req.sectionId() != null) {
            responsibilityService.addResponsibility(
                savedUser.getId(),
                new ResponsibilityRequest("COUNSELLOR", req.departmentId(), req.programId(), null, req.sectionId(), null)
            );
        } else if (req.role() == Role.CLASS_TEACHER && req.sectionId() != null) {
            responsibilityService.addResponsibility(
                savedUser.getId(),
                new ResponsibilityRequest("CLASS_TEACHER", req.departmentId(), req.programId(), null, req.sectionId(), null)
            );
        } else if (req.role() == Role.PRINCIPAL && req.departmentId() != null) {
            responsibilityService.addResponsibility(
                savedUser.getId(),
                new ResponsibilityRequest("PRINCIPAL_INSTITUTION", req.departmentId(), null, null, null, null)
            );
        }

        List<ResponsibilitySummaryDto> resps = responsibilityService.getActiveResponsibilitiesForUser(savedUser.getId());
        return UserSummaryDto.fromEntity(savedUser, resps);
    }

    @Transactional
    public UserSummaryDto updateUser(UUID userId, UpdateUserRequest req, UserPrincipal caller) {
        enforceDownwardAuthority(caller, req.role());

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

        user.setFullName(req.fullName().trim());
        user.setEmail(req.email().trim());
        user.setRole(req.role());
        user.setRegistrationNumber(req.registrationNumber());
        user.setUniqueId(req.uniqueId());
        user.setAadharNumber(req.aadharNumber());
        user.setApaarNumber(req.apaarNumber());
        user.setEnabled(req.enabled());
        if (req.password() != null && !req.password().trim().isEmpty()) {
            user.setPasswordHash(passwordEncoder.encode(req.password().trim()));
        }

        User saved = userRepository.save(user);
        List<ResponsibilitySummaryDto> resps = responsibilityService.getActiveResponsibilitiesForUser(saved.getId());
        return UserSummaryDto.fromEntity(saved, resps);
    }

    @Transactional
    public void toggleUserStatus(UUID userId, boolean enabled, UserPrincipal caller) {
        enforceAdminOrHod(caller);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

        if (caller.getRole() == Role.HOD && (user.getRole() == Role.PRINCIPAL || user.getRole() == Role.ADMIN)) {
            throw new AccessDeniedException("HOD cannot modify a Principal or Admin account");
        }

        user.setEnabled(enabled);
        userRepository.save(user);
    }

    private void enforceAdminOrHod(UserPrincipal caller) {
        if (caller == null) {
            throw new AccessDeniedException("Authentication required");
        }
        Role role = caller.getRole();
        if (role != Role.ADMIN && role != Role.PRINCIPAL && role != Role.HOD) {
            throw new AccessDeniedException("Access denied: You do not have administrative authority");
        }
    }

    private void enforceDownwardAuthority(UserPrincipal caller, Role targetRole) {
        enforceAdminOrHod(caller);
        if (caller.getRole() == Role.HOD) {
            if (targetRole == Role.PRINCIPAL || targetRole == Role.ADMIN) {
                throw new AccessDeniedException("HOD cannot create or assign a Principal or Admin");
            }
        }
    }
}
