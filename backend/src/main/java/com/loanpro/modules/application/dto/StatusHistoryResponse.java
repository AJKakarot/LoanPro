package com.loanpro.modules.application.dto;

import com.loanpro.modules.application.domain.ApplicationStatus;
import com.loanpro.modules.application.domain.ApplicationStatusHistory;

import java.time.Instant;
import java.util.UUID;

public record StatusHistoryResponse(
        UUID id,
        ApplicationStatus fromStatus,
        ApplicationStatus toStatus,
        String changedBy,
        String remarks,
        Instant timestamp
) {
    public static StatusHistoryResponse from(ApplicationStatusHistory history) {
        return new StatusHistoryResponse(
                history.getId(),
                history.getFromStatus(),
                history.getToStatus(),
                history.getChangedBy() == null ? null : history.getChangedBy().getFullName(),
                history.getRemarks(),
                history.getCreatedAt()
        );
    }
}
