package com.crre.academicdms.features.faculty.service;

import com.crre.academicdms.features.academic.model.Department;
import com.crre.academicdms.features.academic.repository.DepartmentRepository;
import com.crre.academicdms.features.faculty.model.FacultyProfile;
import com.crre.academicdms.features.faculty.repository.FacultyProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Seeds published official faculty profiles from Sir C. R. Reddy College of Engineering (https://sircrrengg.ac.in/).
 * Strictly preserves official designations, qualifications, and source URLs without auto-generating user accounts.
 */
@Component
@Order(50)
public class OfficialFacultySeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(OfficialFacultySeeder.class);
    private static final String CSE_FACULTY_URL = "https://sircrrengg.ac.in/departments/computer-science-and-engineering/faculty";

    private final FacultyProfileRepository facultyProfileRepository;
    private final DepartmentRepository departmentRepository;

    public OfficialFacultySeeder(
        FacultyProfileRepository facultyProfileRepository,
        DepartmentRepository departmentRepository
    ) {
        this.facultyProfileRepository = facultyProfileRepository;
        this.departmentRepository = departmentRepository;
    }

    private record OfficialFacultyRecord(
        String name,
        String employeeNumber,
        String designation,
        String qualification,
        String specialization,
        String sourceUrl
    ) {}

    private static final List<OfficialFacultyRecord> OFFICIAL_CSE_FACULTY = List.of(
        new OfficialFacultyRecord("Dr. A. Yesubabu", "CRRE-FAC-OFF-001", "Professor & HOD", "M.Tech., Ph.D.", "Computer Science & Engineering", CSE_FACULTY_URL),
        new OfficialFacultyRecord("Dr. M. Krishna", "CRRE-FAC-OFF-002", "Professor", "M.Tech., Ph.D.", "Computer Science & Engineering", CSE_FACULTY_URL),
        new OfficialFacultyRecord("Dr. B. Madhava Rao", "CRRE-FAC-OFF-003", "Professor", "Ph.D., M.Tech (CST), M.Sc., B.Sc.", "Bioinformatics and Deep Learning", CSE_FACULTY_URL),
        new OfficialFacultyRecord("K. Anil Chowdary", "CRRE-FAC-OFF-004", "Associate Professor", "M.Tech.", "Computer Science & Engineering", CSE_FACULTY_URL),
        new OfficialFacultyRecord("J. S. V. Gopala Krishna", "CRRE-FAC-OFF-005", "Associate Professor", "M.Tech.", "Computer Networks & Security", CSE_FACULTY_URL),
        new OfficialFacultyRecord("Dr. G. Nirmala", "CRRE-FAC-OFF-006", "Associate Professor", "M.Tech., Ph.D.", "Data Mining & Machine Learning", CSE_FACULTY_URL),
        new OfficialFacultyRecord("Dr. N. Deepak", "CRRE-FAC-OFF-007", "Associate Professor", "M.Tech., Ph.D.", "Cloud Computing & Distributed Systems", CSE_FACULTY_URL),
        new OfficialFacultyRecord("Dr. Rama Devi Chalasani", "CRRE-FAC-OFF-008", "Associate Professor", "Ph.D. (CSE), PG (CST), UG (IT)", "Machine Learning & Big Data Analytics", CSE_FACULTY_URL),
        new OfficialFacultyRecord("S. M. B. Chowdary", "CRRE-FAC-OFF-009", "Sr. Assistant Professor", "M.Tech.", "Computer Science & Engineering", CSE_FACULTY_URL),
        new OfficialFacultyRecord("K. Srinivas", "CRRE-FAC-OFF-010", "Assistant Professor", "M.Tech.", "Software Engineering & Algorithms", CSE_FACULTY_URL),
        new OfficialFacultyRecord("Dr. K. N. Madhavi Latha", "CRRE-FAC-OFF-011", "Assistant Professor", "M.Tech., Ph.D.", "Image Processing & Computer Vision", CSE_FACULTY_URL),
        new OfficialFacultyRecord("K. Chaitanya Deepthi", "CRRE-FAC-OFF-012", "Assistant Professor", "M.Tech.", "Information Security", CSE_FACULTY_URL),
        new OfficialFacultyRecord("Y. Leela Sandhya Rani", "CRRE-FAC-OFF-013", "Assistant Professor", "M.Tech.", "Data Science", CSE_FACULTY_URL),
        new OfficialFacultyRecord("K. Varada Rajkumar", "CRRE-FAC-OFF-014", "Assistant Professor", "M.Tech.", "Database Management Systems", CSE_FACULTY_URL),
        new OfficialFacultyRecord("M. Ganesh Babu", "CRRE-FAC-OFF-015", "Assistant Professor", "M.Tech.", "Operating Systems & System Software", CSE_FACULTY_URL),
        new OfficialFacultyRecord("G. Satyanarayana", "CRRE-FAC-OFF-016", "Assistant Professor", "M.Tech.", "Computer Networks", CSE_FACULTY_URL),
        new OfficialFacultyRecord("B. H. Benny", "CRRE-FAC-OFF-017", "Assistant Professor", "M.Tech.", "Web Technologies & Full Stack Development", CSE_FACULTY_URL),
        new OfficialFacultyRecord("S. K. Chaitanya R.", "CRRE-FAC-OFF-018", "Assistant Professor", "M.Tech.", "Computer Graphics", CSE_FACULTY_URL),
        new OfficialFacultyRecord("Samparthi V. S. Kumar", "CRRE-FAC-OFF-019", "Assistant Professor", "M.Tech.", "Artificial Intelligence", CSE_FACULTY_URL),
        new OfficialFacultyRecord("P. Naga Deepthi", "CRRE-FAC-OFF-020", "Assistant Professor", "M.Tech.", "Machine Learning", CSE_FACULTY_URL),
        new OfficialFacultyRecord("K. Swathi", "CRRE-FAC-OFF-021", "Assistant Professor", "Ph.D. (CSE), PG (CNIS), UG (CSE)", "Artificial Intelligence & Data Structures", CSE_FACULTY_URL),
        new OfficialFacultyRecord("P. Pavani", "CRRE-FAC-OFF-022", "Assistant Professor", "M.Tech (CSE), B.Tech (CSE)", "Artificial Intelligence & Foundations of Data Science", CSE_FACULTY_URL)
    );

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Department cseDept = departmentRepository.findByCode("CSE").orElse(null);
        if (cseDept == null) {
            log.warn("CSE Department not found, skipping official faculty profile seeding.");
            return;
        }

        int created = 0;
        for (OfficialFacultyRecord rec : OFFICIAL_CSE_FACULTY) {
            if (facultyProfileRepository.findByEmployeeNumber(rec.employeeNumber()).isEmpty()) {
                FacultyProfile profile = new FacultyProfile(
                    rec.name(),
                    rec.employeeNumber(),
                    cseDept,
                    rec.designation(),
                    rec.qualification(),
                    rec.specialization(),
                    rec.sourceUrl()
                );
                profile.setSynthetic(false);
                facultyProfileRepository.save(profile);
                created++;
            }
        }

        if (created > 0) {
            log.info("Successfully seeded {} official faculty profiles from Sir C. R. Reddy College of Engineering website.", created);
        }
    }
}
