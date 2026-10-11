package com.crre.academicdms.features.student.repository;

import com.crre.academicdms.features.student.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentRepository extends JpaRepository<Student, UUID> {
    Optional<Student> findByRegistrationNumber(String registrationNumber);
    Optional<Student> findByRegistrationNumberIgnoreCase(String registrationNumber);
    Optional<Student> findByUserId(UUID userId);
    List<Student> findAllByOrderByRegistrationNumberAsc();
    List<Student> findByIdInOrderByRegistrationNumberAsc(List<UUID> ids);
}
