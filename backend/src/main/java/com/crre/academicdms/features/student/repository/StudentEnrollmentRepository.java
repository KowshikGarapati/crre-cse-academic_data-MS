package com.crre.academicdms.features.student.repository;

import com.crre.academicdms.features.student.model.StudentEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentEnrollmentRepository extends JpaRepository<StudentEnrollment, UUID> {
    List<StudentEnrollment> findBySectionIdAndCurrentTrueOrderByStudentRegistrationNumberAsc(UUID sectionId);
    Optional<StudentEnrollment> findByStudentIdAndCurrentTrue(UUID studentId);
}
