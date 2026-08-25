package com.loanpro.modules.audit.dto;

import com.loanpro.modules.application.domain.ApplicationStatus;
import com.loanpro.modules.audit.domain.AuditLog;

import java.time.Instant;
import java.util.UUID;

public record AuditLogResponse(
        UUID id,
        String userEmail,
        String action,
        String entityType,
        UUID entityId,
        UUID applicationId,
        String applicationNumber,
        ApplicationStatus oldStatus,
        ApplicationStatus newStatus,
        String remarks,
        String ipAddress,
        Instant timestamp
) {
    public static AuditLogResponse from(AuditLog log) {
        return new AuditLogResponse(
                log.getId(),
                log.getUserEmail(),
                log.getAction(),
                log.getEntityType(),
                log.getEntityId(),
                log.getApplication() != null ? log.getApplication().getId() : null,
                log.getApplication() != null ? log.getApplication().getApplicationNumber() : null,
                log.getOldStatus(),
                log.getNewStatus(),
                log.getRemarks(),
                log.getIpAddress(),
                log.getCreatedAt()
        );
    }
}
