package com.crre.academicdms;

import com.crre.academicdms.features.auth.dto.LoginRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
    @DisplayName("Login with student username returns 200 and valid JWT")
    void loginWithUsername_success() throws Exception {
        LoginRequest request = new LoginRequest("22b91a0501", "Student@123");

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.data.accessToken").isString())
            .andExpect(jsonPath("$.data.user.username").value("22b91a0501"))
            .andExpect(jsonPath("$.data.user.role").value("STUDENT"))
            .andExpect(jsonPath("$.data.user.registrationNumber").value("22B91A0501"))
            .andExpect(jsonPath("$.data.user.uniqueId").value("CRRE20220501"))
            .andExpect(jsonPath("$.data.user.aadharNumber").value(789012345678L))
            .andExpect(jsonPath("$.data.user.apaarNumber").value(987654321012L))
            .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        String token = root.path("data").path("accessToken").asText();
        assertThat(token).isNotBlank();
    }

    @Test
    @DisplayName("Login with student registration number returns 200 and valid JWT")
    void loginWithRegistrationNumber_success() throws Exception {
        LoginRequest request = new LoginRequest("22B91A0501", "Student@123");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.user.username").value("22b91a0501"))
            .andExpect(jsonPath("$.data.user.role").value("STUDENT"));
    }

    @Test
    @DisplayName("Login with student email returns 200 and valid JWT")
    void loginWithEmail_success() throws Exception {
        LoginRequest request = new LoginRequest("student@crre.edu", "Student@123");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.user.username").value("22b91a0501"));
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
    @DisplayName("GET /api/v1/auth/me without token returns 401 Unauthorized")
    void getCurrentUserWithoutToken_unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("GET /api/v1/auth/me with Bearer token returns 200 and user profile")
    void getCurrentUserWithToken_success() throws Exception {
        // Step 1: Login as Admin
        LoginRequest request = new LoginRequest("admin", "Admin@123");
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode root = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String token = root.path("data").path("accessToken").asText();

        // Step 2: Fetch current user profile
        mockMvc.perform(get("/api/v1/auth/me")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.username").value("admin"))
            .andExpect(jsonPath("$.data.email").value("admin@crre.edu"))
            .andExpect(jsonPath("$.data.role").value("ADMIN"))
            .andExpect(jsonPath("$.data.uniqueId").value("ADM20260001"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/logout returns 200 OK")
    void logout_success() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("Logged out successfully"));
    }
}
