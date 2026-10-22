package com.crre.academicdms.features.faculty.repository;

import com.crre.academicdms.features.faculty.model.FacultyProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FacultyProfileRepository extends JpaRepository<FacultyProfile, UUID> {
    Optional<FacultyProfile> findByUserId(UUID userId);
    Optional<FacultyProfile> findByEmployeeNumber(String employeeNumber);
    List<FacultyProfile> findByDepartmentId(UUID departmentId);
    List<FacultyProfile> findByDepartmentIdAndActiveStatusTrue(UUID departmentId);
    List<FacultyProfile> findByIsSyntheticFalse();
    List<FacultyProfile> findByDepartmentIdAndIsSyntheticFalse(UUID departmentId);
}
