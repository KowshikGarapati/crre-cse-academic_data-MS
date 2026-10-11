package com.crre.academicdms;

import com.crre.academicdms.features.auth.dto.LoginRequest;
import com.crre.academicdms.features.responsibility.dto.ResponsibilityRequest;
import com.crre.academicdms.features.user.dto.CreateUserRequest;
import com.crre.academicdms.features.user.model.Role;
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

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Session Login with student username returns 200 and establishes HTTP session")
    void loginWithUsername_success() throws Exception {
        LoginRequest request = new LoginRequest("25b81a4201", "Student@123");

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.user.username").value("25b81a4201"))
            .andExpect(jsonPath("$.data.user.role").value("STUDENT"))
            .andExpect(jsonPath("$.data.user.registrationNumber").value("25B81A4201"))
            .andReturn();

        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        assertThat(session).isNotNull();
    }

    @Test
    @DisplayName("Session Login with registration number returns 200")
    void loginWithRegistrationNumber_success() throws Exception {
        LoginRequest request = new LoginRequest("25B81A4201", "Student@123");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.user.username").value("25b81a4201"))
            .andExpect(jsonPath("$.data.user.role").value("STUDENT"));
    }

    @Test
    @DisplayName("Login with invalid password returns 401 Unauthorized")
    void loginWithWrongPassword_unauthorized() throws Exception {
        LoginRequest request = new LoginRequest("admin", "WrongPassword!");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("Invalid identifier or password"));
    }

    @Test
    @DisplayName("GET /api/v1/auth/me without active session returns 401 Unauthorized")
    void getCurrentUserWithoutSession_unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("GET /api/v1/auth/me with active HTTP session returns 200 and user profile")
    void getCurrentUserWithSession_success() throws Exception {
        // Step 1: Login as Principal
        LoginRequest request = new LoginRequest("principal", "Admin@123");
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertThat(session).isNotNull();

        // Step 2: Fetch current user profile using HTTP session
        mockMvc.perform(get("/api/v1/auth/me").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.username").value("principal"))
            .andExpect(jsonPath("$.data.role").value("PRINCIPAL"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/logout invalidates session")
    void logout_success() throws Exception {
        LoginRequest request = new LoginRequest("admin", "Admin@123");
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);

        mockMvc.perform(post("/api/v1/auth/logout").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("Logged out successfully"));

        // After logout, accessing /me should fail
        mockMvc.perform(get("/api/v1/auth/me").session(session))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Authority check: HOD cannot create or assign a Principal")
    void hodCannotCreatePrincipal_forbidden() throws Exception {
        // Login as HOD
        LoginRequest login = new LoginRequest("hod.cse", "Admin@123");
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(login)))
            .andExpect(status().isOk())
            .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);

        // Attempt to create a Principal
        CreateUserRequest req = new CreateUserRequest(
            "Test Principal", "test.prin", "testprin@crre.edu", "Password@123",
            Role.PRINCIPAL, null, "PRIN999", null, null, null, null, null, null, false
        );

        mockMvc.perform(post("/api/v1/users")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Assignment lifecycle: Add, modify, and remove responsibility without deleting user")
    void responsibilityLifecycle_success() throws Exception {
        // Login as Admin
        LoginRequest login = new LoginRequest("admin", "Admin@123");
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(login)))
            .andExpect(status().isOk())
            .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);

        UUID facultyId = UUID.fromString("00000000-0000-0000-0000-000000000003");
        UUID subjectId = UUID.fromString("70000000-0000-0000-0000-000000000003"); // Environmental Science
        UUID sectionId = UUID.fromString("60000000-0000-0000-0000-000000000001");

        // 1. Add new teaching assignment
        ResponsibilityRequest addReq = new ResponsibilityRequest("TEACHING", null, null, subjectId, sectionId, "Env Science Sec 1");
        MvcResult addRes = mockMvc.perform(post("/api/v1/users/" + facultyId + "/responsibilities")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(addReq)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.type").value("TEACHING"))
            .andReturn();

        String respJson = addRes.getResponse().getContentAsString();
        String respId = objectMapper.readTree(respJson).path("data").path("id").asText();

        // 2. Prevent duplicate active teaching assignment
        mockMvc.perform(post("/api/v1/users/" + facultyId + "/responsibilities")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(addReq)))
            .andExpect(status().isBadRequest());

        // 3. Remove responsibility
        mockMvc.perform(delete("/api/v1/responsibilities/" + respId)
                .session(session))
            .andExpect(status().isOk());

        // 4. Verify user still exists!
        mockMvc.perform(get("/api/v1/users")
                .session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[?(@.id == '" + facultyId + "')]").exists());
    }

    @Test
    @DisplayName("Deactivation: Disabled user cannot log in")
    void disabledUser_cannotLogin() throws Exception {
        LoginRequest adminLogin = new LoginRequest("admin", "Admin@123");
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(adminLogin)))
            .andExpect(status().isOk())
            .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);

        // Create temporary user
        String username = "temp.user." + System.currentTimeMillis();
        CreateUserRequest createReq = new CreateUserRequest(
            "Temp User", username, username + "@crre.edu", "Temp@123",
            Role.STUDENT, null, null, null, null, null, null, null, null, false
        );

        MvcResult createRes = mockMvc.perform(post("/api/v1/users")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createReq)))
            .andExpect(status().isOk())
            .andReturn();

        String userId = objectMapper.readTree(createRes.getResponse().getContentAsString()).path("data").path("id").asText();

        // Disable user
        mockMvc.perform(patch("/api/v1/users/" + userId + "/status")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("enabled", false))))
            .andExpect(status().isOk());

        // Attempt login as disabled user -> should fail with 403 Forbidden
        LoginRequest userLogin = new LoginRequest(username, "Temp@123");
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userLogin)))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Section 1 has all 66 students")
    void section1_has66Students() throws Exception {
        LoginRequest login = new LoginRequest("admin", "Admin@123");
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(login)))
            .andExpect(status().isOk())
            .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);

        UUID sectionId = UUID.fromString("60000000-0000-0000-0000-000000000001");
        mockMvc.perform(get("/api/v1/sections/" + sectionId + "/students").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()", greaterThanOrEqualTo(66)))
            .andExpect(jsonPath("$.data[*].registrationNumber", hasItem("25B81A4201")))
            .andExpect(jsonPath("$.data[*].registrationNumber", hasItem("25B81A4266")));
    }

    @Test
    @DisplayName("TEST A - Normal Faculty Scope: Sees subjects & timetable, cannot access class teacher or counselling scope")
    void testA_normalFacultyScope() throws Exception {
        LoginRequest login = new LoginRequest("faculty.cse", "Faculty@123");
        MvcResult res = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(login)))
            .andExpect(status().isOk())
            .andReturn();

        MockHttpSession session = (MockHttpSession) res.getRequest().getSession(false);

        // Can see my subjects
        mockMvc.perform(get("/api/v1/faculty/my-subjects").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray());

        // Can see my timetable (with room number)
        mockMvc.perform(get("/api/v1/faculty/my-timetable").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray());

        // Cannot access class teacher section students (throws 403)
        mockMvc.perform(get("/api/v1/faculty/my-section/students").session(session))
            .andExpect(status().isForbidden());

        // No assigned counselling students
        mockMvc.perform(get("/api/v1/faculty/my-counselling/students").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @DisplayName("TEST B - Faculty + Class Teacher: Sees section students and section academic overview")
    void testB_classTeacherScope() throws Exception {
        LoginRequest login = new LoginRequest("ramalakshmi", "Faculty@123");
        MvcResult res = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(login)))
            .andExpect(status().isOk())
            .andReturn();

        MockHttpSession session = (MockHttpSession) res.getRequest().getSession(false);

        // Class teacher section overview
        mockMvc.perform(get("/api/v1/faculty/my-section/overview").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.sectionName").value("Section 1"))
            .andExpect(jsonPath("$.data.classTeacherName").value("CH. Rama Lakshmi"));

        // Class teacher section students
        mockMvc.perform(get("/api/v1/faculty/my-section/students").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()", greaterThanOrEqualTo(66)));
    }

    @Test
    @DisplayName("TEST C - Faculty + Counsellor: Sees assigned counselling students and meeting records")
    void testC_counsellorScope() throws Exception {
        LoginRequest login = new LoginRequest("counsellor.cse", "Faculty@123");
        MvcResult res = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(login)))
            .andExpect(status().isOk())
            .andReturn();

        MockHttpSession session = (MockHttpSession) res.getRequest().getSession(false);

        // Sees assigned counselling students
        mockMvc.perform(get("/api/v1/faculty/my-counselling/students").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(greaterThanOrEqualTo(15)))
            .andExpect(jsonPath("$.data[0].registrationNumber").value("25B81A4201"));

        // Sees counselling session logs
        mockMvc.perform(get("/api/v1/faculty/my-counselling/records").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("TEST D - HOD: Sees Department -> Academic Years -> Programs -> Semesters -> Sections hierarchy")
    void testD_hodHierarchy() throws Exception {
        LoginRequest login = new LoginRequest("hod.cse", "Admin@123");
        MvcResult res = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(login)))
            .andExpect(status().isOk())
            .andReturn();

        MockHttpSession session = (MockHttpSession) res.getRequest().getSession(false);

        mockMvc.perform(get("/api/v1/hod/my-department").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.departmentCode").value("CSE"))
            .andExpect(jsonPath("$.data.academicYears").isArray())
            .andExpect(jsonPath("$.data.faculty").isArray())
            .andExpect(jsonPath("$.data.subjects").isArray());
    }

    @Test
    @DisplayName("TEST E - Principal: Sees College Overview with Programs nested inside Departments")
    void testE_principalHierarchy() throws Exception {
        LoginRequest login = new LoginRequest("principal", "Admin@123");
        MvcResult res = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(login)))
            .andExpect(status().isOk())
            .andReturn();

        MockHttpSession session = (MockHttpSession) res.getRequest().getSession(false);

        mockMvc.perform(get("/api/v1/principal/college-overview").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.institutionCode").value("CRRE"))
            .andExpect(jsonPath("$.data.departments[0].code").value("CSE"))
            .andExpect(jsonPath("$.data.departments[0].programNames[0]").value("Artificial Intelligence & Machine Learning"));
    }

    @Test
    @DisplayName("TEST G - Student: Sees own academic placement, educational history, and section timetable")
    void testG_studentScope() throws Exception {
        LoginRequest login = new LoginRequest("25b81a4201", "Student@123");
        MvcResult res = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(login)))
            .andExpect(status().isOk())
            .andReturn();

        MockHttpSession session = (MockHttpSession) res.getRequest().getSession(false);

        // Personal profile details
        mockMvc.perform(get("/api/v1/student/me").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.registrationNumber").value("25B81A4201"))
            .andExpect(jsonPath("$.data.placement.departmentName").value("Computer Science and Engineering"))
            .andExpect(jsonPath("$.data.placement.programName").value("Artificial Intelligence & Machine Learning"))
            .andExpect(jsonPath("$.data.classTeacherName").value("CH. Rama Lakshmi"))
            .andExpect(jsonPath("$.data.educationalHistory[0].qualificationType").value("SSC"))
            .andExpect(jsonPath("$.data.educationalHistory[1].qualificationType").value("INTERMEDIATE"));

        // Student timetable
        mockMvc.perform(get("/api/v1/student/my-timetable").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("TEST I - Add Student: Validates department scope for HOD and saves educational history")
    void testI_addStudentFlow() throws Exception {
        LoginRequest login = new LoginRequest("hod.cse", "Admin@123");
        MvcResult res = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(login)))
            .andExpect(status().isOk())
            .andReturn();

        MockHttpSession session = (MockHttpSession) res.getRequest().getSession(false);

        UUID cseDeptId = UUID.fromString("20000000-0000-0000-0000-000000000001");
        UUID aimlProgId = UUID.fromString("30000000-0000-0000-0000-000000000001");
        UUID ayId = UUID.fromString("40000000-0000-0000-0000-000000000001");
        UUID semId = UUID.fromString("50000000-0000-0000-0000-000000000001");
        UUID secId = UUID.fromString("60000000-0000-0000-0000-000000000001");

        String regNo = "25B81A" + (int)(Math.random() * 8999 + 1000);
        String username = "student." + System.currentTimeMillis();

        var eduHistory = java.util.List.of(
            new com.crre.academicdms.features.user.dto.ScopeAwareUserDtos.CreateEducationalHistoryRequest(
                "SSC", "BSEAP", "Govt High School", "HT-12345", 2021, "General", null,
                new java.math.BigDecimal("600"), new java.math.BigDecimal("510"), new java.math.BigDecimal("85.00"), null
            )
        );

        var req = new com.crre.academicdms.features.user.dto.ScopeAwareUserDtos.CreateStudentUserRequest(
            username, username + "@crre.edu", "New Test Student", "Student@123",
            regNo, "CRRE" + regNo, null, null, "REGULAR",
            cseDeptId, aimlProgId, ayId, semId, secId, eduHistory
        );

        mockMvc.perform(post("/api/v1/users/students")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.username").value(username))
            .andExpect(jsonPath("$.data.role").value("STUDENT"));
    }

    @Test
    @DisplayName("TEST J - Add Faculty: Professional details, no program field, assigns teaching responsibilities")
    void testJ_addFacultyFlow() throws Exception {
        LoginRequest login = new LoginRequest("hod.cse", "Admin@123");
        MvcResult res = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(login)))
            .andExpect(status().isOk())
            .andReturn();

        MockHttpSession session = (MockHttpSession) res.getRequest().getSession(false);

        UUID cseDeptId = UUID.fromString("20000000-0000-0000-0000-000000000001");
        UUID subjectId = UUID.fromString("70000000-0000-0000-0000-000000000006"); // CS306
        UUID secId = UUID.fromString("60000000-0000-0000-0000-000000000001");

        String empNo = "E" + (System.currentTimeMillis() % 100000000);
        String username = "fac." + (System.currentTimeMillis() % 100000000);

        var teaching = java.util.List.of(
            new com.crre.academicdms.features.user.dto.ScopeAwareUserDtos.TeachingAssignmentInput(subjectId, secId)
        );

        var req = new com.crre.academicdms.features.user.dto.ScopeAwareUserDtos.CreateFacultyUserRequest(
            username, username + "@crre.edu", "Dr. Test Faculty", "Faculty@123",
            empNo, cseDeptId, "Assistant Professor", "Ph.D", "Cyber Security",
            "5 Years", "test.faculty@crre.edu", teaching, null, false
        );

        mockMvc.perform(post("/api/v1/users/faculty")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.username").value(username))
            .andExpect(jsonPath("$.data.role").value("FACULTY"));

        // Now attempt to log in with this newly created faculty user!
        LoginRequest facultyLogin = new LoginRequest(username, "Faculty@123");
        MvcResult facLoginRes = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(facultyLogin)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.user.username").value(username))
            .andExpect(jsonPath("$.data.user.role").value("FACULTY"))
            .andReturn();

        MockHttpSession facSession = (MockHttpSession) facLoginRes.getRequest().getSession(false);
        assertThat(facSession).isNotNull();

        mockMvc.perform(get("/api/v1/auth/me").session(facSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.username").value(username))
            .andExpect(jsonPath("$.data.role").value("FACULTY"));
    }

    @Test
    @DisplayName("TEST K - Authentication: Wrong password returns 401 Unauthorized")
    void testK_wrongPasswordReturnsUnauthorized() throws Exception {
        LoginRequest wrongPass = new LoginRequest("hod.cse", "WrongPassword999!");
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(wrongPass)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("TEST L - Student Details: Scoped access control, timetable & courses display, and strict counselling confidentiality")
    void testL_studentDetailsAccessControlAndConfidentiality() throws Exception {
        UUID studentUserId = UUID.fromString("00000000-0000-0000-0000-000000000020");

        // 1. HOD CSE: Can access student and view counselling
        LoginRequest hodLogin = new LoginRequest("hod.cse", "Admin@123");
        MvcResult hodRes = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(hodLogin)))
            .andExpect(status().isOk())
            .andReturn();
        MockHttpSession hodSession = (MockHttpSession) hodRes.getRequest().getSession(false);

        mockMvc.perform(get("/api/v1/students/" + studentUserId).session(hodSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.registrationNumber").value("25B81A4201"))
            .andExpect(jsonPath("$.data.fullName").value("AKHIL SHARIFF"))
            .andExpect(jsonPath("$.data.placement.departmentName").value("Computer Science and Engineering"))
            .andExpect(jsonPath("$.data.canViewCounselling").value(true));

        // 2. Assigned Counsellor: Can access and view counselling
        LoginRequest counsellorLogin = new LoginRequest("counsellor.cse", "Faculty@123");
        MvcResult counsRes = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(counsellorLogin)))
            .andExpect(status().isOk())
            .andReturn();
        MockHttpSession counsSession = (MockHttpSession) counsRes.getRequest().getSession(false);

        mockMvc.perform(get("/api/v1/students/" + studentUserId).session(counsSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.registrationNumber").value("25B81A4201"))
            .andExpect(jsonPath("$.data.canViewCounselling").value(true));

        // 3. Teaching Faculty (Rajesh Kumar teaches CS304/305 to Section 1):
        // Can access student, registered courses and timetable, BUT canViewCounselling MUST BE FALSE!
        LoginRequest facLogin = new LoginRequest("faculty.cse", "Faculty@123");
        MvcResult facRes = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(facLogin)))
            .andExpect(status().isOk())
            .andReturn();
        MockHttpSession facSession = (MockHttpSession) facRes.getRequest().getSession(false);

        mockMvc.perform(get("/api/v1/students/" + studentUserId).session(facSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.registrationNumber").value("25B81A4201"))
            .andExpect(jsonPath("$.data.canViewCounselling").value(false))
            .andExpect(jsonPath("$.data.counsellingRecords").isEmpty())
            .andExpect(jsonPath("$.data.currentCourses").isArray())
            .andExpect(jsonPath("$.data.currentCourses[0].name").exists())
            .andExpect(jsonPath("$.data.timetable").isArray());

        // 4. Other Student: Forbidden from accessing someone else's details
        LoginRequest otherStudentLogin = new LoginRequest("22b91a0501", "Student@123");
        MvcResult studentRes = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(otherStudentLogin)))
            .andExpect(status().isOk())
            .andReturn();
        MockHttpSession otherStudentSession = (MockHttpSession) studentRes.getRequest().getSession(false);

        mockMvc.perform(get("/api/v1/students/" + studentUserId).session(otherStudentSession))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("TEST M - Faculty Schedule: Today classes and chronological timetable ordering")
    void testM_facultyTodayClassesAndTimetableOrdering() throws Exception {
        LoginRequest facLogin = new LoginRequest("faculty.cse", "Faculty@123");
        MvcResult facRes = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(facLogin)))
            .andExpect(status().isOk())
            .andReturn();
        MockHttpSession facSession = (MockHttpSession) facRes.getRequest().getSession(false);

        // Monday classes for faculty.cse (CS302 P1 09:30, CS302 P2 10:20)
        mockMvc.perform(get("/api/v1/faculty/today-classes?day=MONDAY").session(facSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data[0].dayOfWeek").value("MONDAY"))
            .andExpect(jsonPath("$.data[0].periodNumber").value(1))
            .andExpect(jsonPath("$.data[0].subjectCode").value("CS302"))
            .andExpect(jsonPath("$.data[1].periodNumber").value(2))
            .andExpect(jsonPath("$.data[1].subjectCode").value("CS302"));

        // Full timetable: ensure chronological day order (Monday -> Tuesday -> Wednesday -> etc.)
        MvcResult ttRes = mockMvc.perform(get("/api/v1/faculty/my-timetable").session(facSession))
            .andExpect(status().isOk())
            .andReturn();

        String ttJson = ttRes.getResponse().getContentAsString();
        var rootNode = objectMapper.readTree(ttJson).path("data");
        assertThat(rootNode.isArray()).isTrue();
        assertThat(rootNode.size()).isGreaterThan(0);

        java.util.Map<String, Integer> dayWeights = java.util.Map.of(
            "MONDAY", 1, "TUESDAY", 2, "WEDNESDAY", 3, "THURSDAY", 4, "FRIDAY", 5, "SATURDAY", 6, "SUNDAY", 7
        );

        int prevDayWeight = 0;
        java.time.LocalTime prevStartTime = java.time.LocalTime.MIN;

        for (var entry : rootNode) {
            String day = entry.path("dayOfWeek").asText().toUpperCase();
            int currentDayWeight = dayWeights.getOrDefault(day, 99);
            java.time.LocalTime currentStartTime = java.time.LocalTime.parse(entry.path("startTime").asText());

            if (currentDayWeight > prevDayWeight) {
                prevDayWeight = currentDayWeight;
                prevStartTime = currentStartTime;
            } else if (currentDayWeight == prevDayWeight) {
                assertThat(currentStartTime).isAfterOrEqualTo(prevStartTime);
                prevStartTime = currentStartTime;
            } else {
                org.junit.jupiter.api.Assertions.fail("Timetable day order is not chronological! Found " + day + " after weight " + prevDayWeight);
            }
        }
    }
}
