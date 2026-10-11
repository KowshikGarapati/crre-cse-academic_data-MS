package com.crre.academicdms.features.curriculum.repository;

import com.crre.academicdms.features.curriculum.model.Concept;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConceptRepository extends JpaRepository<Concept, UUID> {
    List<Concept> findByUnitIdOrderByConceptOrderAsc(UUID unitId);
    Optional<Concept> findByUnitIdAndConceptOrder(UUID unitId, int conceptOrder);
    List<Concept> findByUnitIdInOrderByConceptOrderAsc(List<UUID> unitIds);
}
