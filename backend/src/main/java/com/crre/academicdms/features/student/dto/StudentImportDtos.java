package com.crre.academicdms.features.student.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class StudentImportDtos {

    public record OfficialStudentRecord(
        String registrationNumber,
        String fullName,
        String branchCode,
        String entryType,
        String admissionCategory,
        LocalDate admissionDate,
        String status,
        LocalDate dateOfBirth,
        String gender,
        String entranceExamName,
        String entranceHallTicket,
        Integer entranceRank,
        String sscSchoolName,
        String sscHallTicket,
        String sscBoard,
        Integer sscCompletionYear,
        String interDipCollegeName,
        String interDipHallTicket,
        BigDecimal interDipPercentage,
        String courseGroup,
        String sheetName,
        int sourceRow
    ) {}

    public record SectionCountDto(
        int sectionNumber,
        String sectionName,
        int regularCount,
        int lateralCount,
        int totalCount,
        String notes
    ) {}

    public record BranchSummaryDto(
        String programCode,
        String programName,
        int regularCount,
        int lateralCount,
        int totalCount,
        int impliedSections,
        boolean supportedInDatabase,
        List<SectionCountDto> sectionBreakdown,
        int unallocatedRegular,
        int unallocatedLateral,
        List<String> anomalies
    ) {}

    public record AnomalyRecord(
        String category, // DUPLICATE_REGISTRATION, MISSING_REGISTRATION, CANCELLED_ADMISSION, UNALLOCATED_CAPACITY
        String sheetName,
        int sourceRow,
        String registrationNumber,
        String studentName,
        String details
    ) {}

    public record ImportPreviewResponse(
        int totalParsed,
        int totalRegular,
        int totalLateral,
        int createdCount,
        int updatedCount,
        int unchangedCount,
        int skippedCount,
        int unmatchedSyntheticCount,
        int enrolledCount,
        int unallocatedCount,
        List<BranchSummaryDto> branchSummaries,
        List<AnomalyRecord> humanVerificationCandidates,
        List<String> unmatchedSyntheticRegistrationNumbers,
        List<String> warnings
    ) {}

    public record ImportExecuteResponse(
        boolean dryRun,
        boolean success,
        String message,
        ImportPreviewResponse summary
    ) {}

    public record ImportRequest(
        String regularFilePath,
        String lateralFilePath,
        boolean dryRun,
        boolean scopedOnlyToConfiguredBranches
    ) {}
}
