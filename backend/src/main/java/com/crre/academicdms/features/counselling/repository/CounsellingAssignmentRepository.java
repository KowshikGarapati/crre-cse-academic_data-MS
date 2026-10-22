package com.crre.academicdms.features.counselling.repository;

import com.crre.academicdms.features.counselling.model.CounsellingAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CounsellingAssignmentRepository extends JpaRepository<CounsellingAssignment, UUID> {
    List<CounsellingAssignment> findByCounsellorIdAndStatus(UUID counsellorUserId, String status);
    List<CounsellingAssignment> findByStudentIdAndStatus(UUID studentId, String status);
    List<CounsellingAssignment> findByStudentId(UUID studentId);
    void deleteByStudentId(UUID studentId);
    Optional<CounsellingAssignment> findByCounsellorIdAndStudentId(UUID counsellorUserId, UUID studentId);
    boolean existsByCounsellorIdAndStudentIdAndStatus(UUID counsellorUserId, UUID studentId, String status);
}
