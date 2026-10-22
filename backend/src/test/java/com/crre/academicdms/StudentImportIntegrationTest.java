package com.crre.academicdms;

import com.crre.academicdms.features.academic.model.Section;
import com.crre.academicdms.features.academic.repository.ProgramRepository;
import com.crre.academicdms.features.academic.repository.SectionRepository;
import com.crre.academicdms.features.academic.repository.SemesterRepository;
import com.crre.academicdms.features.student.dto.StudentImportDtos.*;
import com.crre.academicdms.features.student.model.Student;
import com.crre.academicdms.features.student.model.StudentEducationalHistory;
import com.crre.academicdms.features.student.model.StudentEnrollment;
import com.crre.academicdms.features.student.repository.StudentEducationalHistoryRepository;
import com.crre.academicdms.features.student.repository.StudentEnrollmentRepository;
import com.crre.academicdms.features.student.repository.StudentRepository;
import com.crre.academicdms.features.student.service.StudentImportService;
import com.crre.academicdms.features.student.service.SyntheticDataManagementService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class StudentImportIntegrationTest {

    @Autowired
    private StudentImportService importService;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private StudentEnrollmentRepository enrollmentRepository;

    @Autowired
    private StudentEducationalHistoryRepository educationalHistoryRepository;

    @Autowired
    private SectionRepository sectionRepository;

    @Autowired
    private ProgramRepository programRepository;

    @Autowired
    private SemesterRepository semesterRepository;

    @Autowired
    private SyntheticDataManagementService syntheticDataManagementService;

    private final Path regularPath = Paths.get(StudentImportService.DEFAULT_REGULAR_PATH);
    private final Path lateralPath = Paths.get(StudentImportService.DEFAULT_LATERAL_PATH);

    @Test
    @Order(1)
    @DisplayName("Unit: Verify registration number normalization and validation")
    void testRegistrationNumberNormalizationAndValidation() {
        assertThat(StudentImportService.normalizeRegNo(" 25b81a0501 ")).isEqualTo("25B81A0501");
        assertThat(StudentImportService.normalizeRegNo("25B81A 0502")).isEqualTo("25B81A0502");

        assertThat(StudentImportService.isValidRegistrationNumber("25B81A0501")).isTrue();
        assertThat(StudentImportService.isValidRegistrationNumber("26B85A0501")).isTrue();
        assertThat(StudentImportService.isValidRegistrationNumber("CANCELLED")).isFalse();
        assertThat(StudentImportService.isValidRegistrationNumber("")).isFalse();
        assertThat(StudentImportService.isValidRegistrationNumber(null)).isFalse();
        assertThat(StudentImportService.isValidRegistrationNumber("CSE")).isFalse();
    }

    @Test
    @Order(2)
    @DisplayName("Unit: Verify admission category, status and name parsing")
    void testParsingRules() {
        assertThat(StudentImportService.parseAdmissionCategory("CONVENER")).isEqualTo("CONVENOR");
        assertThat(StudentImportService.parseAdmissionCategory("CONVENOR")).isEqualTo("CONVENOR");
        assertThat(StudentImportService.parseAdmissionCategory("MANAGEMENT QUOTA")).isEqualTo("MANAGEMENT");
        assertThat(StudentImportService.parseAdmissionCategory("SPOT")).isEqualTo("CONVENOR");
        assertThat(StudentImportService.parseAdmissionCategory("ECET")).isEqualTo("CONVENOR");
        assertThat(StudentImportService.parseAdmissionCategory(null)).isEqualTo("CONVENOR");

        assertThat(StudentImportService.parseStatus("JOHN DOE")).isEqualTo("ACTIVE");
        assertThat(StudentImportService.parseStatus("JOHN DOE (CANCELLED)")).isEqualTo("INACTIVE");
        assertThat(StudentImportService.parseStatus("JANE DOE - DISCONTINUED")).isEqualTo("INACTIVE");
        assertThat(StudentImportService.parseStatus("MARK (DETAINED)")).isEqualTo("INACTIVE");

        assertThat(StudentImportService.cleanName("   KUMAR   REDDY   ")).isEqualTo("KUMAR REDDY");
    }

    @Test
    @Order(3)
    @DisplayName("Unit: Verify sheet name to program code mapping")
    void testSheetNameToProgramMapping() {
        assertThat(StudentImportService.mapSheetToProgramCode("CSM")).isEqualTo("AIML");
        assertThat(StudentImportService.mapSheetToProgramCode("CAM")).isEqualTo("AIML");
        assertThat(StudentImportService.mapSheetToProgramCode("CAD")).isEqualTo("AIDS");
        assertThat(StudentImportService.mapSheetToProgramCode("CAD ")).isEqualTo("AIDS");
        assertThat(StudentImportService.mapSheetToProgramCode("CSC")).isEqualTo("CS");
        assertThat(StudentImportService.mapSheetToProgramCode("CSE")).isEqualTo("CSE");
        assertThat(StudentImportService.mapSheetToProgramCode("ECE")).isEqualTo("ECE");
        assertThat(StudentImportService.mapSheetToProgramCode("EEE")).isEqualTo("EEE");
        assertThat(StudentImportService.mapSheetToProgramCode("INF")).isEqualTo("IT");
        assertThat(StudentImportService.mapSheetToProgramCode("IT")).isEqualTo("IT");
        assertThat(StudentImportService.mapSheetToProgramCode("MECH")).isEqualTo("MECH");
        assertThat(StudentImportService.mapSheetToProgramCode("CIV")).isEqualTo("CIVIL");
        assertThat(StudentImportService.mapSheetToProgramCode("CIVIL")).isEqualTo("CIVIL");
    }

    @Test
    @Order(4)
    @DisplayName("Parser: Verify parsing of official workbooks, entrance fields and anomalies")
    void testWorkbookParsingAndAnomalies() {
        var parsed = importService.parseWorkbooks(regularPath, lateralPath);

        assertThat(parsed.validRecords).isNotEmpty();
        // College-wide total: 1,394 (1,216 Regular + 178 Lateral)
        assertThat(parsed.validRecords.size()).isEqualTo(1394);

        // Verify entrance and educational data extracted
        OfficialStudentRecord firstReg = parsed.validRecords.stream()
            .filter(r -> "25B81A4502".equals(r.registrationNumber()))
            .findFirst()
            .orElseThrow();
        assertThat(firstReg.entranceExamName()).isEqualTo("EAPCET");
        assertThat(firstReg.entranceHallTicket()).isEqualTo("550491010025");
        assertThat(firstReg.entranceRank()).isEqualTo(104448);
        assertThat(firstReg.sscSchoolName()).isEqualTo("ZPHS KURUPAM");
        assertThat(firstReg.sscHallTicket()).isEqualTo("2303108237");
        assertThat(firstReg.interDipCollegeName()).isEqualTo("VASU JR.COLLEGE");

        // Verify duplicate registration detection
        assertThat(parsed.duplicateRegs).contains("25B81A1204");
    }

    @Test
    @Order(5)
    @DisplayName("Allocation: Verify 66-regular and 6-lateral allocation rule with overflow")
    void testSectionAllocationRule() {
        var parsed = importService.parseWorkbooks(regularPath, lateralPath);
        List<Section> sections = sectionRepository.findAllWithProgram();

        var allocResult = importService.computeSectionAllocations(parsed.validRecords, sections);

        // CSE has 6 sections in DB
        var cseSummary = allocResult.branchSummaries.stream()
            .filter(b -> "CSE".equals(b.programCode()))
            .findFirst()
            .orElseThrow();

        assertThat(cseSummary.sectionBreakdown()).hasSize(6);

        // Sections 1 through 5: 66 regular + 6 lateral = 72
        for (int i = 0; i < 5; i++) {
            var sec = cseSummary.sectionBreakdown().get(i);
            assertThat(sec.regularCount()).isEqualTo(66);
            assertThat(sec.lateralCount()).isEqualTo(6);
            assertThat(sec.totalCount()).isEqualTo(72);
        }

        // Section 6: 62 regular + 6 lateral = 68 (incomplete regular cohort 62/66)
        var sec6 = cseSummary.sectionBreakdown().get(5);
        assertThat(sec6.regularCount()).isEqualTo(62);
        assertThat(sec6.lateralCount()).isEqualTo(6);
        assertThat(sec6.totalCount()).isEqualTo(68);

        // Lateral overflow: 8 unallocated lateral students exceeding 36 capacity
        assertThat(cseSummary.unallocatedLateral()).isEqualTo(8);

        // AIDS has 2 sections in DB: 66 reg + 6 lat = 72 each
        var aidsSummary = allocResult.branchSummaries.stream()
            .filter(b -> "AIDS".equals(b.programCode()))
            .findFirst()
            .orElseThrow();
        assertThat(aidsSummary.sectionBreakdown()).hasSize(2);
        assertThat(aidsSummary.sectionBreakdown().get(0).totalCount()).isEqualTo(72);
        assertThat(aidsSummary.sectionBreakdown().get(1).totalCount()).isEqualTo(72);

        // AIML has 1 section: 66 regular + 5 lateral = 71 (incomplete lateral cohort 5/6)
        var aimlSummary = allocResult.branchSummaries.stream()
            .filter(b -> "AIML".equals(b.programCode()))
            .findFirst()
            .orElseThrow();
        assertThat(aimlSummary.sectionBreakdown()).hasSize(1);
        assertThat(aimlSummary.sectionBreakdown().get(0).regularCount()).isEqualTo(66);
        assertThat(aimlSummary.sectionBreakdown().get(0).lateralCount()).isEqualTo(5);
        assertThat(aimlSummary.sectionBreakdown().get(0).totalCount()).isEqualTo(71);

        // CS has 1 section: 66 regular + 6 lateral = 72
        var csSummary = allocResult.branchSummaries.stream()
            .filter(b -> "CS".equals(b.programCode()))
            .findFirst()
            .orElseThrow();
        assertThat(csSummary.sectionBreakdown()).hasSize(1);
        assertThat(csSummary.sectionBreakdown().get(0).totalCount()).isEqualTo(72);
    }

    @Test
    @Order(6)
    @DisplayName("DryRun: Verify dry-run preview focuses on 4 programs without modifying DB")
    void testDryRunPreviewDoesNotPersist() {
        long initialStudentCount = studentRepository.count();
        long initialEnrollmentCount = enrollmentRepository.count();

        var preview = importService.preview(regularPath, lateralPath);
        assertThat(preview).isNotNull();
        // 4 programs: 71 (AIML) + 144 (AIDS) + 72 (CS) + 436 (CSE) = 723 scoped records
        assertThat(preview.totalParsed()).isEqualTo(723);

        // Ensure no DB changes occurred during preview
        assertThat(studentRepository.count()).isEqualTo(initialStudentCount);
        assertThat(enrollmentRepository.count()).isEqualTo(initialEnrollmentCount);
    }

    @Test
    @Order(7)
    @DisplayName("Execution & Idempotency: Execute scoped import with educational history and verify isolation")
    void testExecuteImportAndVerifyIdempotency() {
        // Step 1: Execute import
        ImportExecuteResponse response1 = importService.executeImport(regularPath, lateralPath, false);
        assertThat(response1.success()).isTrue();
        assertThat(response1.dryRun()).isFalse();
        assertThat(response1.summary().totalParsed()).isEqualTo(723);

        // Verify educational history was populated for official students
        List<StudentEducationalHistory> allEdu = educationalHistoryRepository.findAll();
        assertThat(allEdu).isNotEmpty();

        // Verify official student attributes: non-synthetic, decoupled from user accounts
        List<Student> officialStudents = studentRepository.findAll().stream()
            .filter(s -> !s.isSynthetic())
            .toList();
        assertThat(officialStudents).isNotEmpty();

        for (Student s : officialStudents) {
            assertThat(s.isSynthetic()).isFalse();
        }

        // Verify synthetic isolation: synthetic students have status INACTIVE
        List<Student> syntheticStudents = studentRepository.findByIsSyntheticTrue();
        assertThat(syntheticStudents).isNotEmpty();
        for (Student syn : syntheticStudents) {
            assertThat(syn.getStatus()).isEqualTo("INACTIVE");
        }

        // Verify active section roster count: 715 official students have current=true
        List<UUID> officialStudentIds = officialStudents.stream().map(Student::getId).toList();
        int currentOfficialEnrollments = enrollmentRepository.findByStudentIdInAndCurrentTrue(officialStudentIds).size();
        assertThat(currentOfficialEnrollments).isEqualTo(715);

        // Step 2: Idempotency execution - running exact same import again
        ImportExecuteResponse response2 = importService.executeImport(regularPath, lateralPath, false);
        assertThat(response2.success()).isTrue();

        // Idempotency: Created count must be 0 on second run
        assertThat(response2.summary().createdCount()).isEqualTo(0);
        assertThat(response2.summary().unchangedCount()).isEqualTo(723);
    }
}
