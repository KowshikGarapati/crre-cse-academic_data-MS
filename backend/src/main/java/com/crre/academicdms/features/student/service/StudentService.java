package com.crre.academicdms.features.student.service;

import com.crre.academicdms.features.academic.dto.AcademicDtos.TimetableEntryDto;
import com.crre.academicdms.features.academic.model.Section;
import com.crre.academicdms.features.academic.model.TimetableEntry;
import com.crre.academicdms.features.academic.repository.TimetableRepository;
import com.crre.academicdms.features.counselling.model.CounsellingAssignment;
import com.crre.academicdms.features.counselling.model.CounsellingRecord;
import com.crre.academicdms.features.counselling.repository.CounsellingAssignmentRepository;
import com.crre.academicdms.features.counselling.repository.CounsellingRecordRepository;
import com.crre.academicdms.features.responsibility.model.Responsibility;
import com.crre.academicdms.features.responsibility.repository.ResponsibilityRepository;
import com.crre.academicdms.features.student.dto.StudentDtos.*;
import com.crre.academicdms.features.student.model.Student;
import com.crre.academicdms.features.student.model.StudentEducationalHistory;
import com.crre.academicdms.features.student.model.StudentEnrollment;
import com.crre.academicdms.features.student.repository.StudentEducationalHistoryRepository;
import com.crre.academicdms.features.student.repository.StudentEnrollmentRepository;
import com.crre.academicdms.features.student.repository.StudentRepository;
import com.crre.academicdms.features.user.model.User;
import com.crre.academicdms.security.UserPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class StudentService {

    private final StudentRepository studentRepository;
    private final StudentEnrollmentRepository enrollmentRepository;
    private final StudentEducationalHistoryRepository educationalHistoryRepository;
    private final ResponsibilityRepository responsibilityRepository;
    private final CounsellingAssignmentRepository counsellingAssignmentRepository;
    private final CounsellingRecordRepository counsellingRecordRepository;
    private final TimetableRepository timetableRepository;
    private final com.crre.academicdms.features.academic.repository.SubjectRepository subjectRepository;
    private final com.crre.academicdms.features.academic.service.HodService hodService;
    private final StudentScopeService studentScopeService;

    public StudentService(
        StudentRepository studentRepository,
        StudentEnrollmentRepository enrollmentRepository,
        StudentEducationalHistoryRepository educationalHistoryRepository,
        ResponsibilityRepository responsibilityRepository,
        CounsellingAssignmentRepository counsellingAssignmentRepository,
        CounsellingRecordRepository counsellingRecordRepository,
        TimetableRepository timetableRepository,
        com.crre.academicdms.features.academic.repository.SubjectRepository subjectRepository,
        com.crre.academicdms.features.academic.service.HodService hodService,
        StudentScopeService studentScopeService
    ) {
        this.studentRepository = studentRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.educationalHistoryRepository = educationalHistoryRepository;
        this.responsibilityRepository = responsibilityRepository;
        this.counsellingAssignmentRepository = counsellingAssignmentRepository;
        this.counsellingRecordRepository = counsellingRecordRepository;
        this.timetableRepository = timetableRepository;
        this.subjectRepository = subjectRepository;
        this.hodService = hodService;
        this.studentScopeService = studentScopeService;
    }

    @Transactional(readOnly = true)
    public StudentProfileDetailsDto getMyProfile(UserPrincipal caller) {
        Student student = studentRepository.findByUserId(caller.getId())
            .orElseGet(() -> studentRepository.findByRegistrationNumber(caller.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Student record not found for user: " + caller.getUsername())));

        List<StudentEnrollment> enrollments = enrollmentRepository.findByStudentId(student.getId());
        Section sec = enrollments.isEmpty() ? null : enrollments.get(0).getSection();

        StudentPlacementDto placement = null;
        String classTeacherName = null;
        String classTeacherEmail = null;

        if (sec != null) {
            placement = new StudentPlacementDto(
                sec.getSemester().getProgram().getDepartment().getId(),
                sec.getSemester().getProgram().getDepartment().getName(),
                sec.getSemester().getProgram().getId(),
                sec.getSemester().getProgram().getName(),
                sec.getSemester().getProgram().getDegree(),
                sec.getSemester().getAcademicYear().getId(),
                sec.getSemester().getAcademicYear().getYearName(),
                sec.getSemester().getAcademicYear().getYearNumber(),
                sec.getSemester().getId(),
                sec.getSemester().getTermName(),
                sec.getSemester().getSemesterNumber(),
                sec.getId(),
                sec.getSectionName()
            );

            // Find Class Teacher
            List<Responsibility> ctList = responsibilityRepository.findByTypeAndStatus(
                "CLASS_TEACHER", "ACTIVE"
            );
            Optional<Responsibility> ctResp = ctList.stream()
                .filter(r -> r.getSection() != null && r.getSection().getId().equals(sec.getId()))
                .findFirst();

            if (ctResp.isPresent()) {
                classTeacherName = ctResp.get().getUser().getFullName();
                classTeacherEmail = ctResp.get().getUser().getEmail();
            }
        }

        // Find Assigned Counsellor
        String counsellorName = null;
        String counsellorEmail = null;
        List<CounsellingAssignment> cList = counsellingAssignmentRepository.findByStudentIdAndStatus(student.getId(), "ACTIVE");
        if (!cList.isEmpty()) {
            User cUser = cList.get(0).getCounsellor();
            counsellorName = cUser.getFullName();
            counsellorEmail = cUser.getEmail();
        }

        // Educational History
        List<StudentEducationalHistory> eduList = educationalHistoryRepository.findByStudentIdOrderByCompletionYearAsc(student.getId());
        List<EducationalHistoryDto> eduDtos = eduList.stream()
            .map(e -> new EducationalHistoryDto(
                e.getId(),
                e.getQualificationType(),
                e.getBoardOrUniversity(),
                e.getInstitutionName(),
                e.getHallTicketNumber(),
                e.getCompletionYear(),
                e.getStream(),
                e.getBranch(),
                e.getMaximumMarks(),
                e.getMarksObtained(),
                e.getPercentage(),
                e.getAdditionalDetails()
            ))
            .toList();

        return new StudentProfileDetailsDto(
            student.getId(),
            caller.getId(),
            student.getRegistrationNumber(),
            student.getFullName(),
            caller.getEmail(),
            caller.getUniqueId(),
            caller.getAadharNumber(),
            caller.getApaarNumber(),
            student.getEntryType(),
            student.getStatus(),
            student.getAdmissionCategory(),
            student.getTuitionCategory(),
            placement,
            classTeacherName,
            classTeacherEmail,
            counsellorName,
            counsellorEmail,
            eduDtos
        );
    }

    @Transactional(readOnly = true)
    public List<TimetableEntryDto> getMyTimetable(UserPrincipal caller) {
        Student student = studentRepository.findByUserId(caller.getId())
            .orElseGet(() -> studentRepository.findByRegistrationNumber(caller.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Student record not found")));

        List<StudentEnrollment> enrollments = enrollmentRepository.findByStudentId(student.getId());
        if (enrollments.isEmpty()) {
            return List.of();
        }

        Section sec = enrollments.get(0).getSection();
        List<TimetableEntry> entries = timetableRepository.findBySectionIdOrderByDayOfWeekAscPeriodNumberAsc(sec.getId());

        return entries.stream()
            .sorted(com.crre.academicdms.features.academic.util.TimetableUtils.CHRONOLOGICAL_COMPARATOR)
            .map(com.crre.academicdms.features.academic.dto.AcademicDtos.TimetableEntryDto::fromEntity)
            .toList();
    }

    @Transactional(readOnly = true)
    public StudentCounsellingSummaryDto getMyCounselling(UserPrincipal caller) {
        Student student = studentRepository.findByUserId(caller.getId())
            .orElseGet(() -> studentRepository.findByRegistrationNumber(caller.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Student record not found")));

        List<CounsellingAssignment> assignments = counsellingAssignmentRepository.findByStudentIdAndStatus(student.getId(), "ACTIVE");
        if (assignments.isEmpty()) {
            return new StudentCounsellingSummaryDto(null, null, null, List.of());
        }

        User counsellor = assignments.get(0).getCounsellor();
        List<CounsellingRecord> records = counsellingRecordRepository.findByStudentIdOrderByMeetingDateDesc(student.getId());

        List<StudentCounsellingSessionDto> sessions = records.stream()
            .map(r -> new StudentCounsellingSessionDto(
                r.getId(),
                r.getMeetingDate(),
                r.getRecordType(),
                r.getRemarks(),
                r.getActionPlan(),
                r.getFollowUpDate(),
                r.getStatus()
            ))
            .toList();

        return new StudentCounsellingSummaryDto(
            counsellor.getFullName(),
            counsellor.getEmail(),
            counsellor.getUniqueId(),
            sessions
        );
    }

    @Transactional(readOnly = true)
    public List<com.crre.academicdms.features.academic.dto.AcademicDtos.StudentDto> getScopedStudents(UserPrincipal caller) {
        return studentScopeService.getAccessibleStudents(caller);
    }

    @Transactional(readOnly = true)
    public List<com.crre.academicdms.features.academic.dto.AcademicDtos.StudentDto> getScopedStudents(UUID sectionId, UserPrincipal caller) {
        return studentScopeService.getAccessibleStudents(sectionId, caller);
    }

    @Transactional(readOnly = true)
    public StudentDetailDto getStudentDetails(UUID studentId, UserPrincipal caller) {
        return getStudentDetails(studentId.toString(), caller);
    }

    @Transactional(readOnly = true)
    public StudentDetailDto getStudentDetails(String identifier, UserPrincipal caller) {
        if (caller == null) {
            throw new org.springframework.security.access.AccessDeniedException("Authentication required");
        }

        // 1. Authoritative resolution & scope verification via StudentScopeService
        Student student = studentScopeService.resolveStudent(identifier);
        if (!studentScopeService.isStudentAccessible(student, caller)) {
            throw new org.springframework.security.access.AccessDeniedException("Access denied: You are not authorized to view this student");
        }

        boolean canViewCounselling = studentScopeService.canViewStudentCounselling(student, caller);

        List<StudentEnrollment> enrollments = enrollmentRepository.findByStudentId(student.getId());
        Section sec = enrollments.isEmpty() ? null : enrollments.get(0).getSection();

        // 3. Academic Placement
        StudentPlacementDto placement = null;
        String classTeacherName = null;
        String classTeacherEmail = null;
        if (sec != null) {
            placement = new StudentPlacementDto(
                sec.getSemester().getProgram().getDepartment().getId(),
                sec.getSemester().getProgram().getDepartment().getName(),
                sec.getSemester().getProgram().getId(),
                sec.getSemester().getProgram().getName(),
                sec.getSemester().getProgram().getDegree(),
                sec.getSemester().getAcademicYear().getId(),
                sec.getSemester().getAcademicYear().getYearName(),
                sec.getSemester().getAcademicYear().getYearNumber(),
                sec.getSemester().getId(),
                sec.getSemester().getTermName(),
                sec.getSemester().getSemesterNumber(),
                sec.getId(),
                sec.getSectionName()
            );

            // Find Class Teacher
            List<Responsibility> ctList = responsibilityRepository.findByTypeAndStatus("CLASS_TEACHER", "ACTIVE");
            Optional<Responsibility> ctResp = ctList.stream()
                .filter(r -> r.getSection() != null && r.getSection().getId().equals(sec.getId()))
                .findFirst();
            if (ctResp.isPresent()) {
                classTeacherName = ctResp.get().getUser().getFullName();
                classTeacherEmail = ctResp.get().getUser().getEmail();
            }
        }

        // Find Assigned Counsellor
        String counsellorName = null;
        String counsellorEmail = null;
        List<CounsellingAssignment> cList = counsellingAssignmentRepository.findByStudentIdAndStatus(student.getId(), "ACTIVE");
        if (!cList.isEmpty()) {
            User cUser = cList.get(0).getCounsellor();
            counsellorName = cUser.getFullName();
            counsellorEmail = cUser.getEmail();
        }

        // Educational History
        List<StudentEducationalHistory> eduList = educationalHistoryRepository.findByStudentIdOrderByCompletionYearAsc(student.getId());
        List<EducationalHistoryDto> eduDtos = eduList.stream()
            .map(e -> new EducationalHistoryDto(
                e.getId(),
                e.getQualificationType(),
                e.getBoardOrUniversity(),
                e.getInstitutionName(),
                e.getHallTicketNumber(),
                e.getCompletionYear(),
                e.getStream(),
                e.getBranch(),
                e.getMaximumMarks(),
                e.getMarksObtained(),
                e.getPercentage(),
                e.getAdditionalDetails()
            ))
            .toList();

        // Current Registered Courses with Faculty Instructors
        List<StudentCourseRegistrationDto> courseDtos = List.of();
        if (sec != null && sec.getSemester() != null) {
            List<com.crre.academicdms.features.academic.model.Subject> subjects = subjectRepository.findBySemesterId(sec.getSemester().getId());
            List<Responsibility> sectionTeaching = responsibilityRepository.findBySectionIdAndStatus(sec.getId(), "ACTIVE").stream()
                .filter(r -> "TEACHING".equals(r.getType()) && r.getSubject() != null)
                .toList();

            courseDtos = subjects.stream()
                .map(sub -> {
                    Optional<Responsibility> match = sectionTeaching.stream()
                        .filter(r -> r.getSubject().getId().equals(sub.getId()))
                        .findFirst();
                    String facultyName = match.map(r -> r.getUser().getFullName()).orElse("Faculty Unassigned");
                    String facultyEmail = match.map(r -> r.getUser().getEmail()).orElse(null);
                    return new StudentCourseRegistrationDto(
                        sub.getId(),
                        sub.getCode(),
                        sub.getName(),
                        sub.getType(),
                        sub.getCredits(),
                        facultyName,
                        facultyEmail
                    );
                })
                .sorted(Comparator.comparing(StudentCourseRegistrationDto::code))
                .toList();
        }

        // Section Timetable sorted chronologically
        List<TimetableEntryDto> timetableDtos = List.of();
        if (sec != null) {
            timetableDtos = timetableRepository.findBySectionIdOrderByDayOfWeekAscPeriodNumberAsc(sec.getId()).stream()
                .sorted(com.crre.academicdms.features.academic.util.TimetableUtils.CHRONOLOGICAL_COMPARATOR)
                .map(com.crre.academicdms.features.academic.dto.AcademicDtos.TimetableEntryDto::fromEntity)
                .toList();
        }

        // Sensitive Counselling Records (only populated if authorized)
        List<StudentCounsellingSessionDto> counsellingRecords = List.of();
        if (canViewCounselling) {
            counsellingRecords = counsellingRecordRepository.findByStudentIdOrderByMeetingDateDesc(student.getId()).stream()
                .map(r -> new StudentCounsellingSessionDto(
                    r.getId(),
                    r.getMeetingDate(),
                    r.getRecordType(),
                    r.getRemarks(),
                    r.getActionPlan(),
                    r.getFollowUpDate(),
                    r.getStatus()
                ))
                .toList();
        }

        User studentUser = student.getUser();
        UUID userId = studentUser != null ? studentUser.getId() : null;
        String email = studentUser != null ? studentUser.getEmail() : null;
        String uniqueId = studentUser != null ? studentUser.getUniqueId() : null;
        Long aadhar = studentUser != null ? studentUser.getAadharNumber() : null;
        Long apaar = studentUser != null ? studentUser.getApaarNumber() : null;

        return new StudentDetailDto(
            student.getId(),
            userId,
            student.getRegistrationNumber(),
            student.getFullName(),
            email,
            uniqueId,
            aadhar,
            apaar,
            student.getEntryType(),
            student.getStatus(),
            student.getAdmissionCategory(),
            student.getTuitionCategory(),
            placement,
            classTeacherName,
            classTeacherEmail,
            counsellorName,
            counsellorEmail,
            eduDtos,
            courseDtos,
            timetableDtos,
            canViewCounselling,
            counsellingRecords,
            "Attendance data not available yet.",
            "No academic results recorded."
        );
    }
}
