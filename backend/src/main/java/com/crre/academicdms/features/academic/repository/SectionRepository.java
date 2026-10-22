package com.crre.academicdms.features.academic.repository;

import com.crre.academicdms.features.academic.model.Section;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SectionRepository extends JpaRepository<Section, UUID> {
    List<Section> findBySemesterId(UUID semesterId);
    java.util.Optional<Section> findBySemesterIdAndSectionNumber(UUID semesterId, int sectionNumber);

    @org.springframework.data.jpa.repository.Query("""
        SELECT s FROM Section s
        JOIN FETCH s.semester sem
        JOIN FETCH sem.program p
    """)
    List<Section> findAllWithProgram();

    @org.springframework.data.jpa.repository.Query("""
        SELECT s FROM Section s
        WHERE s.semester.program.department.id = :deptId
    """)
    List<Section> findByDepartmentId(@org.springframework.data.repository.query.Param("deptId") UUID deptId);
}
