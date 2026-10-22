package com.crre.academicdms.features.audit.service;

import com.crre.academicdms.features.audit.model.SystemAuditLog;
import com.crre.academicdms.features.audit.repository.SystemAuditLogRepository;
import com.crre.academicdms.security.UserPrincipal;
import com.crre.academicdms.features.user.model.User;
import com.crre.academicdms.features.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuditLogService {

    private final SystemAuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public AuditLogService(SystemAuditLogRepository auditLogRepository, UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public SystemAuditLog logAction(
        UserPrincipal actor,
        String action,
        String entityType,
        String entityId,
        String oldValue,
        String newValue,
        String ipAddress,
        String userAgent
    ) {
        User user = null;
        String username = "SYSTEM";
        String role = "SYSTEM";

        if (actor != null) {
            username = actor.getUsername();
            role = actor.getRole() != null ? actor.getRole().name() : "UNKNOWN";
            user = userRepository.findById(actor.getId()).orElse(null);
        }

        SystemAuditLog log = new SystemAuditLog(
            user,
            username,
            role,
            action,
            entityType,
            entityId,
            oldValue,
            newValue,
            ipAddress,
            userAgent
        );

        return auditLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public List<SystemAuditLog> getAuditHistory(String entityType, String entityId) {
        return auditLogRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(entityType, entityId);
    }
}
