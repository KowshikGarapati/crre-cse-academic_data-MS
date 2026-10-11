package com.crre.academicdms.features.curriculum.repository;

import com.crre.academicdms.features.curriculum.model.Resource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ResourceRepository extends JpaRepository<Resource, UUID> {
    List<Resource> findByConceptIdOrderByCreatedAtAsc(UUID conceptId);
    List<Resource> findByConceptIdInOrderByCreatedAtAsc(List<UUID> conceptIds);
}
