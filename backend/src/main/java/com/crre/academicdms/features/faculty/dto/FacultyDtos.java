package com.crre.academicdms.features.faculty.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public class FacultyDtos {

    public record FacultyProfileDto(
        UUID id,
        UUID userId,
        String fullName,
        String email,
        String employeeNumber,
        UUID departmentId,
        String departmentName,
        String designation,
        String qualification,
        String specialization,
        String professionalExperience,
        String institutionalContact,
        boolean activeStatus,
        List<String> activeRoles
    ) {}

    public record MySubjectDto(
        UUID subjectId,
        String code,
        String name,
        String type,
        BigDecimal credits,
        UUID sectionId,
        String sectionName,
        String programName,
        String semesterName
    ) {}

    public record MyClassDto(
        UUID sectionId,
        String sectionName,
        String programName,
        String semesterName,
        String subjectCode,
        String subjectName,
        long enrolledStudentCount
    ) {}

    public record FacultyTimetableEntryDto(
        UUID id,
        String dayOfWeek,
        int periodNumber,
        LocalTime startTime,
        LocalTime endTime,
        String subjectCode,
        String subjectName,
        String sectionName,
        String programName,
        String semesterName,
        String roomNumber
    ) {}

    public record MySectionOverviewDto(
        UUID sectionId,
        String sectionName,
        String programName,
        String semesterName,
        String academicYearName,
        String classTeacherName,
        long totalStudents,
        long regularStudents,
        long lateralStudents
    ) {}

    public record CounsellingStudentDto(
        UUID studentId,
        String registrationNumber,
        String fullName,
        String entryType,
        String sectionName,
        String programName,
        String semesterName,
        int totalCounsellingSessions,
        LocalDate lastMeetingDate,
        String lastMeetingStatus
    ) {}

    public record CounsellingRecordDto(
        UUID id,
        UUID studentId,
        String studentRegistrationNumber,
        String studentFullName,
        LocalDate meetingDate,
        String recordType,
        String remarks,
        String actionPlan,
        LocalDate followUpDate,
        String status
    ) {}

    public record CreateCounsellingRecordRequest(
        UUID studentId,
        LocalDate meetingDate,
        String recordType,
        String remarks,
        String actionPlan,
        LocalDate followUpDate,
        String status
    ) {}

    public record SectionStudentsGroupDto(
        UUID sectionId,
        String sectionName,
        UUID subjectId,
        String subjectCode,
        String subjectName,
        UUID programId,
        String programCode,
        String programName,
        UUID semesterId,
        String semesterName,
        int studentCount,
        List<com.crre.academicdms.features.academic.dto.AcademicDtos.StudentDto> students
    ) {}
}
