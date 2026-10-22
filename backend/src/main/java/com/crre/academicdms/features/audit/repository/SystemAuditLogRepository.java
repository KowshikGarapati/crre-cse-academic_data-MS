package com.crre.academicdms.features.audit.repository;

import com.crre.academicdms.features.audit.model.SystemAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SystemAuditLogRepository extends JpaRepository<SystemAuditLog, UUID> {
    List<SystemAuditLog> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(String entityType, String entityId);
    List<SystemAuditLog> findByActionOrderByCreatedAtDesc(String action);
}
