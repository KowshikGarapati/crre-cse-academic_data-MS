package com.crre.academicdms.features.academic.repository;

import com.crre.academicdms.features.academic.model.Program;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProgramRepository extends JpaRepository<Program, UUID> {
    List<Program> findByDepartmentId(UUID departmentId);
    Optional<Program> findByCode(String code);
}
