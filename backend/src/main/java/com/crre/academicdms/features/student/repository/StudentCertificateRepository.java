package com.crre.academicdms.features.student.repository;

import com.crre.academicdms.features.student.model.StudentCertificate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StudentCertificateRepository extends JpaRepository<StudentCertificate, UUID> {
    List<StudentCertificate> findByStudentIdOrderByCreatedAtAsc(UUID studentId);
    boolean existsByStudentIdAndCertificateType(UUID studentId, String certificateType);
    void deleteByStudentId(UUID studentId);
}
