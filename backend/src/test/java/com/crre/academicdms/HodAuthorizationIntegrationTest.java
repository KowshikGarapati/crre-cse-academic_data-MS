package com.crre.academicdms;

import com.crre.academicdms.features.academic.model.Department;
import com.crre.academicdms.features.academic.model.Section;
import com.crre.academicdms.features.academic.repository.DepartmentRepository;
import com.crre.academicdms.features.academic.repository.SectionRepository;
import com.crre.academicdms.features.auth.dto.LoginRequest;
import com.crre.academicdms.features.responsibility.model.Responsibility;
import com.crre.academicdms.features.responsibility.repository.ResponsibilityRepository;
import com.crre.academicdms.features.user.dto.ScopeAwareUserDtos.CreateStudentUserRequest;
import com.crre.academicdms.features.user.model.Role;
import com.crre.academicdms.features.user.model.User;
import com.crre.academicdms.features.user.repository.UserRepository;
import com.crre.academicdms.features.responsibility.dto.ResponsibilityRequest;
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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class HodAuthorizationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private SectionRepository sectionRepository;

    @Autowired
    private ResponsibilityRepository responsibilityRepository;

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
    @DisplayName("Backward Compatibility: Existing HOD account with Role.HOD is granted HOD endpoints")
    void existingHodAccount_withRoleHod_grantedHodEndpoints() throws Exception {
        MockHttpSession session = loginAs("hod.cse", "Admin@123");

        // 1. My Department Structure
        mockMvc.perform(get("/api/v1/hod/my-department").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.departmentCode").value("CSE"));

        // 2. Department Users
        mockMvc.perform(get("/api/v1/users").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        // 3. Department Students Scope (715 official students across 10 sections)
        mockMvc.perform(get("/api/v1/students").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.length()", greaterThanOrEqualTo(715)));
    }

    @Test
    @Transactional
    @DisplayName("Target Model: User with base role FACULTY and active HOD_DEPT responsibility is granted HOD endpoints")
    void facultyUser_withActiveHodDeptResponsibility_grantedHodEndpoints() throws Exception {
        Department cseDept = departmentRepository.findByCode("CSE")
            .orElseThrow(() -> new IllegalStateException("CSE department not found"));

        Department testDept = new Department(
            cseDept.getInstitution(),
            "IT_TEST",
            "Information Technology Test Department"
        );
        testDept = departmentRepository.save(testDept);

        // Create faculty user with base role FACULTY
        String username = "faculty.hod.test";
        String rawPassword = "TestPassword@123";
        User facultyUser = new User(
            username,
            "faculty.hod.test@crre.edu",
            passwordEncoder.encode(rawPassword),
            "Dr. Test Faculty HOD",
            Role.FACULTY
        );
        facultyUser.setEnabled(true);
        facultyUser = userRepository.save(facultyUser);

        // Assign active HOD_DEPT responsibility to testDept
        Responsibility hodResp = new Responsibility(
            facultyUser,
            "HOD_DEPT",
            testDept,
            null,
            null,
            null,
            "Acting IT Department Head"
        );
        responsibilityRepository.save(hodResp);

        MockHttpSession session = loginAs(username, rawPassword);

        // 1. Verify access to HOD department endpoint
        mockMvc.perform(get("/api/v1/hod/my-department").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.departmentCode").value("IT_TEST"));

        // 2. Verify access to User management endpoint
        mockMvc.perform(get("/api/v1/users").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        // 3. Verify access to Departmental student scope
        mockMvc.perform(get("/api/v1/students").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Security Isolation: Ordinary Faculty without HOD_DEPT responsibility is denied HOD endpoints")
    void ordinaryFacultyUser_withoutHodResponsibility_deniedHodEndpoints() throws Exception {
        MockHttpSession session = loginAs("ramalakshmi", "Faculty@123");

        // 1. Attempt to access HOD department endpoint -> 403 Forbidden
        mockMvc.perform(get("/api/v1/hod/my-department").session(session))
            .andExpect(status().isForbidden());

        // 2. Attempt to access administrative user management -> 403 Forbidden
        mockMvc.perform(get("/api/v1/users").session(session))
            .andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    @DisplayName("Lifecycle Integrity: Faculty with INACTIVE HOD_DEPT responsibility is denied HOD endpoints")
    void facultyUser_withInactiveHodDeptResponsibility_deniedHodEndpoints() throws Exception {
        Department cseDept = departmentRepository.findByCode("CSE")
            .orElseThrow(() -> new IllegalStateException("CSE department not found"));

        String username = "faculty.inactive.hod";
        String rawPassword = "TestPassword@123";
        User facultyUser = new User(
            username,
            "faculty.inactive.hod@crre.edu",
            passwordEncoder.encode(rawPassword),
            "Dr. Past HOD",
            Role.FACULTY
        );
        facultyUser.setEnabled(true);
        facultyUser = userRepository.save(facultyUser);

        // Assign INACTIVE HOD responsibility
        Responsibility hodResp = new Responsibility(
            facultyUser,
            "HOD_DEPT",
            cseDept,
            null,
            null,
            null,
            "Former Department Head"
        );
        hodResp.setStatus("INACTIVE");
        responsibilityRepository.save(hodResp);

        MockHttpSession session = loginAs(username, rawPassword);

        // Verify denied
        mockMvc.perform(get("/api/v1/hod/my-department").session(session))
            .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/users").session(session))
            .andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    @DisplayName("Department Scope Boundaries: HOD of one department cannot create student in another department")
    void crossDepartmentAccess_denied() throws Exception {
        Department cseDept = departmentRepository.findByCode("CSE")
            .orElseThrow(() -> new IllegalStateException("CSE department not found"));

        Department eceDept = new Department(
            cseDept.getInstitution(),
            "ECE",
            "Electronics and Communication Engineering"
        );
        eceDept = departmentRepository.save(eceDept);

        String username = "ece.hod";
        String rawPassword = "TestPassword@123";
        User eceHod = new User(
            username,
            "ece.hod@crre.edu",
            passwordEncoder.encode(rawPassword),
            "Dr. ECE HOD",
            Role.FACULTY
        );
        eceHod.setEnabled(true);
        eceHod = userRepository.save(eceHod);

        Responsibility hodResp = new Responsibility(
            eceHod,
            "HOD_DEPT",
            eceDept,
            null,
            null,
            null,
            "ECE Department Head"
        );
        responsibilityRepository.save(hodResp);

        MockHttpSession session = loginAs(username, rawPassword);

        // Find a Section in CSE department to use its program, AY, semester, section IDs
        List<Section> sections = sectionRepository.findByDepartmentId(cseDept.getId());
        Section sec = sections.get(0);

        // Attempt to create a student in CSE department -> 403 Forbidden
        CreateStudentUserRequest req = new CreateStudentUserRequest(
            "foreign.student",
            "foreign.student@crre.edu",
            "Foreign Student",
            "Pass@123",
            "25B81A0599",
            "CRRE25B81A0599",
            null,
            null,
            "REGULAR",
            cseDept.getId(),
            sec.getSemester().getProgram().getId(),
            sec.getSemester().getAcademicYear().getId(),
            sec.getSemester().getId(),
            sec.getId(),
            null
        );

        mockMvc.perform(post("/api/v1/users/students")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    @DisplayName("HOD Integrity: Department-level HOD assignment succeeds and sectionId is strictly null")
    void validDepartmentLevelHodAssignment_succeedsAndSectionIsNull() throws Exception {
        MockHttpSession adminSession = loginAs("admin", "Admin@123");

        Department cseDept = departmentRepository.findByCode("CSE")
            .orElseThrow(() -> new IllegalStateException("CSE department not found"));

        Department dept = new Department(cseDept.getInstitution(), "MECH_TEST", "Mechanical Engineering Test");
        dept = departmentRepository.save(dept);

        User facultyUser = new User(
            "mech.hod.candidate",
            "mech.hod@crre.edu",
            passwordEncoder.encode("TestPassword@123"),
            "Dr. Mech HOD Candidate",
            Role.FACULTY
        );
        facultyUser.setEnabled(true);
        facultyUser = userRepository.save(facultyUser);

        ResponsibilityRequest req = new ResponsibilityRequest(
            "HOD_DEPT",
            dept.getId(),
            null,
            null,
            null,
            "Mechanical Engineering Department Head"
        );

        mockMvc.perform(post("/api/v1/users/" + facultyUser.getId() + "/responsibilities")
                .session(adminSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.type").value("HOD_DEPT"))
            .andExpect(jsonPath("$.data.departmentId").value(dept.getId().toString()))
            .andExpect(jsonPath("$.data.sectionId").doesNotExist());
    }

    @Test
    @Transactional
    @DisplayName("HOD Integrity: Section-specific HOD assignment is rejected with 400 Bad Request")
    void sectionSpecificHodAssignment_isRejectedWithBadRequest() throws Exception {
        MockHttpSession adminSession = loginAs("admin", "Admin@123");

        Section sec = sectionRepository.findAll().get(0);
        Department dept = sec.getSemester().getProgram().getDepartment();

        User facultyUser = new User(
            "invalid.hod.candidate",
            "invalid.hod@crre.edu",
            passwordEncoder.encode("TestPassword@123"),
            "Dr. Invalid HOD Candidate",
            Role.FACULTY
        );
        facultyUser.setEnabled(true);
        facultyUser = userRepository.save(facultyUser);

        // Attempt to pass sectionId when type is HOD_DEPT
        ResponsibilityRequest req = new ResponsibilityRequest(
            "HOD_DEPT",
            dept.getId(),
            null,
            null,
            sec.getId(),
            "Invalid Section-Scoped HOD"
        );

        mockMvc.perform(post("/api/v1/users/" + facultyUser.getId() + "/responsibilities")
                .session(adminSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message", containsString("HOD responsibility is department-scoped and must not reference a section")));
    }

    @Test
    @Transactional
    @DisplayName("Responsibility Preservation: Legitimate section-scoped responsibilities are accepted")
    void legitimateSectionScopedResponsibilities_accepted() throws Exception {
        MockHttpSession adminSession = loginAs("admin", "Admin@123");

        Section sec = sectionRepository.findAll().get(0);
        Department dept = sec.getSemester().getProgram().getDepartment();

        User facultyUser = new User(
            "ct.candidate",
            "ct.candidate@crre.edu",
            passwordEncoder.encode("TestPassword@123"),
            "Dr. Class Teacher Candidate",
            Role.FACULTY
        );
        facultyUser.setEnabled(true);
        facultyUser = userRepository.save(facultyUser);

        ResponsibilityRequest req = new ResponsibilityRequest(
            "CLASS_TEACHER",
            dept.getId(),
            null,
            null,
            sec.getId(),
            "Class Teacher Assignment"
        );

        mockMvc.perform(post("/api/v1/users/" + facultyUser.getId() + "/responsibilities")
                .session(adminSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.type").value("CLASS_TEACHER"))
            .andExpect(jsonPath("$.data.sectionId").value(sec.getId().toString()));
    }

    @Test
    @Transactional
    @DisplayName("Role Filtering: Department / Users role filter accurately matches decoupled roles and responsibilities")
    void departmentUserFiltering_byRoleAndActiveResponsibility() throws Exception {
        MockHttpSession adminSession = loginAs("admin", "Admin@123");

        // 1. Filter by HOD
        mockMvc.perform(get("/api/v1/users?role=HOD").session(adminSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))))
            .andExpect(jsonPath("$.data[*].responsibilities[*].type", hasItem("HOD_DEPT")));

        // 2. Filter by CLASS_TEACHER
        mockMvc.perform(get("/api/v1/users?role=CLASS_TEACHER").session(adminSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))))
            .andExpect(jsonPath("$.data[*].responsibilities[*].type", hasItem("CLASS_TEACHER")));

        // 3. Filter by COUNSELLOR
        mockMvc.perform(get("/api/v1/users?role=COUNSELLOR").session(adminSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))))
            .andExpect(jsonPath("$.data[*].responsibilities[*].type", hasItem("COUNSELLOR")));

        // 4. Filter by FACULTY
        mockMvc.perform(get("/api/v1/users?role=FACULTY").session(adminSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))))
            .andExpect(jsonPath("$.data[*].role", everyItem(is("FACULTY"))));
    }

    @Test
    @Transactional
    @DisplayName("Authorization Boundary: Ordinary faculty cannot assign or remove responsibilities")
    void unauthorizedFaculty_cannotManageResponsibilities() throws Exception {
        MockHttpSession facultySession = loginAs("ramalakshmi", "Faculty@123");

        Section sec = sectionRepository.findAll().get(0);
        Department dept = sec.getSemester().getProgram().getDepartment();
        List<User> users = userRepository.findAll();
        User target = users.get(0);

        ResponsibilityRequest req = new ResponsibilityRequest(
            "CLASS_TEACHER",
            dept.getId(),
            null,
            null,
            sec.getId(),
            "Unauthorized CT Assignment"
        );

        // Attempt to assign responsibility -> 403
        mockMvc.perform(post("/api/v1/users/" + target.getId() + "/responsibilities")
                .session(facultySession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isForbidden());

        // Attempt to remove responsibility -> 403
        List<Responsibility> resps = responsibilityRepository.findAll();
        if (!resps.isEmpty()) {
            mockMvc.perform(delete("/api/v1/responsibilities/" + resps.get(0).getId())
                    .session(facultySession))
                .andExpect(status().isForbidden());
        }
    }
}
