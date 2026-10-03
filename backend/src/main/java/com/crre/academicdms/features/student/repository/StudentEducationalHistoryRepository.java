package com.crre.academicdms.features.student.repository;

import com.crre.academicdms.features.student.model.StudentEducationalHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StudentEducationalHistoryRepository extends JpaRepository<StudentEducationalHistory, UUID> {
    List<StudentEducationalHistory> findByStudentIdOrderByCompletionYearAsc(UUID studentId);
}
