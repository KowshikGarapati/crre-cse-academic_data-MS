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
            .andExpect(jsonPath("$.data.length()").value(66))
            .andExpect(jsonPath("$.data[0].registrationNumber").value("25B81A4201"))
            .andExpect(jsonPath("$.data[65].registrationNumber").value("25B81A4266"));
    }
}
