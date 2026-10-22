package com.crre.academicdms.features.student.service;

import com.crre.academicdms.features.counselling.repository.CounsellingAssignmentRepository;
import com.crre.academicdms.features.counselling.repository.CounsellingRecordRepository;
import com.crre.academicdms.features.student.model.Student;
import com.crre.academicdms.features.student.model.StudentEnrollment;
import com.crre.academicdms.features.student.repository.StudentEducationalHistoryRepository;
import com.crre.academicdms.features.student.repository.StudentEnrollmentRepository;
import com.crre.academicdms.features.student.repository.StudentRepository;
import com.crre.academicdms.features.user.model.User;
import com.crre.academicdms.features.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class SyntheticDataManagementService {

    private static final Logger log = LoggerFactory.getLogger(SyntheticDataManagementService.class);

    private final StudentRepository studentRepository;
    private final StudentEnrollmentRepository enrollmentRepository;
    private final StudentEducationalHistoryRepository educationalHistoryRepository;
    private final CounsellingAssignmentRepository counsellingAssignmentRepository;
    private final CounsellingRecordRepository counsellingRecordRepository;
    private final UserRepository userRepository;

    public SyntheticDataManagementService(
        StudentRepository studentRepository,
        StudentEnrollmentRepository enrollmentRepository,
        StudentEducationalHistoryRepository educationalHistoryRepository,
        CounsellingAssignmentRepository counsellingAssignmentRepository,
        CounsellingRecordRepository counsellingRecordRepository,
        UserRepository userRepository
    ) {
        this.studentRepository = studentRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.educationalHistoryRepository = educationalHistoryRepository;
        this.counsellingAssignmentRepository = counsellingAssignmentRepository;
        this.counsellingRecordRepository = counsellingRecordRepository;
        this.userRepository = userRepository;
    }

    public record SyntheticIsolationSummary(
        int studentsMarkedSynthetic,
        int enrollmentsDeactivated
    ) {}

    public record SyntheticPurgeSummary(
        int studentsDeleted,
        int enrollmentsDeleted,
        int educationalHistoryDeleted,
        int counsellingAssignmentsDeleted,
        int counsellingRecordsDeleted,
        int usersDeleted
    ) {}

    /**
     * Soft-deactivates all synthetic prototype students and their section enrollments.
     * Identifies synthetic records by comparing against the known official registration numbers.
     */
    @Transactional
    public SyntheticIsolationSummary isolateSyntheticStudents(Set<String> officialRegistrationNumbers) {
        List<Student> allStudents = studentRepository.findAll();
        int markedCount = 0;
        int deactivatedEnrollments = 0;

        for (Student s : allStudents) {
            String reg = s.getRegistrationNumber().trim().toUpperCase();
            if (!officialRegistrationNumbers.contains(reg)) {
                s.setSynthetic(true);
                s.setStatus("INACTIVE");
                studentRepository.save(s);
                markedCount++;

                List<StudentEnrollment> enrollments = enrollmentRepository.findByStudentId(s.getId());
                for (StudentEnrollment se : enrollments) {
                    if (se.isCurrent()) {
                        se.setCurrent(false);
                        enrollmentRepository.save(se);
                        deactivatedEnrollments++;
                    }
                }
            } else {
                s.setSynthetic(false);
                studentRepository.save(s);
            }
        }

        log.info("Isolated synthetic dataset: {} students marked synthetic/INACTIVE, {} enrollments deactivated.",
            markedCount, deactivatedEnrollments);
        return new SyntheticIsolationSummary(markedCount, deactivatedEnrollments);
    }

    /**
     * Completely purges all synthetic students and their associated test records.
     * Official student records are strictly untouched.
     */
    @Transactional
    public SyntheticPurgeSummary purgeSyntheticData() {
        List<Student> syntheticStudents = studentRepository.findByIsSyntheticTrue();
        int stuCount = syntheticStudents.size();
        int enrCount = 0;
        int eduCount = 0;
        int cAssCount = 0;
        int cRecCount = 0;
        int uCount = 0;

        for (Student s : syntheticStudents) {
            UUID sId = s.getId();
            User user = s.getUser();

            cRecCount += counsellingRecordRepository.findByStudentIdOrderByMeetingDateDesc(sId).size();
            counsellingRecordRepository.deleteByStudentId(sId);

            cAssCount += counsellingAssignmentRepository.findByStudentId(sId).size();
            counsellingAssignmentRepository.deleteByStudentId(sId);

            eduCount += educationalHistoryRepository.findByStudentId(sId).size();
            educationalHistoryRepository.deleteByStudentId(sId);

            List<StudentEnrollment> enrollments = enrollmentRepository.findByStudentId(sId);
            enrCount += enrollments.size();
            enrollmentRepository.deleteAll(enrollments);

            s.setUser(null);
            studentRepository.delete(s);

            if (user != null) {
                userRepository.delete(user);
                uCount++;
            }
        }

        log.info("Purged synthetic dataset: {} students, {} enrollments, {} educational history rows, {} counselling records, {} users.",
            stuCount, enrCount, eduCount, cRecCount, uCount);

        return new SyntheticPurgeSummary(stuCount, enrCount, eduCount, cAssCount, cRecCount, uCount);
    }
}
