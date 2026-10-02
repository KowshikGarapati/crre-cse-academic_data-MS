package com.crre.academicdms.features.academic.repository;

import com.crre.academicdms.features.academic.model.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, UUID> {
    Optional<Subject> findByCode(String code);
    List<Subject> findBySemesterId(UUID semesterId);
    List<Subject> findByProgramId(UUID programId);
}
