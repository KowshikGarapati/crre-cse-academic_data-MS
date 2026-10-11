package com.crre.academicdms.features.student.dto;

import com.crre.academicdms.features.academic.dto.AcademicDtos.TimetableEntryDto;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class StudentDtos {

    public record EducationalHistoryDto(
        UUID id,
        String qualificationType,
        String boardOrUniversity,
        String institutionName,
        String hallTicketNumber,
        int completionYear,
        String stream,
        String branch,
        BigDecimal maximumMarks,
        BigDecimal marksObtained,
        BigDecimal percentage,
        String additionalDetails
    ) {}

    public record StudentPlacementDto(
        UUID departmentId,
        String departmentName,
        UUID programId,
        String programName,
        String degree,
        UUID academicYearId,
        String academicYearName,
        int yearNumber,
        UUID semesterId,
        String semesterName,
        int semesterNumber,
        UUID sectionId,
        String sectionName
    ) {}

    public record StudentProfileDetailsDto(
        UUID id,
        UUID userId,
        String registrationNumber,
        String fullName,
        String email,
        String uniqueId,
        Long aadharNumber,
        Long apaarNumber,
        String entryType,
        String status,
        String admissionCategory,
        String tuitionCategory,
        StudentPlacementDto placement,
        String classTeacherName,
        String classTeacherEmail,
        String counsellorName,
        String counsellorEmail,
        List<EducationalHistoryDto> educationalHistory
    ) {}

    public record StudentCounsellingSummaryDto(
        String counsellorName,
        String counsellorEmail,
        String counsellorEmployeeNumber,
        List<StudentCounsellingSessionDto> sessions
    ) {}

    public record StudentCounsellingSessionDto(
        UUID id,
        LocalDate meetingDate,
        String recordType,
        String remarks,
        String actionPlan,
        LocalDate followUpDate,
        String status
    ) {}

    public record StudentCourseRegistrationDto(
        UUID id,
        String code,
        String name,
        String type,
        BigDecimal credits,
        String facultyName,
        String facultyEmail
    ) {}

    public record StudentDetailDto(
        UUID id,
        UUID userId,
        String registrationNumber,
        String fullName,
        String email,
        String uniqueId,
        Long aadharNumber,
        Long apaarNumber,
        String entryType,
        String status,
        String admissionCategory,
        String tuitionCategory,
        StudentPlacementDto placement,
        String classTeacherName,
        String classTeacherEmail,
        String counsellorName,
        String counsellorEmail,
        List<EducationalHistoryDto> educationalHistory,
        List<StudentCourseRegistrationDto> currentCourses,
        List<TimetableEntryDto> timetable,
        boolean canViewCounselling,
        List<StudentCounsellingSessionDto> counsellingRecords,
        String attendanceStatus,
        String resultsStatus
    ) {}
}
