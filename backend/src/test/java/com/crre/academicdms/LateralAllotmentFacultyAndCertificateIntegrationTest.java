package com.crre.academicdms;

import com.crre.academicdms.features.academic.model.Department;
import com.crre.academicdms.features.academic.model.Section;
import com.crre.academicdms.features.academic.repository.DepartmentRepository;
import com.crre.academicdms.features.academic.repository.SectionRepository;
import com.crre.academicdms.features.audit.model.SystemAuditLog;
import com.crre.academicdms.features.audit.repository.SystemAuditLogRepository;
import com.crre.academicdms.features.auth.dto.LoginRequest;
import com.crre.academicdms.features.faculty.dto.FacultyDtos.*;
import com.crre.academicdms.features.faculty.model.FacultyProfile;
import com.crre.academicdms.features.faculty.repository.FacultyProfileRepository;
import com.crre.academicdms.features.student.dto.StudentDtos.*;
import com.crre.academicdms.features.student.dto.UpdateStudentRequest;
import com.crre.academicdms.features.student.model.Student;
import com.crre.academicdms.features.student.model.StudentCertificate;
import com.crre.academicdms.features.student.model.StudentEnrollment;
import com.crre.academicdms.features.student.repository.StudentCertificateRepository;
import com.crre.academicdms.features.student.repository.StudentEnrollmentRepository;
import com.crre.academicdms.features.student.repository.StudentRepository;
import com.crre.academicdms.features.student.service.StudentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class LateralAllotmentFacultyAndCertificateIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private StudentEnrollmentRepository enrollmentRepository;

    @Autowired
    private SectionRepository sectionRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private FacultyProfileRepository facultyProfileRepository;

    @Autowired
    private StudentCertificateRepository certificateRepository;

    @Autowired
    private SystemAuditLogRepository auditLogRepository;

    @Autowired
    private StudentService studentService;

    private MockHttpSession loginAs(String username, String password) throws Exception {
        LoginRequest req = new LoginRequest(username, password);
        MvcResult res = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andReturn();
        return (MockHttpSession) res.getRequest().getSession(false);
    }

    // ==================== PHASE 2: LATERAL SECTION REASSIGNMENT ====================

    @Test
    @Transactional
    @DisplayName("Phase 2: Authorized HOD can reassign a lateral-entry student between sections in same program")
    void authorizedHod_canReassignLateralStudentBetweenSections() throws Exception {
        MockHttpSession hodSession = loginAs("hod.cse", "Admin@123");

        // Find an active official lateral student in CSE (e.g. 26B85A0501)
        Student lateralStudent = studentRepository.findByRegistrationNumber("26B85A0501")
            .orElseThrow(() -> new IllegalStateException("Lateral student 26B85A0501 not found"));
        assertThat(lateralStudent.getEntryType()).isEqualTo("LATERAL");

        // Current section of lateralStudent
        StudentEnrollment currentEnrollment = enrollmentRepository.findByStudentIdAndCurrentTrue(lateralStudent.getId())
            .orElseThrow(() -> new IllegalStateException("Active enrollment not found for lateral student"));
        Section currentSection = currentEnrollment.getSection();

        // Target: CSE Section 2 (or a different CSE section)
        List<Section> cseSections = sectionRepository.findAllWithProgram().stream()
            .filter(s -> "CSE".equalsIgnoreCase(s.getSemester().getProgram().getCode()))
            .filter(s -> !s.getId().equals(currentSection.getId()))
            .toList();
        assertThat(cseSections).isNotEmpty();
        Section targetSection = cseSections.get(0);

        ReassignSectionRequest reassignReq = new ReassignSectionRequest(targetSection.getId(), "Balanced lateral allocation");

        mockMvc.perform(put("/api/v1/students/" + lateralStudent.getRegistrationNumber() + "/reassign-section")
                .session(hodSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reassignReq)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.previousSectionId").value(currentSection.getId().toString()))
            .andExpect(jsonPath("$.data.newSectionId").value(targetSection.getId().toString()));

        // Verify database: exactly 1 current enrollment
        List<StudentEnrollment> enrollments = enrollmentRepository.findByStudentId(lateralStudent.getId());
        long currentCount = enrollments.stream().filter(StudentEnrollment::isCurrent).count();
        assertThat(currentCount).isEqualTo(1);

        StudentEnrollment newCurrent = enrollmentRepository.findByStudentIdAndCurrentTrue(lateralStudent.getId()).orElseThrow();
        assertThat(newCurrent.getSection().getId()).isEqualTo(targetSection.getId());

        // Verify audit log created
        List<SystemAuditLog> auditLogs = auditLogRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc("STUDENT_ENROLLMENT", lateralStudent.getId().toString());
        assertThat(auditLogs).isNotEmpty();
        assertThat(auditLogs.get(0).getAction()).isIn("REASSIGN_LATERAL_SECTION", "ASSIGN_LATERAL_SECTION");
        assertThat(auditLogs.get(0).getActorUsername()).isEqualTo("hod.cse");
    }

    @Test
    @Transactional
    @DisplayName("Phase 2: Reassigning regular-entry student section allotment is strictly rejected")
    void reassigningRegularStudentSection_isStrictlyRejected() throws Exception {
        MockHttpSession hodSession = loginAs("hod.cse", "Admin@123");

        // Pick an active official regular student (e.g. 25B81A0501)
        Student regularStudent = studentRepository.findByRegistrationNumber("25B81A0501")
            .orElseThrow(() -> new IllegalStateException("Regular student 25B81A0501 not found"));
        assertThat(regularStudent.getEntryType()).isEqualTo("REGULAR");

        // Target another section
        Section otherSection = sectionRepository.findAllWithProgram().stream()
            .filter(s -> "CSE".equalsIgnoreCase(s.getSemester().getProgram().getCode()))
            .filter(s -> s.getSectionNumber() == 2)
            .findFirst()
            .orElseThrow();

        // 1. Test dedicated reassign-section endpoint
        ReassignSectionRequest reassignReq = new ReassignSectionRequest(otherSection.getId(), "Attempt regular shift");
        mockMvc.perform(put("/api/v1/students/" + regularStudent.getRegistrationNumber() + "/reassign-section")
                .session(hodSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reassignReq)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message", containsString("Only lateral-entry students can have their section allotment modified")));

        // 2. Test via general updateStudent endpoint
        UpdateStudentRequest updateReq = new UpdateStudentRequest(
            regularStudent.getFullName(),
            null,
            "REGULAR",
            regularStudent.getAdmissionCategory(),
            regularStudent.getTuitionCategory(),
            null,
            null,
            otherSection.getId(),
            "ACTIVE"
        );
        mockMvc.perform(put("/api/v1/students/" + regularStudent.getId())
                .session(hodSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateReq)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message", containsString("Only lateral-entry students can have their section allotment modified")));
    }

    @Test
    @Transactional
    @DisplayName("Phase 2: Reassigning lateral student across different programs is rejected")
    void reassigningLateralStudent_acrossPrograms_isRejected() throws Exception {
        MockHttpSession hodSession = loginAs("hod.cse", "Admin@123");

        Student lateralStudent = studentRepository.findByRegistrationNumber("26B85A0501").orElseThrow();

        // Target an AIDS or CS section
        Section aidsSection = sectionRepository.findAllWithProgram().stream()
            .filter(s -> "AIDS".equalsIgnoreCase(s.getSemester().getProgram().getCode()))
            .findFirst()
            .orElseThrow();

        ReassignSectionRequest crossProgReq = new ReassignSectionRequest(aidsSection.getId(), "Cross program shift");
        mockMvc.perform(put("/api/v1/students/" + lateralStudent.getRegistrationNumber() + "/reassign-section")
                .session(hodSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(crossProgReq)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message", containsString("Cannot reassign lateral student across different programs")));
    }

    @Test
    @Transactional
    @DisplayName("Phase 2: Authorized HOD can assign previously unallocated lateral student to Section 6")
    void authorizedHod_canAssignUnallocatedLateralStudent() throws Exception {
        MockHttpSession hodSession = loginAs("hod.cse", "Admin@123");

        // 26B85A0537 is one of the 8 unallocated lateral students
        Student unallocatedStudent = studentRepository.findByRegistrationNumber("26B85A0537").orElseThrow();
        assertThat(unallocatedStudent.getEntryType()).isEqualTo("LATERAL");

        // Target CSE Section 6
        Section cseSec6 = sectionRepository.findAllWithProgram().stream()
            .filter(s -> "CSE".equalsIgnoreCase(s.getSemester().getProgram().getCode()) && s.getSectionNumber() == 6)
            .findFirst()
            .orElseThrow();

        ReassignSectionRequest assignReq = new ReassignSectionRequest(cseSec6.getId(), "Allotting unallocated lateral to Section 6");

        mockMvc.perform(put("/api/v1/students/" + unallocatedStudent.getRegistrationNumber() + "/reassign-section")
                .session(hodSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(assignReq)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.previousSectionName").value("UNALLOCATED"))
            .andExpect(jsonPath("$.data.newSectionName").value("Section 6"));

        // Verify active enrollment now exists
        StudentEnrollment enr = enrollmentRepository.findByStudentIdAndCurrentTrue(unallocatedStudent.getId()).orElseThrow();
        assertThat(enr.getSection().getId()).isEqualTo(cseSec6.getId());
    }

    // ==================== PHASE 3: FACULTY WEB PROFILES ====================

    @Test
    @Transactional
    @DisplayName("Phase 3: Official faculty profiles from sircrrengg.ac.in are loaded with designations and source URLs")
    void officialFacultyProfiles_areLoadedWithSourceUrls() throws Exception {
        MockHttpSession hodSession = loginAs("hod.cse", "Admin@123");

        mockMvc.perform(get("/api/v1/faculty/profiles").session(hodSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(20))))
            .andExpect(jsonPath("$.data[?(@.employeeNumber == 'CRRE-FAC-OFF-001')].fullName", hasItem("Dr. A. Yesubabu")))
            .andExpect(jsonPath("$.data[?(@.employeeNumber == 'CRRE-FAC-OFF-001')].designation", hasItem("Professor & HOD")))
            .andExpect(jsonPath("$.data[?(@.employeeNumber == 'CRRE-FAC-OFF-001')].sourceUrl", hasItem(containsString("sircrrengg.ac.in"))))
            .andExpect(jsonPath("$.data[?(@.employeeNumber == 'CRRE-FAC-OFF-003')].fullName", hasItem("Dr. B. Madhava Rao")));
    }

    @Test
    @Transactional
    @DisplayName("Phase 3: Authorized HOD can update faculty profile fields")
    void authorizedHod_canUpdateFacultyProfileFields() throws Exception {
        MockHttpSession hodSession = loginAs("hod.cse", "Admin@123");

        FacultyProfile fp = facultyProfileRepository.findByEmployeeNumber("CRRE-FAC-OFF-001")
            .orElseThrow(() -> new IllegalStateException("CRRE-FAC-OFF-001 not found"));

        UpdateFacultyProfileRequest updateReq = new UpdateFacultyProfileRequest(
            "Senior Professor & HOD",
            "M.Tech., Ph.D., Post-Doc",
            "Advanced Artificial Intelligence & Algorithms",
            "25+ Years Institutional Experience",
            "hodcse@sircrrengg.ac.in",
            "https://sircrrengg.ac.in/departments/computer-science-and-engineering/faculty",
            true
        );

        mockMvc.perform(put("/api/v1/faculty/profiles/" + fp.getId())
                .session(hodSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateReq)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.designation").value("Senior Professor & HOD"))
            .andExpect(jsonPath("$.data.specialization").value("Advanced Artificial Intelligence & Algorithms"));

        // Verify DB persistence
        FacultyProfile reloaded = facultyProfileRepository.findById(fp.getId()).orElseThrow();
        assertThat(reloaded.getDesignation()).isEqualTo("Senior Professor & HOD");
        assertThat(reloaded.getSpecialization()).isEqualTo("Advanced Artificial Intelligence & Algorithms");
    }

    // ==================== PHASE 4: CERTIFICATE DATA MODEL ====================

    @Test
    @Transactional
    @DisplayName("Phase 4: Certificate lifecycle — Add, Update verification status, and Retrieve")
    void certificateLifecycle_AddUpdateAndRetrieve() throws Exception {
        MockHttpSession hodSession = loginAs("hod.cse", "Admin@123");

        Student student = studentRepository.findByRegistrationNumber("25B81A0501").orElseThrow();

        // 1. Add Certificate (Transfer Certificate)
        UpsertCertificateRequest addReq = new UpsertCertificateRequest(
            "TRANSFER_CERTIFICATE",
            "College Transfer Certificate",
            "TC-2025-99881",
            "Sir CR Reddy Junior College",
            2025,
            LocalDate.of(2025, 7, 15),
            "SUBMITTED",
            "Original copy submitted at admission desk"
        );

        MvcResult addRes = mockMvc.perform(post("/api/v1/students/" + student.getRegistrationNumber() + "/certificates")
                .session(hodSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(addReq)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.certificateType").value("TRANSFER_CERTIFICATE"))
            .andExpect(jsonPath("$.data.submissionStatus").value("SUBMITTED"))
            .andReturn();

        String respStr = addRes.getResponse().getContentAsString();
        UUID certId = UUID.fromString(objectMapper.readTree(respStr).path("data").path("id").asText());

        // 2. Update Certificate status to VERIFIED
        UpsertCertificateRequest verifyReq = new UpsertCertificateRequest(
            "TRANSFER_CERTIFICATE",
            "College Transfer Certificate",
            "TC-2025-99881",
            "Sir CR Reddy Junior College",
            2025,
            LocalDate.of(2025, 7, 15),
            "VERIFIED",
            "Physically verified by admission committee"
        );

        mockMvc.perform(put("/api/v1/students/" + student.getRegistrationNumber() + "/certificates/" + certId)
                .session(hodSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(verifyReq)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.submissionStatus").value("VERIFIED"))
            .andExpect(jsonPath("$.data.verifiedByName").value("Dr. M. Sreenivasa Rao"));

        // 3. Retrieve student details: Certificates array is present
        mockMvc.perform(get("/api/v1/students/" + student.getRegistrationNumber()).session(hodSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.certificates", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @Transactional
    @DisplayName("Phase 4: Certificate backfill populates certificates from official educational and entrance records")
    void certificateBackfill_PopulatesFromEducationalRecords() throws Exception {
        MockHttpSession adminSession = loginAs("admin", "Admin@123");

        mockMvc.perform(post("/api/v1/students/certificates/backfill").session(adminSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data", greaterThanOrEqualTo(500)));

        // Verify student 25B81A0501 has SSC and EAPCET certificates populated
        Student student = studentRepository.findByRegistrationNumber("25B81A0501").orElseThrow();
        List<StudentCertificate> certs = certificateRepository.findByStudentIdOrderByCreatedAtAsc(student.getId());
        assertThat(certs).isNotEmpty();
        assertThat(certs.stream().map(StudentCertificate::getCertificateType).toList())
            .contains("SSC_PASS_CERTIFICATE", "EAPCET_RANK_CARD");
    }
}
