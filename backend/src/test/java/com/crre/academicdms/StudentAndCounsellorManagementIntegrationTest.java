package com.crre.academicdms;

import com.crre.academicdms.features.academic.model.Department;
import com.crre.academicdms.features.academic.repository.DepartmentRepository;
import com.crre.academicdms.features.auth.dto.LoginRequest;
import com.crre.academicdms.features.counselling.repository.CounsellingAssignmentRepository;
import com.crre.academicdms.features.responsibility.dto.AssignCounsellingStudentsRequest;
import com.crre.academicdms.features.responsibility.dto.ResponsibilityRequest;
import com.crre.academicdms.features.responsibility.model.Responsibility;
import com.crre.academicdms.features.responsibility.repository.ResponsibilityRepository;
import com.crre.academicdms.features.responsibility.service.ResponsibilityService;
import com.crre.academicdms.features.student.dto.UpdateStudentRequest;
import com.crre.academicdms.features.student.model.Student;
import com.crre.academicdms.features.student.repository.StudentRepository;
import com.crre.academicdms.features.user.model.Role;
import com.crre.academicdms.features.user.model.User;
import com.crre.academicdms.features.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class StudentAndCounsellorManagementIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ResponsibilityRepository responsibilityRepository;

    @Autowired
    private CounsellingAssignmentRepository counsellingAssignmentRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private MockHttpSession loginAs(String username, String password) throws Exception {
        LoginRequest req = new LoginRequest(username, password);
        MvcResult res = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andReturn();
        return (MockHttpSession) res.getRequest().getSession(false);
    }

    @Test
    @Transactional
    @DisplayName("Feature 1: Authorized HOD updates student data within their department")
    void authorizedHod_canUpdateStudentData() throws Exception {
        MockHttpSession hodSession = loginAs("hod.cse", "Admin@123");

        Student student = studentRepository.findAll().stream()
            .filter(s -> s.getUser() != null)
            .findFirst()
            .orElseThrow();
        String studentId = student.getId().toString();

        UpdateStudentRequest updateReq = new UpdateStudentRequest(
            "Updated Student Full Name",
            "updated.student.test@crre.edu.in",
            "LATERAL",
            "MANAGEMENT",
            "SELF_FINANCE",
            null,
            null,
            null,
            "ACTIVE"
        );

        mockMvc.perform(put("/api/v1/students/" + studentId)
                .session(hodSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateReq)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.fullName").value("Updated Student Full Name"))
            .andExpect(jsonPath("$.data.entryType").value("LATERAL"))
            .andExpect(jsonPath("$.data.admissionCategory").value("MANAGEMENT"))
            .andExpect(jsonPath("$.data.tuitionCategory").value("SELF_FINANCE"))
            .andExpect(jsonPath("$.data.email").value("updated.student.test@crre.edu.in"));

        // Verify database persistence
        Student reloaded = studentRepository.findById(student.getId()).orElseThrow();
        assertThat(reloaded.getFullName()).isEqualTo("Updated Student Full Name");
        assertThat(reloaded.getEntryType()).isEqualTo("LATERAL");
        assertThat(reloaded.getAdmissionCategory()).isEqualTo("MANAGEMENT");
        assertThat(reloaded.getTuitionCategory()).isEqualTo("SELF_FINANCE");
    }

    @Test
    @Transactional
    @DisplayName("Feature 1: Student role is forbidden from editing student data")
    void studentRole_isDeniedFromEditingStudent() throws Exception {
        MockHttpSession studentSession = loginAs("25B81A4201", "Student@123");

        Student student = studentRepository.findAll().stream()
            .filter(s -> s.getUser() != null)
            .findFirst()
            .orElseThrow();

        UpdateStudentRequest updateReq = new UpdateStudentRequest(
            "Malicious Student Update",
            "hacker@crre.edu.in",
            "REGULAR",
            "CONVENOR",
            "RTF",
            null,
            null,
            null,
            "ACTIVE"
        );

        mockMvc.perform(put("/api/v1/students/" + student.getId())
                .session(studentSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateReq)))
            .andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    @DisplayName("Feature 1: Validation fails on invalid entry type and invalid categories")
    void updateStudent_validationFailsOnInvalidCategories() throws Exception {
        MockHttpSession hodSession = loginAs("hod.cse", "Admin@123");

        Student student = studentRepository.findAll().stream()
            .filter(s -> s.getUser() != null)
            .findFirst()
            .orElseThrow();

        UpdateStudentRequest invalidReq = new UpdateStudentRequest(
            "Valid Name",
            "valid.email@crre.edu.in",
            "INVALID_ENTRY_TYPE",
            "INVALID_CAT",
            "INVALID_TUITION",
            null,
            null,
            null,
            "ACTIVE"
        );

        mockMvc.perform(put("/api/v1/students/" + student.getId())
                .session(hodSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidReq)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    @DisplayName("Feature 2: HOD and Admin manage students assigned to a counsellor")
    void counsellorManagement_viewAssignAndRemoveMentees() throws Exception {
        MockHttpSession adminSession = loginAs("admin", "Admin@123");

        // 1. Locate counsellor responsibility
        List<Responsibility> counsellors = responsibilityRepository.findByTypeAndStatus("COUNSELLOR", "ACTIVE");
        assertThat(counsellors).isNotEmpty();
        Responsibility counsellorResp = counsellors.get(0);
        UUID respId = counsellorResp.getId();

        // 2. View current assigned students
        mockMvc.perform(get("/api/v1/responsibilities/" + respId + "/counselling-students")
                .session(adminSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray());

        // 3. Find 2 students
        List<Student> students = studentRepository.findAll();
        Student student1 = students.get(0);
        Student student2 = students.get(1);

        // 4. Assign student to counsellor
        AssignCounsellingStudentsRequest assignReq = new AssignCounsellingStudentsRequest(
            List.of(student1.getId(), student2.getId())
        );

        mockMvc.perform(post("/api/v1/responsibilities/" + respId + "/counselling-students")
                .session(adminSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(assignReq)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        // Verify assignment exists in database
        assertThat(counsellingAssignmentRepository.existsByCounsellorIdAndStudentIdAndStatus(
            counsellorResp.getUser().getId(), student1.getId(), "ACTIVE"
        )).isTrue();

        // 5. Duplicate assignment prevention: Re-assigning should be idempotent and succeed without duplicates
        mockMvc.perform(post("/api/v1/responsibilities/" + respId + "/counselling-students")
                .session(adminSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(assignReq)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        // 6. Remove student from counsellor
        mockMvc.perform(delete("/api/v1/responsibilities/" + respId + "/counselling-students/" + student1.getId())
                .session(adminSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        // Verify student1 assignment is now INACTIVE
        assertThat(counsellingAssignmentRepository.existsByCounsellorIdAndStudentIdAndStatus(
            counsellorResp.getUser().getId(), student1.getId(), "ACTIVE"
        )).isFalse();
    }

    @Test
    @Transactional
    @DisplayName("Feature 2: Student role cannot manage counsellor assignments")
    void unauthorizedUser_deniedFromManagingCounsellorAssignments() throws Exception {
        MockHttpSession studentSession = loginAs("25B81A4201", "Student@123");

        List<Responsibility> counsellors = responsibilityRepository.findByTypeAndStatus("COUNSELLOR", "ACTIVE");
        assertThat(counsellors).isNotEmpty();
        Responsibility counsellorResp = counsellors.get(0);

        mockMvc.perform(get("/api/v1/responsibilities/" + counsellorResp.getId() + "/counselling-students")
                .session(studentSession))
            .andExpect(status().isForbidden());
    }
}
