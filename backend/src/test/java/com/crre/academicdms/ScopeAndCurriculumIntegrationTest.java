package com.crre.academicdms;

import com.crre.academicdms.features.academic.model.Subject;
import com.crre.academicdms.features.academic.repository.SubjectRepository;
import com.crre.academicdms.features.auth.dto.LoginRequest;
import com.crre.academicdms.features.curriculum.dto.CurriculumDtos.CreateResourceRequest;
import com.crre.academicdms.features.curriculum.model.Concept;
import com.crre.academicdms.features.curriculum.model.Unit;
import com.crre.academicdms.features.curriculum.repository.ConceptRepository;
import com.crre.academicdms.features.curriculum.repository.UnitRepository;
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

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ScopeAndCurriculumIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private UnitRepository unitRepository;

    @Autowired
    private ConceptRepository conceptRepository;

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
    @DisplayName("Institutional Identity Login: Student can log in with Registration Number as both username and password")
    void studentLoginWithInstitutionalId() throws Exception {
        LoginRequest req = new LoginRequest("25B81A4202", "25B81A4202");
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.user.registrationNumber").value("25B81A4202"))
            .andExpect(jsonPath("$.data.user.mustChangePassword").value(true));
    }

    @Test
    @DisplayName("Institutional Identity Login: Faculty can log in with Employee Number as initial password")
    void facultyLoginWithInstitutionalId() throws Exception {
        LoginRequest req = new LoginRequest("FAC-CSE-005", "FAC-CSE-005");
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.user.username").value("FAC-CSE-005"))
            .andExpect(jsonPath("$.data.user.role").value("FACULTY"))
            .andExpect(jsonPath("$.data.user.mustChangePassword").value(true));
    }

    @Test
    @DisplayName("Unified Student Scope: GET /api/v1/students returns authoritatively scoped students for Class Teacher")
    void classTeacher_scopedStudents() throws Exception {
        MockHttpSession session = loginAs("ramalakshmi", "Faculty@123");

        mockMvc.perform(get("/api/v1/students").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.length()", greaterThanOrEqualTo(66)))
            .andExpect(jsonPath("$.data[*].registrationNumber", hasItem("25B81A4201")));
    }

    @Test
    @DisplayName("Unified Student Scope: GET /api/v1/students returns assigned mentees for Counsellor")
    void counsellor_scopedStudents() throws Exception {
        MockHttpSession session = loginAs("counsellor.cse", "Faculty@123");

        mockMvc.perform(get("/api/v1/students").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.length()", greaterThanOrEqualTo(15)))
            .andExpect(jsonPath("$.data[*].registrationNumber", hasItem("25B81A4201")));
    }

    @Test
    @DisplayName("Unified Student Scope: HOD sees all students in department (720 students across 10 sections)")
    void hod_scopedStudents() throws Exception {
        MockHttpSession session = loginAs("hod.cse", "Admin@123");

        mockMvc.perform(get("/api/v1/students").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.length()", greaterThanOrEqualTo(720)));
    }

    @Test
    @DisplayName("Student Details URL parameter accepts Registration Number string (e.g. 25B81A4201)")
    void studentDetails_acceptsRegistrationNumber() throws Exception {
        MockHttpSession session = loginAs("hod.cse", "Admin@123");

        mockMvc.perform(get("/api/v1/students/25B81A4201").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.registrationNumber").value("25B81A4201"))
            .andExpect(jsonPath("$.data.fullName").value("AKHIL SHARIFF"))
            .andExpect(jsonPath("$.data.placement.sectionName").value("Section 1"))
            .andExpect(jsonPath("$.data.currentCourses").isArray())
            .andExpect(jsonPath("$.data.timetable").isArray())
            .andExpect(jsonPath("$.data.canViewCounselling").value(true));
    }

    @Test
    @DisplayName("Curriculum: Subject contains 5 Units, Concepts, and Learning Resources")
    void curriculum_subjectUnitsAndResources() throws Exception {
        MockHttpSession session = loginAs("25B81A4202", "25B81A4202");

        List<Subject> subjects = subjectRepository.findByCode("CS301");
        assertThat(subjects).isNotEmpty();
        UUID subjectId = subjects.get(0).getId();

        mockMvc.perform(get("/api/v1/curriculum/subjects/" + subjectId + "/units").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.subjectCode").value("CS301"))
            .andExpect(jsonPath("$.data.units.length()").value(5))
            .andExpect(jsonPath("$.data.units[0].concepts.length()").value(3))
            .andExpect(jsonPath("$.data.units[0].concepts[0].resources").isArray())
            .andExpect(jsonPath("$.data.units[0].concepts[0].resources[0].resourceType").value("YOUTUBE_VIDEO"));
    }

    @Test
    @DisplayName("Student Resources: GET /api/v1/student/resources returns current semester registered subjects and units")
    void student_resourcesFlow() throws Exception {
        MockHttpSession session = loginAs("25B81A4202", "25B81A4202");

        mockMvc.perform(get("/api/v1/student/resources").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data.length()", greaterThanOrEqualTo(5)))
            .andExpect(jsonPath("$.data[0].units.length()").value(5));
    }

    @Test
    @DisplayName("Resource Upload Authorization: Faculty cannot upload resource to subject they do not teach")
    void faculty_unauthorizedResourceUpload_forbidden() throws Exception {
        // Faculty Ramalakshmi teaches CS301 (Discrete Math).
        // Let's find a Concept for a subject she does NOT teach, e.g. CS305 (Universal Human Values)
        List<Subject> uhvSubjects = subjectRepository.findByCode("CS305");
        assertThat(uhvSubjects).isNotEmpty();
        Subject uhvSubject = uhvSubjects.get(0);

        List<Unit> uhvUnits = unitRepository.findBySubjectIdOrderByUnitNumberAsc(uhvSubject.getId());
        assertThat(uhvUnits).isNotEmpty();
        List<Concept> uhvConcepts = conceptRepository.findByUnitIdOrderByConceptOrderAsc(uhvUnits.get(0).getId());
        assertThat(uhvConcepts).isNotEmpty();
        UUID foreignConceptId = uhvConcepts.get(0).getId();

        MockHttpSession session = loginAs("ramalakshmi", "Faculty@123");

        CreateResourceRequest req = new CreateResourceRequest(
            "Unauthorized Lecture Video",
            "This should fail authorization",
            "YOUTUBE_VIDEO",
            "https://www.youtube.com/watch?v=sample",
            null
        );

        mockMvc.perform(post("/api/v1/curriculum/concepts/" + foreignConceptId + "/resources")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("HOD Manage Users: Does NOT return stray test users or students, only departmental personnel")
    void hod_manageUsersExcludesStudentsAndStrayRecords() throws Exception {
        MockHttpSession session = loginAs("hod.cse", "Admin@123");

        mockMvc.perform(get("/api/v1/users").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[*].role", not(hasItem("STUDENT"))))
            .andExpect(jsonPath("$.data[*].role", not(hasItem("PRINCIPAL"))))
            .andExpect(jsonPath("$.data[*].role", not(hasItem("ADMIN"))));
    }
}
