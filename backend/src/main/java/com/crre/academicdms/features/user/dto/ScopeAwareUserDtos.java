package com.crre.academicdms.features.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class ScopeAwareUserDtos {

    public record CreateEducationalHistoryRequest(
        @NotBlank(message = "Qualification type is required") String qualificationType,
        @NotBlank(message = "Board or university is required") String boardOrUniversity,
        @NotBlank(message = "Institution name is required") String institutionName,
        @NotBlank(message = "Hall ticket number is required") String hallTicketNumber,
        int completionYear,
        String stream,
        String branch,
        BigDecimal maximumMarks,
        BigDecimal marksObtained,
        BigDecimal percentage,
        String additionalDetails
    ) {}

    public record CreateStudentUserRequest(
        @NotBlank(message = "Username is required") String username,
        @NotBlank(message = "Email is required") @Email String email,
        @NotBlank(message = "Full name is required") String fullName,
        @NotBlank(message = "Password is required") String password,
        @NotBlank(message = "Registration number is required") String registrationNumber,
        String uniqueId,
        Long aadharNumber,
        Long apaarNumber,
        String entryType, // REGULAR, LATERAL
        @NotNull(message = "Department ID is required") UUID departmentId,
        @NotNull(message = "Program ID is required") UUID programId,
        @NotNull(message = "Academic Year ID is required") UUID academicYearId,
        @NotNull(message = "Semester ID is required") UUID semesterId,
        @NotNull(message = "Section ID is required") UUID sectionId,
        List<CreateEducationalHistoryRequest> educationalHistory
    ) {}

    public record TeachingAssignmentInput(
        @NotNull(message = "Subject ID is required") UUID subjectId,
        @NotNull(message = "Section ID is required") UUID sectionId
    ) {}

    public record CreateFacultyUserRequest(
        @NotBlank(message = "Username is required") String username,
        @NotBlank(message = "Email is required") @Email String email,
        @NotBlank(message = "Full name is required") String fullName,
        @NotBlank(message = "Password is required") String password,
        @NotBlank(message = "Employee number is required") String employeeNumber,
        @NotNull(message = "Department ID is required") UUID departmentId,
        @NotBlank(message = "Designation is required") String designation,
        @NotBlank(message = "Qualification is required") String qualification,
        String specialization,
        String professionalExperience,
        String institutionalContact,
        List<TeachingAssignmentInput> teachingAssignments,
        UUID classTeacherSectionId,
        boolean isCounsellor
    ) {}
}
