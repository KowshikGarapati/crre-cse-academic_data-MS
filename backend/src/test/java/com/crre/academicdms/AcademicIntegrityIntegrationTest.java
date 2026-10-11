package com.crre.academicdms;

import com.crre.academicdms.features.academic.model.Section;
import com.crre.academicdms.features.academic.model.Subject;
import com.crre.academicdms.features.academic.model.TimetableEntry;
import com.crre.academicdms.features.academic.repository.DepartmentRepository;
import com.crre.academicdms.features.academic.repository.SectionRepository;
import com.crre.academicdms.features.academic.repository.SubjectRepository;
import com.crre.academicdms.features.academic.repository.TimetableRepository;
import com.crre.academicdms.features.academic.service.SyntheticDatasetSeeder;
import com.crre.academicdms.features.auth.dto.LoginRequest;
import com.crre.academicdms.features.responsibility.repository.ResponsibilityRepository;
import com.crre.academicdms.features.user.model.User;
import com.crre.academicdms.features.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AcademicIntegrityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SyntheticDatasetSeeder seeder;

    @Autowired
    private TimetableRepository timetableRepository;

    @Autowired
    private ResponsibilityRepository responsibilityRepository;

    @Autowired
    private SectionRepository sectionRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

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
    @DisplayName("DB Constraint uk_timetable_faculty_day_period: Rejects faculty collision at same day and period")
    void facultyTimetableCollision_RejectedByDatabaseConstraint() {
        List<TimetableEntry> existing = timetableRepository.findAll();
        assertThat(existing).isNotEmpty();
        TimetableEntry sample = existing.get(0);

        List<Section> otherSections = sectionRepository.findAll().stream()
            .filter(s -> !s.getId().equals(sample.getSection().getId()))
            .toList();
        assertThat(otherSections).isNotEmpty();
        Section diffSection = otherSections.get(0);

        // Attempt to book the same faculty in a different section at the exact same day and period
        TimetableEntry colliding = new TimetableEntry(
            diffSection,
            sample.getSubject(),
            sample.getFacultyUser(),
            sample.getTeachingAssignment(),
            sample.getDayOfWeek(),
            sample.getPeriodNumber(),
            sample.getStartTime(),
            sample.getEndTime(),
            "LH-999",
            "THEORY"
        );

        assertThatThrownBy(() -> {
            timetableRepository.saveAndFlush(colliding);
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("DB Constraint uk_timetable_section_day_period: Rejects section collision at same day and period")
    void sectionTimetableCollision_RejectedByDatabaseConstraint() {
        List<TimetableEntry> existing = timetableRepository.findAll();
        assertThat(existing).isNotEmpty();
        TimetableEntry sample = existing.get(0);

        List<User> otherFaculty = userRepository.findAll().stream()
            .filter(u -> !u.getId().equals(sample.getFacultyUser().getId()))
            .toList();
        assertThat(otherFaculty).isNotEmpty();
        User diffFaculty = otherFaculty.get(0);

        // Attempt to book a second subject/faculty for the same section at the same slot
        TimetableEntry colliding = new TimetableEntry(
            sample.getSection(),
            sample.getSubject(),
            diffFaculty,
            sample.getTeachingAssignment(),
            sample.getDayOfWeek(),
            sample.getPeriodNumber(),
            sample.getStartTime(),
            sample.getEndTime(),
            "LH-999",
            "THEORY"
        );

        assertThatThrownBy(() -> {
            timetableRepository.saveAndFlush(colliding);
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Seeder Idempotency: Running seeder again produces 0 new records and exactly 420 timetable entries")
    void seederIdempotency_ZeroDuplicates() throws Exception {
        long timetableBefore = timetableRepository.count();
        long teachingBefore = responsibilityRepository.findByTypeAndStatus("TEACHING", "ACTIVE").size();
        long ctBefore = responsibilityRepository.findByTypeAndStatus("CLASS_TEACHER", "ACTIVE").size();

        // Run seeder again
        seeder.run(null);

        long timetableAfter = timetableRepository.count();
        long teachingAfter = responsibilityRepository.findByTypeAndStatus("TEACHING", "ACTIVE").size();
        long ctAfter = responsibilityRepository.findByTypeAndStatus("CLASS_TEACHER", "ACTIVE").size();

        // Exactly 420 timetable slots (10 sections * 6 days * 7 periods)
        assertThat(timetableAfter).isEqualTo(420);
        assertThat(timetableAfter).isEqualTo(timetableBefore);
        assertThat(teachingAfter).isEqualTo(teachingBefore);
        assertThat(ctAfter).isEqualTo(ctBefore);
    }

    @Test
    @DisplayName("Academic Periods API: Returns canonical period definitions (P1..P7)")
    void getAcademicPeriods_ReturnsCanonicalTimes() throws Exception {
        MockHttpSession session = loginAs("admin", "Admin@123");

        mockMvc.perform(get("/api/v1/academic/periods")
                .session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data", hasSize(7)))
            .andExpect(jsonPath("$.data[0].periodNumber").value(1))
            .andExpect(jsonPath("$.data[0].startTime").value("09:30"))
            .andExpect(jsonPath("$.data[0].endTime").value("10:20"))
            .andExpect(jsonPath("$.data[6].periodNumber").value(7))
            .andExpect(jsonPath("$.data[6].startTime").value("15:50"))
            .andExpect(jsonPath("$.data[6].endTime").value("16:30"));
    }

    @Test
    @DisplayName("Section Detail Overview: Returns Class Teacher, Roster, Timetable, and Subjects")
    void getSectionOverview_ReturnsCompleteInformation() throws Exception {
        MockHttpSession session = loginAs("hod.cse", "Admin@123");
        Section sec = sectionRepository.findAll().get(0);

        mockMvc.perform(get("/api/v1/sections/" + sec.getId() + "/overview")
                .session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.sectionId").value(sec.getId().toString()))
            .andExpect(jsonPath("$.data.sectionName").value(sec.getSectionName()))
            .andExpect(jsonPath("$.data.classTeacherName").isNotEmpty())
            .andExpect(jsonPath("$.data.subjects", hasSize(greaterThanOrEqualTo(7))))
            .andExpect(jsonPath("$.data.timetable", hasSize(42)))
            .andExpect(jsonPath("$.data.studentRoster", hasSize(greaterThanOrEqualTo(60))))
            .andExpect(jsonPath("$.data.students", hasSize(greaterThanOrEqualTo(60))));
    }

    @Test
    @DisplayName("Faculty Grouped Students: Returns students grouped by section")
    void getFacultyGroupedStudents_ReturnsGroupedList() throws Exception {
        MockHttpSession session = loginAs("faculty.cse", "Faculty@123");

        mockMvc.perform(get("/api/v1/faculty/my-students/grouped")
                .session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("Counselling Mentees & Records API: Dedicated /api/v1/counselling endpoints work correctly")
    void counsellingEndpoints_WorkSeparatelyFromClassStudents() throws Exception {
        MockHttpSession session = loginAs("counsellor.cse", "Faculty@123");

        mockMvc.perform(get("/api/v1/counselling/mentees")
                .session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))));

        mockMvc.perform(get("/api/v1/counselling/records")
                .session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("Principal College Overview & Department Drill-Down: Returns programs and hierarchy")
    void principalHierarchyDrillDown_WorksCorrectly() throws Exception {
        MockHttpSession session = loginAs("principal", "Admin@123");

        MvcResult res = mockMvc.perform(get("/api/v1/principal/college-overview")
                .session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.departments", hasSize(greaterThanOrEqualTo(1))))
            .andExpect(jsonPath("$.data.departments[0].programs", hasSize(greaterThanOrEqualTo(1))))
            .andReturn();

        String deptId = departmentRepository.findAll().get(0).getId().toString();

        mockMvc.perform(get("/api/v1/principal/departments/" + deptId)
                .session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.academicYears", hasSize(greaterThanOrEqualTo(1))));
    }
}
