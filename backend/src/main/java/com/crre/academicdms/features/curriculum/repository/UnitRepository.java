package com.crre.academicdms.features.curriculum.repository;

import com.crre.academicdms.features.curriculum.model.Unit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UnitRepository extends JpaRepository<Unit, UUID> {
    List<Unit> findBySubjectIdOrderByUnitNumberAsc(UUID subjectId);
    Optional<Unit> findBySubjectIdAndUnitNumber(UUID subjectId, int unitNumber);
}
