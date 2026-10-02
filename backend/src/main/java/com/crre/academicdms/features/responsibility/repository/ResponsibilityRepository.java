package com.crre.academicdms.features.responsibility.repository;

import com.crre.academicdms.features.responsibility.model.Responsibility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ResponsibilityRepository extends JpaRepository<Responsibility, UUID> {

    List<Responsibility> findByUserId(UUID userId);

    List<Responsibility> findByUserIdAndStatus(UUID userId, String status);

    List<Responsibility> findByDepartmentIdAndStatus(UUID departmentId, String status);

    List<Responsibility> findBySectionIdAndStatus(UUID sectionId, String status);

    @Query("""
        SELECT COUNT(r) > 0 FROM Responsibility r
        WHERE r.user.id = :userId
          AND r.type = 'TEACHING'
          AND r.subject.id = :subjectId
          AND r.section.id = :sectionId
          AND r.status = 'ACTIVE'
    """)
    boolean existsActiveTeaching(@Param("userId") UUID userId, @Param("subjectId") UUID subjectId, @Param("sectionId") UUID sectionId);

    @Query("""
        SELECT COUNT(r) > 0 FROM Responsibility r
        WHERE r.user.id = :userId
          AND r.type = :type
          AND r.section.id = :sectionId
          AND r.status = 'ACTIVE'
    """)
    boolean existsActiveSectionRole(@Param("userId") UUID userId, @Param("type") String type, @Param("sectionId") UUID sectionId);

    @Query("""
        SELECT COUNT(r) > 0 FROM Responsibility r
        WHERE r.user.id = :userId
          AND r.type = :type
          AND r.department.id = :deptId
          AND r.status = 'ACTIVE'
    """)
    boolean existsActiveDeptRole(@Param("userId") UUID userId, @Param("type") String type, @Param("deptId") UUID deptId);
}
