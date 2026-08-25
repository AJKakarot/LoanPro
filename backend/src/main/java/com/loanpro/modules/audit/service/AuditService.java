package com.loanpro.modules.audit.service;

import com.loanpro.modules.application.domain.ApplicationStatus;
import com.loanpro.modules.application.domain.LoanApplication;
import com.loanpro.modules.audit.domain.AuditLog;
import com.loanpro.modules.audit.dto.AuditLogResponse;
import com.loanpro.modules.audit.repository.AuditLogRepository;
import com.loanpro.modules.identity.domain.User;
import com.loanpro.common.api.PageResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public void record(
            User user,
            String action,
            String entityType,
            UUID entityId,
            LoanApplication application,
            ApplicationStatus oldStatus,
            ApplicationStatus newStatus,
            String remarks
    ) {
        AuditLog log = new AuditLog();
        log.setUser(user);
        log.setUserEmail(user != null ? user.getEmail() : null);
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setApplication(application);
        log.setOldStatus(oldStatus);
        log.setNewStatus(newStatus);
        log.setRemarks(remarks);
        log.setIpAddress(currentIp());
        auditLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> search(String search, UUID applicationId, Pageable pageable) {
        String q = search == null || search.isBlank() ? "" : search.trim();
        return PageResponse.from(auditLogRepository.search(q, applicationId, pageable).map(AuditLogResponse::from));
    }

    private String currentIp() {
        var attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes servletAttrs) {
            HttpServletRequest request = servletAttrs.getRequest();
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        }
        return null;
    }
}
