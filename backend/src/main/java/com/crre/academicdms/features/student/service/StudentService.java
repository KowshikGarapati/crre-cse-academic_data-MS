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

@Service
public class StudentService {

    private final StudentRepository studentRepository;
    private final StudentEnrollmentRepository enrollmentRepository;
    private final StudentEducationalHistoryRepository educationalHistoryRepository;
    private final ResponsibilityRepository responsibilityRepository;
    private final CounsellingAssignmentRepository counsellingAssignmentRepository;
    private final CounsellingRecordRepository counsellingRecordRepository;
    private final TimetableRepository timetableRepository;

    public StudentService(
        StudentRepository studentRepository,
        StudentEnrollmentRepository enrollmentRepository,
        StudentEducationalHistoryRepository educationalHistoryRepository,
        ResponsibilityRepository responsibilityRepository,
        CounsellingAssignmentRepository counsellingAssignmentRepository,
        CounsellingRecordRepository counsellingRecordRepository,
        TimetableRepository timetableRepository
    ) {
        this.studentRepository = studentRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.educationalHistoryRepository = educationalHistoryRepository;
        this.responsibilityRepository = responsibilityRepository;
        this.counsellingAssignmentRepository = counsellingAssignmentRepository;
        this.counsellingRecordRepository = counsellingRecordRepository;
        this.timetableRepository = timetableRepository;
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
            .sorted(Comparator.comparing(TimetableEntry::getDayOfWeek).thenComparingInt(TimetableEntry::getPeriodNumber))
            .map(e -> new TimetableEntryDto(
                e.getId(),
                sec.getId(),
                sec.getSectionName(),
                e.getSubject().getId(),
                e.getSubject().getCode(),
                e.getSubject().getName(),
                e.getFacultyUser() != null ? e.getFacultyUser().getFullName() : "Staff",
                e.getDayOfWeek(),
                e.getPeriodNumber(),
                e.getStartTime().toString(),
                e.getEndTime().toString()
            ))
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
}
