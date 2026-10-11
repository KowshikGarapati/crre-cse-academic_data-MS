package com.crre.academicdms.features.academic.repository;

import com.crre.academicdms.features.academic.model.Semester;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SemesterRepository extends JpaRepository<Semester, UUID> {
    List<Semester> findByProgramId(UUID programId);
    Optional<Semester> findByProgramIdAndSemesterNumber(UUID programId, int semesterNumber);
}
