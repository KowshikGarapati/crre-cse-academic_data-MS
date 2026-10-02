package com.crre.academicdms.features.academic.repository;

import com.crre.academicdms.features.academic.model.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, UUID> {
    Optional<Department> findByCode(String code);
    List<Department> findByInstitutionId(UUID institutionId);
}
